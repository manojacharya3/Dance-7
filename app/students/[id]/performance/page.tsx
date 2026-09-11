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

  return <div className="min-h-screen"><Sidebar /><div className="lg:pl-[272px]"><Navbar /><main className="max-w-5xl d7-page"><Link href={`/students/${params.id}`} className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-violet-700"><ArrowLeft size={16} />Back to student</Link>{error ? <p className="rounded-lg bg-red-50 p-4 text-sm text-red-700">{error}</p> : !student ? <p className="text-sm text-slate-500">Loading performance...</p> : <><div className="mb-8"><p className="mb-2 text-sm font-semibold text-violet-700">Student performance</p><h1 className="text-3xl font-semibold tracking-tight text-slate-900">{student.firstName} {student.lastName}</h1><p className="mt-2 text-sm text-slate-500">Monthly attendance and performance summary for {monthLabel()}.</p></div><section className="mb-8 grid gap-4 sm:grid-cols-3"><Metric label="Attendance %" value={`${attendancePercentage.toFixed(1)}%`} icon={<CalendarCheck size={18} />} /><Metric label="Average performance" value={`${averagePerformanceScore.toFixed(1)} / 10`} icon={<Gauge size={18} />} /><Metric label="Total classes" value={records.length} icon={<TrendingUp size={18} />} /></section><section className="rounded-xl border border-slate-200/80 bg-white p-6"><div className="mb-5"><p className="text-xs font-bold uppercase tracking-[0.16em] text-violet-700">Monthly Summary</p><h2 className="mt-1 text-lg font-semibold text-slate-900">Attendance history</h2></div>{records.length ? <div className="overflow-x-auto"><table className="w-full min-w-[560px] text-left"><thead className="bg-slate-50 text-xs uppercase tracking-[0.12em] text-slate-400"><tr><th className="px-4 py-3 font-semibold">Date</th><th className="px-4 py-3 font-semibold">Status</th><th className="px-4 py-3 font-semibold">Performance score</th><th className="px-4 py-3 font-semibold">Remarks</th></tr></thead><tbody>{records.map((record) => <HistoryRow key={record.id} record={record} />)}</tbody></table></div> : <p className="py-8 text-center text-sm text-slate-500">No attendance records for this month.</p>}</section></>}</main></div></div>;
}

function HistoryRow({ record }: { record: Attendance }) {
  return <tr className="border-t border-slate-100"><td className="px-4 py-4 text-sm text-slate-500">{record.attendanceDate}</td><td className="px-4 py-4 text-sm font-semibold text-slate-900">{record.status}</td><td className="px-4 py-4 text-sm text-slate-500">{record.performanceScore ?? "-"}/10</td><td className="px-4 py-4 text-sm text-slate-500">{record.remarks || "-"}</td></tr>;
}

function Metric({ label, value, icon }: { label: string; value: string | number; icon: React.ReactNode }) {
  return <div className="rounded-xl border border-slate-200/80 bg-white p-5"><div className="flex items-center justify-between text-sm font-medium text-slate-500">{label}<span className="text-violet-700">{icon}</span></div><p className="mt-3 text-2xl font-semibold text-slate-900">{value}</p></div>;
}

function isCurrentMonth(record: Attendance) {
  const date = new Date(`${record.attendanceDate}T00:00:00`);
  const now = new Date();
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
}

function monthLabel() { return new Intl.DateTimeFormat("en-US", { month: "long", year: "numeric" }).format(new Date()); }