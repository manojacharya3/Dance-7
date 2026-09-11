# Feedback Screenshot Support — Dance7

Date: 2026-09-11. Enhancement only: existing feedback CRUD, statuses, notes, and
records untouched (new `Category` values are additive; old rows unaffected).

## 1. Database changes

- New `feedback_attachments` table via `FeedbackAttachmentMigration` (@Order(4),
  `IF NOT EXISTS`, boot-safe): `id, tenant_id, feedback_id, file_name,
  content_type, file_size, data BYTEA, created_at` + index on `feedback_id`.
  Entity `FeedbackAttachment` mirrors it.
- Categories extended additively: `UI_ISSUE, MOBILE_ISSUE, PERFORMANCE, CHATBOT,
  PAYMENT, OTHER` (existing `BUG/IMPROVEMENT/FEATURE_REQUEST` preserved; no DB
  check constraint existed).

## 2. API changes

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/feedback/{id}/attachments` | POST (multipart `file`) | Upload (10 MB / 5-per-feedback enforced) |
| `/api/feedback/{id}/attachments` | GET | Metadata list (`file_url` per item, no bytes) |
| `/api/feedback/attachments?feedbackId=` | GET | Alias |
| `/api/feedback/attachments/{aid}/content` | GET | Image bytes (`inline`, correct content-type) |
| `/api/feedback/attachments/{aid}` | DELETE | Remove |
| Multipart limits: 10 MB/file, 55 MB/request (application.yml). All routes use existing staff auth. |

## 3. Storage approach

No file service exists in the project and Railway has no persistent volume, so
bytes live in the `data` column with `file_url/file_path` semantics preserved
via the content endpoint (`file_url` = `/api/feedback/attachments/{id}/content`).
Bounded blast radius (≤50 MB per feedback max). Migration path: swap
`FeedbackAttachmentService` persistence for S3-compatible storage + backfill —
shape unchanged. AI analysis later joins on `feedback_id` (noted, not built).

## 4. UI changes

- `feedback/new`: 9-category select; screenshot picker (PNG/JPG/WEBP, ≤5, ≤10 MB
  client-validated) with thumbnail grid, file names, remove buttons, and per-file
  XHR progress bars; create-then-upload flow with partial-failure messaging.
- `feedback` list: per-item "View screenshots" toggle (lazy load), thumbnail
  gallery with hover delete, lightbox with prev/next, fullscreen, and download.
- Existing filters extended with new categories.

## 5. Mobile support

Native `<input type="file" accept="image/…" multiple>` — iPhone Safari, Android
Chrome, and iPad Safari all offer camera + gallery automatically. Touch-sized
controls, responsive thumbnail grid, fullscreen lightbox.

## 6. Security controls

Extension + content-type allowlists, magic-byte sniffing (PNG/JPEG/WEBP headers —
renamed executables/scripts rejected), filename sanitization (no path traversal),
tenant-scoped reads/writes/deletes, existing auth chain, no URL guessing (IDs
are non-sequential-guessable enough for staff-only use; no public routes added).

## 7. Validation results

- `mvn test` ✅ — 18 tests, 0 failures (incl. 5 new: magic bytes, executable/
  script rejection, filename sanitization, extensions, limits).
- `mvn compile` ✅ (via test run).
- `npm run build` ✅ — 29/29 pages (`/feedback`, `/feedback/new` rebuilt).
- Manual pilot checklist: upload 1, upload 5, 6th rejected, oversize rejected,
  remove pre-submit, save, gallery expand, lightbox prev/next/fullscreen/download,
  delete from gallery, mobile camera upload.

## 8. Files modified / commit / push

Backend: `model/FeedbackAttachment`, `model/Feedback` (categories),
`repository/FeedbackAttachmentRepository`, `config/FeedbackAttachmentMigration`,
`service/FeedbackAttachmentService`, `controller/FeedbackAttachmentController`,
`resources/application.yml`, `src/test/.../FeedbackAttachmentTest`.
Frontend: `lib/feedback.ts`, `app/feedback/new/page.tsx`, `app/feedback/page.tsx`.

- Commit: "Add screenshot attachments to feedback system" (`0862a7d`).
- Push: `origin/main` (manojacharya3/Dance-7) — ✅ synced.
