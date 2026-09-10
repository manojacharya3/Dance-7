export type Branch = { id: number; tenantId: string; name: string; address?: string; phone?: string; active: boolean; createdAt?: string; updatedAt?: string };
const API = process.env.NEXT_PUBLIC_API_URL ?? "/api";
async function request<T>(path: string): Promise<T> { const response = await fetch(`${API}${path}`, { credentials: "include", headers: { "Content-Type": "application/json" }, cache: "no-store" }); if (!response.ok) throw new Error((await response.text()) || `Request failed with status ${response.status}.`); return response.json(); }
export function getBranches() { return request<Branch[]>("/branches?tenantId=default"); }
