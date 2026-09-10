export type Batch = { id: number; tenantId: string; branchId: number; batchName: string; instructorId: number; startTime: string; endTime: string; capacity: number; active: boolean; createdAt?: string; updatedAt?: string };
export type BatchPayload = Omit<Batch, "id" | "createdAt" | "updatedAt">;
export type BatchPage = { content: Batch[]; number: number; totalElements: number; totalPages: number; first: boolean; last: boolean };
export type StudentBatch = { id: number; tenantId: string; studentId: number; batchId: number; assignedAt?: string };
const API = process.env.NEXT_PUBLIC_API_URL ?? "/api";
async function request<T>(path: string, options?: RequestInit): Promise<T> { const response = await fetch(`${API}${path}`, { ...options, credentials: "include", headers: { "Content-Type": "application/json", ...(options?.headers ?? {}) }, cache: "no-store" }); if (!response.ok) throw new Error((await response.text()) || `Request failed with status ${response.status}.`); return response.status === 204 ? (undefined as T) : response.json(); }
export function getBatches(search = "", page = 0, size = 100) { const params = new URLSearchParams({ tenantId: "default", page: String(page), size: String(size) }); if (search.trim()) params.set("search", search.trim()); return request<BatchPage>(`/batches?${params}`); }
export function getBatch(id: string) { return request<Batch>(`/batches/${id}?tenantId=default`); }
export function createBatch(payload: BatchPayload) { return request<Batch>("/batches", { method: "POST", body: JSON.stringify(payload) }); }
export function updateBatch(id: string, payload: BatchPayload) { return request<Batch>(`/batches/${id}`, { method: "PUT", body: JSON.stringify(payload) }); }
export function deleteBatch(id: number) { return request<void>(`/batches/${id}?tenantId=default`, { method: "DELETE" }); }
export function getBatchCount() { return request<number>("/batches/count?tenantId=default"); }
export function getBatchStudents(batchId: number) { return request<StudentBatch[]>(`/student-batches/batch/${batchId}?tenantId=default`); }
export function getStudentBatches(studentId: number) { return request<StudentBatch[]>(`/student-batches/student/${studentId}?tenantId=default`); }
export function assignStudent(studentId: number, batchId: number) { return request<StudentBatch>("/student-batches", { method: "POST", body: JSON.stringify({ tenantId: "default", studentId, batchId }) }); }
export function removeStudent(studentId: number, batchId: number) { return request<void>(`/student-batches/${studentId}/${batchId}?tenantId=default`, { method: "DELETE" }); }