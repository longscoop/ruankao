#!/usr/bin/env python3
"""Stage or publish PDF source pages through the admin import workflow.

Parsed question and knowledge candidates stay unpublished. Every PDF page is
preserved as source text plus its rendered image in small reader lessons.
"""

import argparse
import hashlib
import json
import os
import sys
import uuid
from pathlib import Path
from urllib import error, request


def sha256_file(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


class AdminApi:
    def __init__(self, base_url, username, password):
        self.base_url = base_url.rstrip("/")
        login = self._request("POST", "/api/v1/auth/admin/login", {
            "username": username, "password": password,
        })
        self.token = login["token"]

    def _request(self, method, path, payload=None, multipart=None):
        headers = {"Accept": "application/json"}
        token = getattr(self, "token", None)
        if token:
            headers["Authorization"] = "Bearer " + token
        body = None
        if multipart is not None:
            body, boundary = multipart
            headers["Content-Type"] = "multipart/form-data; boundary=" + boundary
        elif payload is not None:
            body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            headers["Content-Type"] = "application/json"
        operation = request.Request(self.base_url + path, data=body,
                                    headers=headers, method=method)
        try:
            with request.urlopen(operation, timeout=1800) as response:
                content = response.read()
                return json.loads(content) if content else None
        except error.HTTPError as exc:
            message = exc.read(500).decode("utf-8", errors="replace")
            raise RuntimeError(f"{method} {path}: HTTP {exc.code}: {message}") from exc

    def list_batches(self, exam_id):
        return self._request("GET", f"/api/v1/admin/imports?examId={exam_id}")

    def upload_pdf(self, exam_id, path):
        boundary = "ruankao-" + uuid.uuid4().hex
        filename = path.name.replace('"', "_")
        body = (
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"examId\"\r\n\r\n{exam_id}\r\n"
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; "
            f"filename=\"{filename}\"\r\nContent-Type: application/pdf\r\n\r\n"
        ).encode("utf-8") + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode("ascii")
        return self._request("POST", "/api/v1/admin/imports/pdf", multipart=(body, boundary))

    def update_title(self, batch_id, title):
        return self._request("PUT", f"/api/v1/admin/imports/{batch_id}", {"title": title})

    def create_source_lessons(self, batch_id):
        return self._request("POST", f"/api/v1/admin/imports/{batch_id}/source-lessons")

    def detail(self, batch_id):
        return self._request("GET", f"/api/v1/admin/imports/{batch_id}")

    def approve_item(self, batch_id, item_id):
        return self._request("POST", f"/api/v1/admin/imports/{batch_id}/items/{item_id}/approve")

    def reject_item(self, batch_id, item_id):
        return self._request("POST", f"/api/v1/admin/imports/{batch_id}/items/{item_id}/reject")

    def resolve_issue(self, batch_id, issue_id):
        return self._request("POST", f"/api/v1/admin/imports/{batch_id}/issues/{issue_id}/resolve")

    def confirm(self, batch_id, key):
        return self._request("POST", f"/api/v1/admin/imports/{batch_id}/confirm", {"confirmKey": key})

    def publish_lesson(self, batch_id, item_id, knowledge_ids):
        return self._request("POST", f"/api/v1/admin/imports/{batch_id}/items/{item_id}/publish-lesson",
                             {"knowledgeIds": knowledge_ids})


def validate_source_pages(detail, source_item_ids):
    pages = detail["pages"]
    expected_count = detail["pageCount"]
    if expected_count < 1 or len(pages) != expected_count:
        raise ValueError(f"PDF page count {expected_count} does not match stored pages {len(pages)}")
    page_by_number = {}
    for number, page in enumerate(pages, 1):
        if page["pageNumber"] != number or not page.get("imageObjectKey"):
            raise ValueError(f"page {number} is missing its ordered source image")
        page_by_number[number] = page

    items = {item["id"]: item for item in detail["items"]}
    covered = []
    for item_id in source_item_ids:
        item = items.get(item_id)
        if not item or item["itemType"] != "LESSON" or not item["itemKey"].startswith("source-pages-"):
            raise ValueError(f"source lesson {item_id} is missing")
        lesson = json.loads(item["contentJson"])
        first, last = lesson["sourcePageStart"], lesson["sourcePageEnd"]
        if last - first + 1 > 12:
            raise ValueError(f"source lesson {item_id} exceeds 12 pages")
        images = {}
        texts = {}
        for block in lesson["blocks"]:
            number = block["sourcePage"]
            if block["blockType"] == "IMAGE":
                if number in images:
                    raise ValueError(f"page {number} has duplicate images")
                images[number] = block.get("imageObjectKey")
            elif block["blockType"] == "TEXT":
                if number in texts:
                    raise ValueError(f"page {number} has duplicate text")
                texts[number] = block.get("textContent")
            else:
                raise ValueError(f"source lesson {item_id} has an unexpected block type")
        for number in range(first, last + 1):
            if number not in page_by_number or images.get(number) != page_by_number[number]["imageObjectKey"]:
                raise ValueError(f"page {number} image does not match source evidence")
            text = page_by_number[number].get("textContent") or ""
            if text.strip() and texts.get(number) != text:
                raise ValueError(f"page {number} text does not match source evidence")
            covered.append(number)
    if covered != list(range(1, expected_count + 1)):
        raise ValueError("source lessons do not cover every PDF page once in order")


def publish_source_lessons(api, detail, source_item_ids, sha256):
    batch_id = detail["batchId"]
    source_ids = set(source_item_ids)
    for issue in detail["issues"]:
        if issue["status"] != "OPEN" or issue["severity"] != "ERROR":
            continue
        if issue["itemId"] in source_ids or (
                issue["itemId"] is None and issue["code"] != "UNRECOGNIZED_DOCUMENT"):
            raise ValueError(f"open source error {issue['code']} blocks publication")
    for item in detail["items"]:
        if item["id"] not in source_ids and item["status"] in {"PENDING", "APPROVED"}:
            api.reject_item(batch_id, item["id"])
    for issue in detail["issues"]:
        if issue["status"] == "OPEN" and issue["code"] == "UNRECOGNIZED_DOCUMENT":
            api.resolve_issue(batch_id, issue["id"])
    for item in detail["items"]:
        if item["id"] in source_ids and item["status"] == "PENDING":
            api.approve_item(batch_id, item["id"])

    api.confirm(batch_id, "source-" + sha256)
    confirmed = api.detail(batch_id)
    materialized = {item["id"]: item for item in confirmed["items"]}
    for item_id in source_item_ids:
        item = materialized[item_id]
        if item["status"] == "PUBLISHED":
            continue
        if item["status"] != "MATERIALIZED" or not item.get("targetId"):
            raise ValueError(f"source lesson {item_id} was not materialized")
        api.publish_lesson(batch_id, item_id, [])
    final = api.detail(batch_id)
    final_items = {item["id"]: item for item in final["items"]}
    if not final.get("courseId") or any(final_items[item_id]["status"] != "PUBLISHED"
                                         for item_id in source_item_ids):
        raise ValueError("published source lessons were not persisted")
    return final


def import_directory(api, root, exam_id, publish=False):
    root = Path(root)
    if not root.is_dir():
        raise ValueError(f"source directory does not exist: {root}")
    files = sorted((path for path in root.rglob("*") if path.is_file()
                    and path.suffix.lower() == ".pdf"), key=lambda path: str(path))
    batches = api.list_batches(exam_id)
    by_sha = {}
    for batch in batches:
        if batch.get("sha256") and batch["status"] != "FAILED":
            current = by_sha.get(batch["sha256"])
            if current is None or batch["status"] == "PUBLISHED":
                by_sha[batch["sha256"]] = batch
    processed = set()
    results = []
    for path in files:
        digest = sha256_file(path)
        result = {"file": str(path.relative_to(root)), "sha256": digest}
        try:
            if digest in processed:
                result["status"] = "duplicate"
                result["batchId"] = by_sha[digest]["batchId"]
                result["courseId"] = by_sha[digest].get("courseId")
                results.append(result)
                continue
            batch = by_sha.get(digest)
            if batch and batch["status"] == "PUBLISHED":
                result.update(status="duplicate", batchId=batch["batchId"],
                              courseId=batch.get("courseId"), pageCount=batch["pageCount"])
                processed.add(digest)
                results.append(result)
                continue
            if batch is None:
                uploaded = api.upload_pdf(exam_id, path)
                batch_id = uploaded["batchId"]
                title = "／".join(path.relative_to(root).with_suffix("").parts)
                api.update_title(batch_id, title[:500])
            else:
                batch_id = batch["batchId"]
            result["batchId"] = batch_id
            source_item_ids = api.create_source_lessons(batch_id)
            detail = api.detail(batch_id)
            validate_source_pages(detail, source_item_ids)
            result.update(pageCount=detail["pageCount"], sourceLessonCount=len(source_item_ids),
                          detectedType=detail["detectedType"])
            if publish:
                detail = publish_source_lessons(api, detail, source_item_ids, digest)
                result.update(status="published", courseId=detail["courseId"])
            else:
                result["status"] = "staged"
            by_sha[digest] = {"batchId": batch_id, "courseId": result.get("courseId"),
                              "status": detail["status"], "pageCount": detail["pageCount"]}
            processed.add(digest)
        except (RuntimeError, ValueError, KeyError, OSError) as exc:
            result.update(status="error", error=str(exc))
        results.append(result)
    return results


def credentials(env_file):
    values = {}
    if env_file:
        for line in Path(env_file).read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                values[key] = value.strip().strip('"').strip("'")
    username = os.environ.get("RUANKAO_ADMIN_USERNAME") or values.get("ADMIN_USERNAME")
    password = os.environ.get("RUANKAO_ADMIN_PASSWORD") or values.get("ADMIN_PASSWORD")
    if not username or not password:
        raise ValueError("admin credentials require RUANKAO_ADMIN_USERNAME/PASSWORD or --env-file")
    return username, password


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, type=Path, help="directory containing PDF files")
    parser.add_argument("--exam-id", required=True, type=int)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--env-file", type=Path, help="server-only deployment .env with admin credentials")
    parser.add_argument("--publish", action="store_true", help="approve and publish source-page lessons after page audit")
    parser.add_argument("--report", required=True, type=Path, help="JSON result file, outside the repository")
    args = parser.parse_args(argv)
    username, password = credentials(args.env_file)
    api = AdminApi(args.base_url, username, password)
    results = import_directory(api, args.source, args.exam_id, publish=args.publish)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(results, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    counts = {status: sum(item["status"] == status for item in results)
              for status in {item["status"] for item in results}}
    print(json.dumps({"files": len(results), "statusCounts": counts, "report": str(args.report)},
                     ensure_ascii=False))
    return 1 if counts.get("error") else 0


if __name__ == "__main__":
    sys.exit(main())
