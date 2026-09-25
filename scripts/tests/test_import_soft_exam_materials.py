import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "import_soft_exam_materials.py"


class FakeApi:
    def __init__(self, missing_image=False):
        self.missing_image = missing_image
        self.uploads = 0
        self.actions = []
        self.batch = None
        self.items = []

    def list_batches(self, exam_id):
        return [] if self.batch is None else [self.batch]

    def upload_pdf(self, exam_id, path):
        self.uploads += 1
        self.batch = {
            "batchId": "batch-1", "examId": exam_id, "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "status": "REVIEWING", "pageCount": 2, "courseId": None,
            "detectedType": "UNKNOWN",
        }
        return {"batchId": "batch-1"}

    def update_title(self, batch_id, title):
        self.actions.append(("title", title))

    def create_source_lessons(self, batch_id):
        self.items = [
            {"id": 10, "itemType": "LESSON", "itemKey": "source-pages-1-2", "status": "PENDING",
             "targetId": None, "contentJson": json.dumps({
                 "sourcePageStart": 1, "sourcePageEnd": 2,
                 "blocks": [
                     {"blockType": "TEXT", "sourcePage": 1, "textContent": "Page one"},
                     {"blockType": "IMAGE", "sourcePage": 1, "imageObjectKey": "pages/1.png"},
                     {"blockType": "IMAGE", "sourcePage": 2, "imageObjectKey": "pages/2.png"},
                 ],
             })},
            {"id": 11, "itemType": "QUESTION", "itemKey": "question-1", "status": "PENDING",
             "targetId": None, "contentJson": "{}"},
        ]
        return [10]

    def detail(self, batch_id):
        return {
            **self.batch,
            "pages": [
                {"pageNumber": 1, "textContent": "Page one", "imageObjectKey": "pages/1.png"},
                {"pageNumber": 2, "textContent": "", "imageObjectKey": "" if self.missing_image else "pages/2.png"},
            ],
            "items": self.items,
            "issues": [{"id": 3, "code": "UNRECOGNIZED_DOCUMENT", "severity": "ERROR",
                        "itemId": None, "status": "OPEN"}],
        }

    def reject_item(self, batch_id, item_id):
        self.actions.append(("reject", item_id))
        self.items[1]["status"] = "REJECTED"

    def approve_item(self, batch_id, item_id):
        self.actions.append(("approve", item_id))
        self.items[0]["status"] = "APPROVED"

    def resolve_issue(self, batch_id, issue_id):
        self.actions.append(("resolve", issue_id))

    def confirm(self, batch_id, key):
        self.actions.append(("confirm", key))
        self.batch["courseId"] = 50
        self.batch["status"] = "CONFIRMED"
        self.items[0]["status"] = "MATERIALIZED"
        self.items[0]["targetId"] = 70
        return {"courseId": 50}

    def publish_lesson(self, batch_id, item_id, knowledge_ids):
        self.actions.append(("publish", item_id, knowledge_ids))
        self.items[0]["status"] = "PUBLISHED"
        self.batch["status"] = "PUBLISHED"


class ImportScriptTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        spec = importlib.util.spec_from_file_location("import_soft_exam_materials", SCRIPT)
        cls.module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.module)

    def test_publishes_source_pages_and_skips_duplicate_bytes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "one.pdf").write_bytes(b"same PDF")
            (root / "copy.pdf").write_bytes(b"same PDF")
            api = FakeApi()

            results = self.module.import_directory(api, root, exam_id=1, publish=True)

            self.assertEqual(1, api.uploads)
            self.assertEqual(["duplicate", "published"], sorted(x["status"] for x in results))
            self.assertIn(("reject", 11), api.actions)
            self.assertIn(("approve", 10), api.actions)
            self.assertIn(("publish", 10, []), api.actions)
            self.assertEqual(50, next(x["courseId"] for x in results if x["status"] == "published"))

    def test_missing_page_image_stops_before_any_publication(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "broken.pdf").write_bytes(b"PDF bytes")
            api = FakeApi(missing_image=True)

            results = self.module.import_directory(api, root, exam_id=1, publish=True)

            self.assertEqual("error", results[0]["status"])
            self.assertIn("page 2", results[0]["error"])
            self.assertFalse(any(action[0] in {"approve", "confirm", "publish"} for action in api.actions))


if __name__ == "__main__":
    unittest.main()
