export type Student = {
  id: number;
  tenantId: string;
  branchId: number;
  firstName: string;
  lastName: string;
  dateOfBirth?: string;
  gender?: string;
  email?: string;
  phone?: string;
  address?: string;
  emergencyContact?: string;
  parentName?: string;
  parentPhone?: string;
  danceStyle?: string;
  skillLevel?: string;
  medicalNotes?: string;
  studentPhotoUrl?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type StudentPayload = Omit<Student, "id" | "createdAt" | "updatedAt">;

export type StudentPage = {
  content: Student[];
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
    throw new Error(message || `Request failed with status ${response.status}.`);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export function getStudents(search: string, page: number, size = 10) {
  const params = new URLSearchParams({ tenantId: "default", page: String(page), size: String(size) });
  if (search.trim()) params.set("search", search.trim());
  return request<StudentPage>(`/students?${params.toString()}`);
}

export function getStudent(id: string) {
  return request<Student>(`/students/${id}?tenantId=default`);
}

export function createStudent(student: StudentPayload) {
  return request<Student>("/students", { method: "POST", body: JSON.stringify(student) });
}

export function updateStudent(id: string, student: StudentPayload) {
  return request<Student>(`/students/${id}`, { method: "PUT", body: JSON.stringify(student) });
}

export function deleteStudent(id: number) {
  return request<void>(`/students/${id}?tenantId=default`, { method: "DELETE" });
}
