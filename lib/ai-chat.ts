"use client";

const BASE = "/api/chat/public";
const ADMIN = "/api/ai/admin";

async function request<T>(url: string, options?: RequestInit, auth = false): Promise<T> {
  const response = await fetch(url, {
    ...options,
    ...(auth ? { credentials: "include" as const } : {}),
    headers: { "Content-Type": "application/json", ...(options?.headers ?? {}) },
  });
  if (!response.ok) {
    const body = await response.text();
    let message = body || "Request failed.";
    try {
      message = JSON.parse(body).message ?? JSON.parse(body).error ?? message;
    } catch {
      /* plain response */
    }
    throw new Error(message);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export type BranchOption = { id: number; name: string; slug: string };
export type ChatRecommendation = { classId: number; name: string; category: string; reason: string; ageRange: string; schedule: string; fee: string };
export type ChatReply = {
  conversationId: number;
  visitorId: string;
  reply: string;
  intent: string;
  leadPrompt: boolean;
  quickActions: string[];
  recommendations: ChatRecommendation[];
};

export function getBranches() {
  return request<BranchOption[]>(`${BASE}/branches`);
}

export function sendMessage(input: { branch: string; conversationId?: number; visitorId?: string; message: string }) {
  return request<ChatReply>(`${BASE}/message`, { method: "POST", body: JSON.stringify(input) });
}

export function submitLead(input: {
  branch: string;
  conversationId?: number;
  visitorId?: string;
  studentName?: string;
  parentName?: string;
  age?: number;
  phone: string;
  email?: string;
  interestedClass?: string;
  preferredBatch?: string;
  preferredStartDate?: string;
  message?: string;
}) {
  return request<{ leadId: number; status: string; confirmation: string }>(`${BASE}/leads`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export type Analytics = {
  conversations: number;
  leads: number;
  leadsNew: number;
  messages: number;
  topIntents: { intent: string; count: number }[];
  leadsByStatus: { status: string; count: number }[];
};

export type Diagnostics = {
  tenant: string;
  branchId: number;
  branchName: string;
  branchActive: boolean;
  datasetVersion: string | null;
  counts: Record<string, number>;
};

export type ChatLead = {
  id: number;
  branchId: number;
  conversationId: number;
  studentName: string;
  parentName: string;
  age: number;
  phone: string;
  email: string;
  interestedClass: string;
  preferredBatch: string;
  preferredStartDate: string;
  message: string;
  status: string;
  createdAt: string;
};

function admin<T>(url: string, options?: RequestInit) {
  return request<T>(url, options, true);
}

export const aiAdmin = {
  analytics: (branch: string) => admin<Analytics>(`${ADMIN}/analytics?branch=${encodeURIComponent(branch)}`),
  diagnostics: (branch: string) => admin<Diagnostics>(`${ADMIN}/diagnostics?branch=${encodeURIComponent(branch)}`),
  list: <T>(resource: string, branch: string) =>
    admin<T[]>(`${ADMIN}/${resource}?branch=${encodeURIComponent(branch)}`),
  save: <T>(resource: string, payload: unknown) =>
    admin<T>(`${ADMIN}/${resource}`, { method: "POST", body: JSON.stringify(payload) }),
  remove: (resource: string, branch: string, id: number) =>
    admin<void>(`${ADMIN}/${resource}/${id}?branch=${encodeURIComponent(branch)}`, { method: "DELETE" }),
  leads: (branch: string, status = "ALL") =>
    admin<ChatLead[]>(`${ADMIN}/leads?branch=${encodeURIComponent(branch)}&status=${status}`),
  leadStatus: (branch: string, id: number, status: string) =>
    admin<ChatLead>(`${ADMIN}/leads/${id}?branch=${encodeURIComponent(branch)}`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }),
};
