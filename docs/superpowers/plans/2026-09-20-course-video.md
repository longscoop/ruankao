# Course / Video Implementation Plan

**Goal:** Add the V1 course/video learning domain: configurable course hierarchy, knowledge-linked videos, persistent playback progress, subtitle segments, deterministic 85% completion, one-time weak mastery evidence, and storage-provider boundaries.

**Spec:** `docs/specs/soft-exam-v1.md`

**Depends on:** completed Phase 1-3.

**Tech Stack:** Java 17, Spring Boot 3.3.x, MyBatis-Plus, PostgreSQL, Flyway, Testcontainers, JUnit 5.

## Global Constraints

- Video is learning content attached to knowledge points, not an isolated media library.
- Every publishable video must link to at least one knowledge point in the same exam.
- Database stores object keys, never signed/expiring URLs.
- User video progress is persistent and idempotent per `(user_id, video_id)`.
- Completion threshold is exactly `>= 85%`.
- A completed video may add weak mastery evidence at most once per linked knowledge point.
- Video completion adds at most `+3` mastery per linked knowledge point and does not modify answer streaks.
- Repeated progress reports must never apply video mastery twice.
- AI/video summarization is Phase 5; do not introduce LLM dependencies here.
- Admin upload/workflow UI is Phase 6; Phase 4 exposes domain services and provider abstractions only.
- No Redis or payment dependencies.

---

### Task 1: Flyway V4 course/video schema

**Create:**
- `V4__create_course_video_schema.sql`
- test first: `CourseVideoSchemaMigrationTest.java`

Tables:
- `course`
- `course_chapter`
- `video`
- `video_knowledge_relation`
- `user_video_progress`
- `video_transcript_segment`

Critical constraints:
- course belongs to exam.
- chapter belongs to course; unique `(course_id, sort_order)`.
- video belongs to chapter and stores `object_key`, duration, free flag, status.
- relation unique `(video_id, knowledge_id)`.
- progress unique `(user_id, video_id)`; completion rate 0..100; progress >=0; `mastery_applied` defaults false.
- transcript segment has nonnegative start/end and `end_second > start_second`.

RED: migration test sees missing tables / invalid rows accepted.
GREEN: all V4 schema constraints pass against PostgreSQL.

---

### Task 2: Course / chapter persistence

**Create:**
- `course/model/CourseStatus.java`
- entities/mappers for course and chapter.
- `CourseService.java`
- test first: `CourseServiceIntegrationTest.java`

Interface:
- create course for exam.
- create chapter for course.
- list courses for exam.
- list chapters in stable sort order.

Rules:
- names/titles trimmed and nonblank.
- exam/course must exist.
- sort order nonnegative.
- chapter sort order unique within course.

---

### Task 3: Video persistence and knowledge linking

**Create:**
- `course/model/VideoStatus.java`
- video / video-knowledge entities and mappers.
- `VideoService.java`
- test first: `VideoServiceIntegrationTest.java`

Interface:
- create video metadata.
- link video to knowledge points.
- find/list videos for chapter and knowledge point.

Rules:
- object key trimmed, nonblank.
- duration seconds > 0.
- linked knowledge must belong to same exam as the course.
- duplicate knowledge link rejected.
- PUBLISHED video requires at least one knowledge link.
- database persists object key only.

---

### Task 4: Subtitle segments

**Create:**
- transcript entity/mapper.
- `VideoTranscriptService.java`
- test first: `VideoTranscriptServiceIntegrationTest.java`

Rules:
- replace transcript segments transactionally for a video.
- each segment: `start >= 0`, `end > start`, text nonblank.
- returned segments ordered by start time then id.

---

### Task 5: Playback progress and one-time video mastery

**Create:**
- progress entity/mapper.
- extend mastery persistence with non-answer evidence update.
- `VideoProgressService.java`
- test first: `VideoProgressServiceIntegrationTest.java`

Interface:
- `update(userId, videoId, progressSeconds)`
- `find(userId, videoId)`

Rules:
- reported progress cannot be negative.
- stored progress never moves backwards: store max prior/reported progress.
- effective progress is capped at video duration.
- completion rate = progress/duration * 100.
- completed when completion rate >= 85.
- first completion atomically claims `mastery_applied=false -> true`.
- each linked knowledge gets +3 mastery, clamped 0..100.
- increments evidence count once, but does not change correct/wrong streak.
- repeated completion reports do not apply mastery again.

---

### Task 6: StorageProvider boundary

**Create:**
- `storage/StorageProvider.java`
- `StorageUploadRequest.java`
- `StoredObject.java`
- provider contract unit test.

Contract:
- `upload`
- `delete`
- `generateAccessUrl`

Rules:
- business entities persist only `objectKey`.
- provider-specific SDK types do not escape the storage package.
- no production cloud vendor SDK in Phase 4.
- tests use a fake provider.

---

### Task 7: Phase 4 quality gate

Verify:
- `mvn -B verify`.
- Phase 1 coverage gate stays green.
- all PostgreSQL integration tests green.
- video completion threshold covered at 84.99% / 85% boundaries.
- repeated completion cannot duplicate mastery evidence.
- no AI/Redis/payment dependency.
- no signed URL stored in DB.
- mark this plan complete and link it from `docs/implementation-plan.md`.
