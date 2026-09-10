export type AuthUser = {
  id: number;
  email: string;
  fullName: string;
  tenantId: string;
  roles: string[];
  branchId?: number | null;
  instructorId?: number | null;
};

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "/api";

async function authRequest<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      credentials: "include",
      headers: { "Content-Type": "application/json", ...(options?.headers ?? {}) },
      cache: "no-store",
    });
  } catch {
    throw new Error("Dance7 API is unavailable. Start the backend and PostgreSQL before signing in.");
  }
  if (!response.ok) {
    const body = await response.text();
    let message = body || (response.status === 401 ? "Invalid email or password." : "Authentication request failed.");
    try { message = JSON.parse(body).message ?? JSON.parse(body).error ?? message; } catch { /* plain response */ }
    throw new Error(message);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export function login(email: string, password: string) {
  return authRequest<AuthUser>("/auth/login", { method: "POST", body: JSON.stringify({ email, password }) });
}

export function register(fullName: string, email: string, password: string) {
  return authRequest<AuthUser>("/auth/register", { method: "POST", body: JSON.stringify({ fullName, email, password, tenantId: "default" }) });
}

export function currentUser() {
  return authRequest<AuthUser>("/auth/me");
}

export function logout() {
  return authRequest<void>("/auth/logout", { method: "POST" });
}
