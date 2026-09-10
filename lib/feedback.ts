export type FeedbackCategory = "BUG" | "IMPROVEMENT" | "FEATURE_REQUEST";
export type FeedbackPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type FeedbackStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED";

export type Feedback = {
  id: number;
  tenantId: string;
  title: string;
  description: string;
  category: FeedbackCategory;
  priority: FeedbackPriority;
  status: FeedbackStatus;
  createdBy: string;
  branchId: number;
  internalNotes?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type FeedbackPayload = {
  tenantId: string;
  title: string;
  description: string;
  category: FeedbackCategory;
  priority: FeedbackPriority;
  branchId: number;
};

export type FeedbackPage = {
  content: Feedback[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "/api";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      credentials: "include",
      headers: { "Content-Type": "application/json", ...(options?.headers ?? {}) },
      cache: "no-store",
    });
  } catch {
    throw new Error("Dance7 API is unavailable. Start the backend on port 8080 and make sure PostgreSQL is running on port 5432.");
  }
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || "The request could not be completed.");
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export function getFeedbackList(params: { search?: string; status?: string; category?: string; page?: number; size?: number } = {}) {
  const query = new URLSearchParams({ tenantId: "default", page: String(params.page ?? 0), size: String(params.size ?? 20) });
  if (params.search?.trim()) query.set("search", params.search.trim());
  if (params.status) query.set("status", params.status);
  if (params.category) query.set("category", params.category);
  return request<FeedbackPage>(`/feedback?${query.toString()}`);
}

export function createFeedback(payload: FeedbackPayload) {
  return request<Feedback>("/feedback", { method: "POST", body: JSON.stringify(payload) });
}

export function updateFeedbackStatus(id: number, status: FeedbackStatus) {
  return request<Feedback>(`/feedback/${id}/status?tenantId=default`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

export function updateFeedbackNotes(id: number, notes: string) {
  return request<Feedback>(`/feedback/${id}/notes?tenantId=default`, {
    method: "PATCH",
    body: JSON.stringify({ notes }),
  });
}
