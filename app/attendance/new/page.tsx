"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AttendanceForm } from "@/components/attendance/AttendanceForm";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { createAttendance, type AttendancePayload } from "@/lib/attendance";
import { getStudents, type Student } from "@/lib/students";

export default function NewAttendancePage() {
  const router = useRouter(); const [students, setStudents] = useState<Student[]>([]); const [error, setError] = useState("");
  useEffect(() => { getStudents("", 0, 100).then((result) => setStudents(result.content)).catch((requestError) => setError(requestError instanceof Error ? requestError.message : "Unable to load students.")); }, []);
  async function submit(payload: AttendancePayload) { await createAttendance(payload); router.push("/attendance"); }
  return <div className="min-h-screen"><Sidebar /><div className="lg:pl-[272px]"><Navbar /><main className="max-w-5xl d7-page"><Link href="/attendance" className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-violet-700"><ArrowLeft size={16} />Back to attendance</Link><div className="mb-8"><p className="mb-2 text-sm font-semibold text-violet-700">Studio register</p><h1 className="text-3xl font-semibold tracking-tight text-slate-900">Mark attendance</h1><p className="mt-2 text-sm text-slate-500">Record a student&apos;s status for a specific day.</p></div>{error ? <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p> : <AttendanceForm students={students} onSubmit={submit} />}</main></div></div>;
}