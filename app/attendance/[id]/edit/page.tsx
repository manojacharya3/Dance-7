"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AttendanceForm } from "@/components/attendance/AttendanceForm";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { getAttendanceById, updateAttendance, type Attendance, type AttendancePayload } from "@/lib/attendance";
import { getStudents, type Student } from "@/lib/students";

export default function EditAttendancePage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [record, setRecord] = useState<Attendance | null>(null);
  const [students, setStudents] = useState<Student[]>([]);
  const [error, setError] = useState("");
  useEffect(() => { Promise.all([getAttendanceById(params.id), getStudents("", 0, 100)]).then(([attendance, studentPage]) => { setRecord(attendance); setStudents(studentPage.content); }).catch((requestError) => setError(requestError instanceof Error ? requestError.message : "Unable to load attendance.")); }, [params.id]);
  async function submit(payload: AttendancePayload) { await updateAttendance(params.id ? Number(params.id) : 0, payload); router.push("/attendance"); }
  const initialValue = record ? { tenantId: record.tenantId, studentId: record.studentId, batchId: record.batchId, attendanceDate: record.attendanceDate, status: record.status, performanceScore: record.performanceScore, remarks: record.remarks } : undefined;
  return <div className="min-h-screen"><Sidebar /><div className="lg:pl-[272px]"><Navbar /><main className="max-w-5xl d7-page"><Link href="/attendance" className="mb-6 inline-flex items-center gap-2 text-sm text-[#b3b3b3]"><ArrowLeft size={16} />Back to attendance</Link><div className="mb-8"><p className="mb-2 text-sm font-semibold text-[#ff6b6b]">Attendance record</p><h1 className="text-3xl font-semibold text-white">Edit attendance</h1><p className="mt-2 text-sm text-[#b3b3b3]">Update status, score, date, or remarks.</p></div>{error ? <p className="rounded-lg bg-[#ef4444]/10 p-4 text-sm text-[#ff9999]">{error}</p> : !record ? <p className="text-sm text-[#b3b3b3]">Loading attendance...</p> : <AttendanceForm students={students} initialValue={initialValue} onSubmit={submit} />}</main></div></div>;
}