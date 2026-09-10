"use client";

import Link from "next/link";
import { ArrowLeft, Pencil } from "lucide-react";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { getMembership, type Membership } from "@/lib/memberships";
import { getStudent, type Student } from "@/lib/students";

export default function MembershipDetailsPage() { const params = useParams<{ id: string }>(); const [membership, setMembership] = useState<Membership | null>(null); const [student, setStudent] = useState<Student | null>(null); const [error, setError] = useState(""); useEffect(() => { getMembership(params.id).then((item) => { setMembership(item); return getStudent(String(item.studentId)); }).then(setStudent).catch((e) => setError(e instanceof Error ? e.message : "Unable to load membership.")); }, [params.id]); return <Shell><Link href="/memberships" className="mb-6 inline-flex items-center gap-2 text-sm text-[#667078]"><ArrowLeft size={16} />Back to memberships</Link>{error ? <p className="rounded-lg bg-[#fff0ed] p-4 text-sm text-[#b84639]">{error}</p> : !membership ? <p className="text-sm text-[#7b8285]">Loading membership...</p> : <><div className="mb-8 flex items-end justify-between gap-4"><div><p className="mb-2 text-sm font-semibold text-[#21816b]">Membership details</p><h1 className="text-3xl font-semibold text-[#18232b]">{membership.planName}</h1><p className="mt-2 text-sm text-[#7b8285]">{student ? `${student.firstName} ${student.lastName}` : `Student #${membership.studentId}`}</p></div><Link href={`/memberships/${membership.id}/edit`} className="flex items-center gap-2 rounded-lg bg-[#18232b] px-4 py-2.5 text-sm font-semibold text-white"><Pencil size={16} />Edit membership</Link></div><section className="grid gap-5 rounded-xl border border-[#e8e5df] bg-white p-6 sm:grid-cols-2"><Detail label="Status" value={membership.status} /><Detail label="Fee" value={Number(membership.feeAmount).toFixed(2)} /><Detail label="Duration" value={`${membership.durationMonths} months`} /><Detail label="Dates" value={`${membership.startDate} to ${membership.endDate}`} /><Detail label="Student ID" value={String(membership.studentId)} /></section></>}</Shell>; }
function Shell({ children }: { children: React.ReactNode }) { return <div className="min-h-screen bg-[#faf9f6]"><Sidebar /><div className="lg:pl-64"><Navbar /><main className="max-w-5xl px-6 py-8 lg:px-10">{children}</main></div></div>; }
function Detail({ label, value }: { label: string; value: string }) { return <div><p className="text-xs font-semibold uppercase tracking-[0.1em] text-[#969d9d]">{label}</p><p className="mt-1 text-sm text-[#3d4a50]">{value}</p></div>; }
