export type ManagedUser = { id: number; tenantId: string; fullName: string; email: string; password?: string | null; role: string; branchId?: number; instructorId?: number; enabled: boolean };
const API = process.env.NEXT_PUBLIC_API_URL ?? "/api";
function parseErrorMessage(body: string, fallback: string): string { if (!body) return fallback; try { const parsed = JSON.parse(body) as { error?: string; message?: string }; return parsed.error || parsed.message || body || fallback; } catch { return body || fallback; } }
async function request<T>(path: string, options?: RequestInit): Promise<T> { const response = await fetch(`${API}${path}`, { ...options, credentials: "include", headers: { "Content-Type": "application/json", ...(options?.headers ?? {}) }, cache: "no-store" }); if (!response.ok) throw new Error(parseErrorMessage(await response.text(), "The request could not be completed.")); if (response.status === 204) return undefined as T; const text = await response.text(); return (text ? JSON.parse(text) : undefined) as T; }
export function getManagedUsers() { return request<ManagedUser[]>("/admin/users"); }
export function createManagedUser(payload: ManagedUser) { return request<ManagedUser>("/admin/users", { method: "POST", body: JSON.stringify(payload) }); }
export function updateManagedUser(id: number, payload: ManagedUser) { return request<ManagedUser>(`/admin/users/${id}`, { method: "PUT", body: JSON.stringify(payload) }); }
export function disableManagedUser(id: number) { return request<void>(`/admin/users/${id}`, { method: "DELETE" }); }
export function removeManagedUser(id: number) { return request<void>(`/admin/users/${id}?permanent=true`, { method: "DELETE" }); }
