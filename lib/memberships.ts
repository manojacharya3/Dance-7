export type MembershipStatus = "ACTIVE" | "EXPIRED" | "CANCELLED";
export type Membership = { id: number; tenantId: string; branchId: number; studentId: number; planName: string; durationMonths: number; startDate: string; endDate: string; feeAmount: number; status: MembershipStatus; active: boolean; createdAt?: string; updatedAt?: string };
export type MembershipPayload = Omit<Membership, "id" | "createdAt" | "updatedAt">;
export type MembershipPage = { content: Membership[]; number: number; totalElements: number; totalPages: number; first: boolean; last: boolean };
const API = process.env.NEXT_PUBLIC_API_URL ?? "/api";
async function request<T>(path: string, options?: RequestInit): Promise<T> { const response = await fetch(`${API}${path}`, { ...options, credentials: "include", headers: { "Content-Type": "application/json", ...(options?.headers ?? {}) }, cache: "no-store" }); if (!response.ok) { const body = await response.text(); let message = body || `Request failed with status ${response.status}.`; try { const parsed = JSON.parse(body) as { error?: string; message?: string }; message = parsed.error || parsed.message || message; } catch { /* keep body/status message */ } throw new Error(message); } return response.status === 204 ? (undefined as T) : response.json(); }
export function getMemberships(search = "", page = 0, size = 100) { const params = new URLSearchParams({ tenantId: "default", page: String(page), size: String(size) }); if (search.trim()) params.set("search", search.trim()); return request<MembershipPage>(`/memberships?${params}`); }
export function getMembership(id: string) { return request<Membership>(`/memberships/${id}?tenantId=default`); }
export function createMembership(payload: MembershipPayload) { return request<Membership>("/memberships", { method: "POST", body: JSON.stringify(payload) }); }
export function updateMembership(id: string, payload: MembershipPayload) { return request<Membership>(`/memberships/${id}`, { method: "PUT", body: JSON.stringify(payload) }); }
export function deleteMembership(id: number) { return request<void>(`/memberships/${id}?tenantId=default`, { method: "DELETE" }); }
export function getActiveMembershipCount() { return request<number>("/memberships/count/active?tenantId=default"); }
export function getExpiringMembershipCount() { return request<number>("/memberships/count/expiring?tenantId=default"); }
export function getExpiredMembershipCount() { return request<number>("/memberships/count/expired?tenantId=default"); }
