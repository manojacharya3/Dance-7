"use client";

import Link from "next/link";
import { ArrowLeft, CalendarCheck, Gauge, TrendingUp } from "lucide-react";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { getAttendanceByStudent, type Attendance } from "@/lib/attendance";
import { getStudent, type Student } from "@/lib/students";

export default function StudentPerformancePage() {
  const params = useParams<{ id: string }>();
  const [student, setStudent] = useState<Student | null>(null);
  const [records, setRecords] = useState<Attendance[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([getStudent(params.id), getAttendanceByStudent(Number(params.id))])
      .then(([studentResult, attendanceResult]) => {
        setStudent(studentResult);
        setRecords(attendanceResult.content.filter(isCurrentMonth));
      })
      .catch((requestError) => setError(requestError instanceof Error ? requestError.message : "Unable to load student performance."));
  }, [params.id]);

  const presentCount = records.filter((record) => record.status === "PRESENT").length;
  const attendancePercentage = records.length ? (presentCount * 100) / records.length : 0;
  const scoredRecords = records.filter((record) => record.performanceScore !== undefined && record.performanceScore !== null);
  const averagePerformanceScore = scoredRecords.length
    ? scoredRecords.reduce((total, record) => total + (record.performanceScore || 0), 0) / scoredRecords.length
    : 0;

  return <div className="min-h-screen bg-[#faf9f6]"><Sidebar /><div className="lg:pl-64"><Navbar /><main className="max-w-5xl px-6 py-8 lg:px-10"><Link href={`/students/${params.id}`} className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-[#667078] hover:text-[#21816b]"><ArrowLeft size={16} />Back to student</Link>{error ? <p className="rounded-lg bg-[#fff0ed] p-4 text-sm text-[#b84639]">{error}</p> : !student ? <p className="text-sm text-[#7b8285]">Loading performance...</p> : <><div className="mb-8"><p className="mb-2 text-sm font-semibold text-[#21816b]">Student performance</p><h1 className="text-3xl font-semibold tracking-tight text-[#18232b]">{student.firstName} {student.lastName}</h1><p className="mt-2 text-sm text-[#7b8285]">Monthly attendance and performance summary for {monthLabel()}.</p></div><section className="mb-8 grid gap-4 sm:grid-cols-3"><Metric label="Attendance %" value={`${attendancePercentage.toFixed(1)}%`} icon={<CalendarCheck size={18} />} /><Metric label="Average performance" value={`${averagePerformanceScore.toFixed(1)} / 10`} icon={<Gauge size={18} />} /><Metric label="Total classes" value={records.length} icon={<TrendingUp size={18} />} /></section><section className="rounded-xl border border-[#e8e5df] bg-white p-6"><div className="mb-5"><p className="text-xs font-bold uppercase tracking-[0.16em] text-[#21816b]">Monthly Summary</p><h2 className="mt-1 text-lg font-semibold text-[#18232b]">Attendance history</h2></div>{records.length ? <div className="overflow-x-auto"><table className="w-full min-w-[560px] text-left"><thead className="bg-[#fdfcf9] text-xs uppercase tracking-[0.12em] text-[#969d9d]"><tr><th className="px-4 py-3 font-semibold">Date</th><th className="px-4 py-3 font-semibold">Status</th><th className="px-4 py-3 font-semibold">Performance score</th><th className="px-4 py-3 font-semibold">Remarks</th></tr></thead><tbody>{records.map((record) => <HistoryRow key={record.id} record={record} />)}</tbody></table></div> : <p className="py-8 text-center text-sm text-[#7b8285]">No attendance records for this month.</p>}</section></>}</main></div></div>;
}

function HistoryRow({ record }: { record: Attendance }) {
  return <tr className="border-t border-[#eeeae3]"><td className="px-4 py-4 text-sm text-[#667078]">{record.attendanceDate}</td><td className="px-4 py-4 text-sm font-semibold text-[#18232b]">{record.status}</td><td className="px-4 py-4 text-sm text-[#667078]">{record.performanceScore ?? "-"}/10</td><td className="px-4 py-4 text-sm text-[#7b8285]">{record.remarks || "-"}</td></tr>;
}

function Metric({ label, value, icon }: { label: string; value: string | number; icon: React.ReactNode }) {
  return <div className="rounded-xl border border-[#e8e5df] bg-white p-5"><div className="flex items-center justify-between text-sm font-medium text-[#7b8285]">{label}<span className="text-[#21816b]">{icon}</span></div><p className="mt-3 text-2xl font-semibold text-[#18232b]">{value}</p></div>;
}

function isCurrentMonth(record: Attendance) {
  const date = new Date(`${record.attendanceDate}T00:00:00`);
  const now = new Date();
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
}

function monthLabel() { return new Intl.DateTimeFormat("en-US", { month: "long", year: "numeric" }).format(new Date()); }