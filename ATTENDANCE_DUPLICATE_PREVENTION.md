# Attendance Duplicate Prevention

## Root Cause

The batch attendance flow always called `POST /api/attendance/batch` with new entries. It did not query existing records for the selected batch and date, so the UI looked like a new entry form and repeated submissions could insert duplicates.

## Files Changed

- `app/attendance/batch/page.tsx`
- `components/batch-attendance-form.tsx`
- `lib/attendance.ts`
- `backend/src/main/java/com/studioos/repository/AttendanceRepository.java`
- `backend/src/main/java/com/studioos/dto/BatchAttendanceEntryDTO.java`
- `backend/src/main/java/com/studioos/service/AttendanceServiceImpl.java`
- `ATTENDANCE_DUPLICATE_PREVENTION.md`

## Duplicate Prevention Strategy

- The Batch Attendance screen queries attendance by selected date and filters records by selected `batchId`.
- Existing records hydrate status, performance score, remarks, and record ID into the form.
- The screen displays `Attendance already recorded for this date` when matching records exist.
- Existing record IDs are sent with batch entries.
- The backend also looks up `tenant + student + batch + attendanceDate` before inserting, and updates the matching record instead.
- This prevents duplicate records for Student + Batch + Date while reusing the existing batch attendance API.
