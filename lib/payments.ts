export type PaymentStatus = "PAID" | "PENDING";
export type PaymentMethod = "CASH" | "UPI" | "CARD" | "BANK_TRANSFER";
export type Payment = { id: number; tenantId: string; branchId: number; membershipId: number; studentId: number; amount: number; paymentDate: string; paymentMethod: PaymentMethod; paymentStatus: PaymentStatus; remarks?: string; active: boolean; createdAt?: string; updatedAt?: string };
export type PaymentPayload = Omit<Payment, "id" | "createdAt" | "updatedAt">;
export type PaymentPage = { content: Payment[]; number: number; totalElements: number; totalPages: number; first: boolean; last: boolean };
const API = process.env.NEXT_PUBLIC_API_URL ?? "/api";
async function request<T>(path: string, options?: RequestInit): Promise<T> { const response = await fetch(`${API}${path}`, { ...options, credentials: "include", headers: { "Content-Type": "application/json", ...(options?.headers ?? {}) }, cache: "no-store" }); if (!response.ok) throw new Error((await response.text()) || `Request failed with status ${response.status}.`); return response.status === 204 ? (undefined as T) : response.json(); }
export function getPayments(search = "", page = 0, size = 100) { const params = new URLSearchParams({ tenantId: "default", page: String(page), size: String(size) }); if (search.trim()) params.set("search", search.trim()); return request<PaymentPage>(`/payments?${params}`); }
export function getPayment(id: string) { return request<Payment>(`/payments/${id}?tenantId=default`); }
export function createPayment(payload: PaymentPayload) { return request<Payment>("/payments", { method: "POST", body: JSON.stringify(payload) }); }
export function updatePayment(id: string, payload: PaymentPayload) { return request<Payment>(`/payments/${id}`, { method: "PUT", body: JSON.stringify(payload) }); }
export function deletePayment(id: number) { return request<void>(`/payments/${id}?tenantId=default`, { method: "DELETE" }); }
export function getTotalRevenue() { return request<number>("/payments/summary/total-revenue?tenantId=default"); }
export function getPendingPayments() { return request<number>("/payments/summary/pending?tenantId=default"); }
export function getMonthlyCollections() { return request<number>("/payments/summary/monthly-collections?tenantId=default"); }
