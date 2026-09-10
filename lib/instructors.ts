export type Instructor = { id: number; tenantId: string; branchId: number; firstName: string; lastName: string; email: string; phone?: string; specialization?: string; active: boolean; createdAt?: string; updatedAt?: string };
export type InstructorPayload = Omit<Instructor, "id" | "createdAt" | "updatedAt">;
export type InstructorPage = { content: Instructor[]; number: number; totalElements: number; totalPages: number; first: boolean; last: boolean };
const API = process.env.NEXT_PUBLIC_API_URL ?? "/api";
async function request<T>(path: string, options?: RequestInit): Promise<T> { const response = await fetch(`${API}${path}`, { ...options, credentials: "include", headers: { "Content-Type": "application/json", ...(options?.headers ?? {}) }, cache: "no-store" }); if (!response.ok) throw new Error((await response.text()) || `Request failed with status ${response.status}.`); return response.status === 204 ? (undefined as T) : response.json(); }
export function getInstructors(search = "", page = 0, size = 100) { const params = new URLSearchParams({ tenantId: "default", page: String(page), size: String(size) }); if (search.trim()) params.set("search", search.trim()); return request<InstructorPage>(`/instructors?${params}`); }
export function getInstructor(id: string) { return request<Instructor>(`/instructors/${id}?tenantId=default`); }
export function createInstructor(payload: InstructorPayload) { return request<Instructor>("/instructors", { method: "POST", body: JSON.stringify(payload) }); }
export function updateInstructor(id: string, payload: InstructorPayload) { return request<Instructor>(`/instructors/${id}`, { method: "PUT", body: JSON.stringify(payload) }); }
export function deleteInstructor(id: number) { return request<void>(`/instructors/${id}?tenantId=default`, { method: "DELETE" }); }
export function getInstructorCount() { return request<number>("/instructors/count?tenantId=default"); }