export type FeedbackCategory =
  | "BUG"
  | "UI_ISSUE"
  | "MOBILE_ISSUE"
  | "PERFORMANCE"
  | "CHATBOT"
  | "PAYMENT"
  | "IMPROVEMENT"
  | "FEATURE_REQUEST"
  | "OTHER";
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

export const MAX_SCREENSHOTS = 5;
export const MAX_SCREENSHOT_BYTES = 10 * 1024 * 1024;
const ACCEPTED_TYPES = ["image/png", "image/jpeg", "image/webp"];

export type FeedbackAttachment = {
  id: number;
  fileName: string;
  fileUrl: string;
  contentType: string;
  fileSize: number;
  uploadedAt?: string | null;
};

export function attachmentUrl(attachment: Pick<FeedbackAttachment, "fileUrl">) {
  const base = process.env.NEXT_PUBLIC_API_URL ?? "/api";
  return `${base}${attachment.fileUrl}`;
}

export function listAttachments(feedbackId: number) {
  return request<FeedbackAttachment[]>(`/feedback/${feedbackId}/attachments?tenantId=default`);
}

export function deleteAttachment(attachmentId: number) {
  return request<void>(`/feedback/attachments/${attachmentId}?tenantId=default`, { method: "DELETE" });
}

export function validateScreenshot(file: File): string | null {
  if (!ACCEPTED_TYPES.includes(file.type)) return `${file.name}: only PNG, JPG and WEBP images are accepted.`;
  if (file.size > MAX_SCREENSHOT_BYTES) return `${file.name}: exceeds the 10 MB limit.`;
  return null;
}

/** Multipart upload with progress (fetch has no upload progress events). */
export function uploadScreenshot(feedbackId: number, file: File, onProgress?: (percent: number) => void) {
  const base = process.env.NEXT_PUBLIC_API_URL ?? "/api";
  return new Promise<FeedbackAttachment>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", `${base}/feedback/${feedbackId}/attachments?tenantId=default`);
    xhr.withCredentials = true;
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) onProgress(Math.round((event.loaded / event.total) * 100));
    };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          resolve(JSON.parse(xhr.responseText));
        } catch {
          reject(new Error("Upload completed but the response was unreadable."));
        }
      } else {
        reject(new Error(xhr.responseText || "Upload failed."));
      }
    };
    xhr.onerror = () => reject(new Error("Dance7 API is unavailable. Upload failed."));
    const form = new FormData();
    form.append("file", file, file.name);
    xhr.send(form);
  });
}
