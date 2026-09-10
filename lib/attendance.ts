export type AttendanceStatus = "PRESENT" | "ABSENT" | "LATE";

export type Attendance = {
  id: number;
  tenantId: string;
  studentId: number;
  batchId?: number;
  attendanceDate: string;
  status: AttendanceStatus;
  performanceScore?: number;
  remarks?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type AttendancePayload = Omit<Attendance, "id" | "createdAt" | "updatedAt">;
export type AttendancePage = { content: Attendance[]; number: number; totalElements: number; totalPages: number };

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "/api";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...options, credentials: "include", headers: { "Content-Type": "application/json", ...(options?.headers ?? {}) }, cache: "no-store" });
  } catch {
    throw new Error("Dance7 API is unavailable. Start the backend on port 8080 and make sure PostgreSQL is running on port 5432.");
  }
  if (!response.ok) { const message = await response.text(); throw new Error(message || `Request failed with status ${response.status}.`); }
  return response.status === 204 ? (undefined as T) : response.json();
}

export function getAttendance(date?: string, studentId?: number) {
  const params = new URLSearchParams({ tenantId: "default", page: "0", size: "100" });
  if (date) params.set("date", date);
  if (studentId) params.set("studentId", String(studentId));
  return request<AttendancePage>(`/attendance?${params.toString()}`);
}

export function getAttendanceByStudent(studentId: number) { return request<AttendancePage>(`/attendance/student/${studentId}?tenantId=default&size=100`); }
export function getAttendanceById(id: string) { return request<Attendance>(`/attendance/${id}?tenantId=default`); }
export function createAttendance(attendance: AttendancePayload) { return request<Attendance>("/attendance", { method: "POST", body: JSON.stringify(attendance) }); }
export function updateAttendance(id: number, attendance: AttendancePayload) { return request<Attendance>(`/attendance/${id}`, { method: "PUT", body: JSON.stringify(attendance) }); }
export type BatchAttendanceEntry = { id?: number; studentId: number; status: AttendanceStatus; performanceScore?: number; remarks?: string };
export function saveBatchAttendance(batchId: number, attendanceDate: string, entries: BatchAttendanceEntry[]) { return request<Attendance[]>("/attendance/batch", { method: "POST", body: JSON.stringify({ tenantId: "default", batchId, attendanceDate, entries }) }); }

export function today() { return new Date().toISOString().slice(0, 10); }