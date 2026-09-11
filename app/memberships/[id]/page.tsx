"use client";

import Link from "next/link";
import { ArrowLeft, Pencil } from "lucide-react";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { getMembership, type Membership } from "@/lib/memberships";
import { getStudent, type Student } from "@/lib/students";

export default function MembershipDetailsPage() { const params = useParams<{ id: string }>(); const [membership, setMembership] = useState<Membership | null>(null); const [student, setStudent] = useState<Student | null>(null); const [error, setError] = useState(""); useEffect(() => { getMembership(params.id).then((item) => { setMembership(item); return getStudent(String(item.studentId)); }).then(setStudent).catch((e) => setError(e instanceof Error ? e.message : "Unable to load membership.")); }, [params.id]); return <Shell><Link href="/memberships" className="mb-6 inline-flex items-center gap-2 text-sm text-slate-500"><ArrowLeft size={16} />Back to memberships</Link>{error ? <p className="rounded-lg bg-red-50 p-4 text-sm text-red-700">{error}</p> : !membership ? <p className="text-sm text-slate-500">Loading membership...</p> : <><div className="mb-8 flex items-end justify-between gap-4"><div><p className="mb-2 text-sm font-semibold text-violet-700">Membership details</p><h1 className="text-3xl font-semibold text-slate-900">{membership.planName}</h1><p className="mt-2 text-sm text-slate-500">{student ? `${student.firstName} ${student.lastName}` : `Student #${membership.studentId}`}</p></div><Link href={`/memberships/${membership.id}/edit`} className="flex items-center gap-2 rounded-lg bg-violet-600 px-4 py-2.5 text-sm font-semibold text-white"><Pencil size={16} />Edit membership</Link></div><section className="grid gap-5 rounded-xl border border-slate-200/80 bg-white p-6 sm:grid-cols-2"><Detail label="Status" value={membership.status} /><Detail label="Fee" value={Number(membership.feeAmount).toFixed(2)} /><Detail label="Duration" value={`${membership.durationMonths} months`} /><Detail label="Dates" value={`${membership.startDate} to ${membership.endDate}`} /><Detail label="Student ID" value={String(membership.studentId)} /></section></>}</Shell>; }
function Shell({ children }: { children: React.ReactNode }) { return <div className="min-h-screen"><Sidebar /><div className="lg:pl-[272px]"><Navbar /><main className="max-w-5xl d7-page">{children}</main></div></div>; }
function Detail({ label, value }: { label: string; value: string }) { return <div><p className="text-xs font-semibold uppercase tracking-[0.1em] text-slate-400">{label}</p><p className="mt-1 text-sm text-slate-700">{value}</p></div>; }
