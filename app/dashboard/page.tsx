"use client";

import Image from "next/image";
import Link from "next/link";
import {
  ArrowRight,
  Bell,
  CalendarClock,
  ClipboardCheck,
  CreditCard,
  Flame,
  Gauge,
  Plus,
  Receipt,
  Star,
  UsersRound,
  Wallet,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { Card, LoadingState, StatCard } from "@/components/ui/card";
import { currentUser, type AuthUser } from "@/lib/auth";
import { getAttendance, type Attendance } from "@/lib/attendance";
import { getBatches, getBatchStudents, type Batch, type StudentBatch } from "@/lib/batches";
import { getBranches, type Branch } from "@/lib/branches";
import { getInstructors, type Instructor } from "@/lib/instructors";
import { getMemberships, type Membership } from "@/lib/memberships";
import { getPayments, type Payment } from "@/lib/payments";
import { getStudents, type Student } from "@/lib/students";
import { getFeedbackList, type Feedback as FeedbackItem } from "@/lib/feedback";
import { formatCurrency } from "@/lib/currency";
import { formatTimeRange } from "@/lib/time";

type Scope = "OWNER" | "BRANCH_HEAD" | "INSTRUCTOR" | "DEVELOPER";

const SCOPE_LABEL: Record<Scope, string> = {
  OWNER: "Owner command center",
  BRANCH_HEAD: "Branch command center",
  INSTRUCTOR: "Instructor command center",
  DEVELOPER: "Developer overview",
};

export default function DashboardPage() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [instructors, setInstructors] = useState<Instructor[]>([]);
  const [batches, setBatches] = useState<Batch[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [attendance, setAttendance] = useState<Attendance[]>([]);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [assignments, setAssignments] = useState<Record<number, StudentBatch[]>>({});
  const [feedbackItems, setFeedbackItems] = useState<FeedbackItem[]>([]);
  const [error, setError] = useState("");
  const [authLoading, setAuthLoading] = useState(true);

  useEffect(() => {
    async function init() {
      let me: AuthUser | null = null;
      try {
        me = await currentUser();
        setUser(me);
      } catch {
        me = null;
      } finally {
        setAuthLoading(false);
      }
      const instructorScope = normalizedRoles(me?.roles).includes("INSTRUCTOR");
      const developerScope = normalizedRoles(me?.roles).includes("DEVELOPER");
      if (developerScope) {
        try {
          const [branchList, feedbackPage] = await Promise.all([getBranches(), getFeedbackList({ page: 0, size: 50 })]);
          setBranches(branchList);
          setFeedbackItems(feedbackPage.content);
        } catch (e) {
          setError(e instanceof Error && e.message ? e.message : "Unable to load dashboard.");
        }
        return;
      }
      try {
        const [branchList, instructorPage, batchPage, studentPage, attendancePage, membershipPage, paymentPage] =
          await Promise.all([
            getBranches(),
            getInstructors(),
            getBatches("", 0, 100),
            getStudents("", 0, 100),
            getAttendance(),
            getMemberships("", 0, 100),
            getPayments("", 0, 100),
          ]);
        setBranches(branchList);
        setInstructors(instructorPage.content);
        setBatches(batchPage.content);
        setStudents(studentPage.content);
        setAttendance(attendancePage.content);
        setMemberships(membershipPage.content);
        setPayments(paymentPage.content);
        const pairs = await Promise.all(
          batchPage.content.map(async (batch) => [batch.id, await getBatchStudents(batch.id)] as const)
        );
        const map: Record<number, StudentBatch[]> = {};
        pairs.forEach(([, records]) =>
          records.forEach((record) => {
            map[record.studentId] = [...(map[record.studentId] || []), record];
          })
        );
        setAssignments(map);
        if (instructorScope) {
          const linked =
            me?.instructorId != null
              ? instructorPage.content.find((item) => item.id === me?.instructorId)
              : undefined;
          const fallback =
            linked ??
            instructorPage.content.find(
              (item) =>
                item.email.toLowerCase() === me?.email.toLowerCase() ||
                `${item.firstName} ${item.lastName}`.toLowerCase() === me?.fullName.toLowerCase()
            );
          if (!fallback) setError("No instructor profile has been linked to this user.");
        }
      } catch (e) {
        setError(e instanceof Error && e.message ? e.message : "Unable to load dashboard.");
      }
    }
    init();
  }, []);

  const roles = normalizedRoles(user?.roles);
  const scope: Scope = roles.includes("INSTRUCTOR")
    ? "INSTRUCTOR"
    : roles.includes("BRANCH_HEAD")
      ? "BRANCH_HEAD"
      : roles.includes("DEVELOPER")
        ? "DEVELOPER"
        : "OWNER";
  const branchHeadBranchId = scope === "BRANCH_HEAD" ? (user?.branchId ?? null) : null;
  const instructor =
    scope === "INSTRUCTOR"
      ? (user?.instructorId != null
          ? (instructors.find((item) => item.id === user?.instructorId) ??
            instructors.find(
              (item) =>
                item.email.toLowerCase() === user?.email.toLowerCase() ||
                `${item.firstName} ${item.lastName}`.toLowerCase() === user?.fullName.toLowerCase()
            ))
          : instructors.find(
              (item) =>
                item.email.toLowerCase() === user?.email.toLowerCase() ||
                `${item.firstName} ${item.lastName}`.toLowerCase() === user?.fullName.toLowerCase()
            ))
      : undefined;
  const scopedBatches =
    scope === "INSTRUCTOR" && instructor
      ? batches.filter((batch) => batch.instructorId === instructor.id)
      : branchHeadBranchId != null
        ? batches.filter((batch) => batch.branchId === branchHeadBranchId)
        : batches;
  const scopedBatchIds = new Set(scopedBatches.map((batch) => batch.id));
  const scopedStudents =
    scope === "INSTRUCTOR"
      ? students.filter((student) => (assignments[student.id] || []).some((a) => scopedBatchIds.has(a.batchId)))
      : branchHeadBranchId != null
        ? students.filter((student) => student.branchId === branchHeadBranchId)
        : students;
  const scopedStudentsIds = new Set(scopedStudents.map((student) => student.id));
  const scopedAttendance = attendance.filter(
    (record) => scopedStudentsIds.has(record.studentId) && (!record.batchId || scopedBatchIds.has(record.batchId))
  );
  const scopedMemberships = memberships.filter((m) => scopedStudentsIds.has(m.studentId));
  const scopedPayments = payments.filter((p) => scopedStudentsIds.has(p.studentId));

  const paidPayments = scopedPayments.filter((item) => item.paymentStatus === "PAID");
  const activeMemberships = scopedMemberships.filter((item) => item.active && item.status === "ACTIVE");
  const attendancePct = percentage(scopedAttendance);
  const avgScore = average(scopedAttendance);
  const attendancePendingStudents = scopedStudents.filter(
    (student) => !scopedAttendance.some((record) => record.studentId === student.id)
  );
  const reviewsPending = scopedAttendance.filter((record) => record.performanceScore == null);
  const pendingPaymentsList = scopedPayments.filter((item) => item.active && item.paymentStatus === "PENDING");
  const overduePaymentsList = pendingPaymentsList.filter((item) => daysUntil(item.paymentDate) < 0);
  const expiringMembershipsList = scopedMemberships.filter(
    (item) => item.active && item.status === "ACTIVE" && daysUntil(item.endDate) >= 0 && daysUntil(item.endDate) <= 30
  );
  const revenue = sum(paidPayments);

  return (
    <div className="min-h-screen">
      <Sidebar />
      <div className="lg:pl-[272px]">
        <Navbar />
        <main className="d7-page">
          {/* Welcome banner — Dance7 command center */}
          <section className="relative mb-6 overflow-hidden rounded-[20px] border border-[#2a2a2a] bg-gradient-to-br from-[#161616] via-[#101010] to-[#2a0808] p-6 sm:mb-8 sm:p-8">
            <div
              className="pointer-events-none absolute inset-0"
              style={{ background: "radial-gradient(600px 220px at 90% 0%, rgba(255,26,26,0.22), transparent 65%)" }}
            />
            <div className="relative flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
              <div className="flex items-start gap-4">
                <Image
                  src="/brand/dance7-logo.jpg"
                  alt="Dance7 — The Art Factory"
                  width={64}
                  height={64}
                  priority
                  className="rounded-full object-cover ring-2 ring-[#ff1a1a]/50 shadow-[0_0_32px_rgba(255,26,26,0.45)]"
                />
                <div className="min-w-0">
                  <p className="d7-eyebrow">{SCOPE_LABEL[scope]}</p>
                  <h1 className="d7-h1 mt-2">
                    Good {daypart()}, {firstName(user?.fullName)}
                  </h1>
                  <p className="d7-sub">
                    Dance7 · The Art Factory — everything happening across your studio, live in one place.
                  </p>
                </div>
              </div>
              <div className="flex shrink-0 flex-col gap-2 sm:flex-row sm:items-center">
                <Link href="/reminders" className="d7-btn-secondary">
                  <Bell size={16} /> Open reminders
                </Link>
                <Link href="/students/new" className="d7-btn-primary">
                  <Plus size={16} /> Add student
                </Link>
              </div>
            </div>
          </section>

          {error && (
            <p role="alert" className="d7-error mb-6">
              {error}
            </p>
          )}

          {authLoading ? (
            <Card>
              <LoadingState label="Loading command center…" rows={5} />
            </Card>
          ) : scope === "DEVELOPER" ? (
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <StatCard label="Branches" value={branches.length} icon={<ClipboardCheck size={18} />} tone="brand" sub="Workspaces in scope" />
              <StatCard label="Open feedback" value={feedbackItems.length} icon={<Star size={18} />} tone="amber" sub="Triage in Feedback" />
              <StatCard label="Role" value="Developer" icon={<Gauge size={18} />} tone="slate" sub="Read-only operations" />
              <StatCard label="System" value="Healthy" icon={<UsersRound size={18} />} tone="emerald" sub="APIs reachable" />
            </section>
          ) : (
            <>
              {/* Revenue summary */}
              <section className="relative mb-4 overflow-hidden rounded-[20px] border border-[#ff1a1a]/30 bg-gradient-to-r from-[#1c0a0a] to-[#111111] p-6 sm:p-7">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                  <div>
                    <p className="flex items-center gap-2 text-xs font-bold uppercase tracking-[0.22em] text-[#ff6b6b]">
                      <Flame size={14} /> Revenue collected
                    </p>
                    <p className="mt-2 text-4xl font-extrabold tracking-tight text-white sm:text-5xl">
                      {formatCurrency(revenue)}
                    </p>
                    <p className="mt-1.5 text-sm text-[#b3b3b3]">
                      {paidPayments.length} paid payments · {formatCurrency(sum(pendingPaymentsList))} pending
                      {overduePaymentsList.length ? ` · ${overduePaymentsList.length} overdue` : ""}
                    </p>
                  </div>
                  <Link href="/payments/new" className="d7-btn-primary shrink-0">
                    <Plus size={16} /> Record payment
                  </Link>
                </div>
              </section>

              <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                <StatCard label={scope === "INSTRUCTOR" ? "My students" : "Students"} value={scopedStudents.length} icon={<UsersRound size={18} />} tone="brand" sub={`${scopedBatches.length} batches · ${instructors.length} instructors`} />
                <StatCard label="Active memberships" value={activeMemberships.length} icon={<CreditCard size={18} />} tone="emerald" sub="Currently valid plans" />
                <StatCard label="Attendance rate" value={`${attendancePct}%`} icon={<Gauge size={18} />} tone="blue" sub={`${avgScore} / 10 avg performance`} />
                <StatCard label="Expiring in 30 days" value={expiringMembershipsList.length} icon={<CalendarClock size={18} />} tone="amber" sub="Needs renewal follow-up" />
              </section>

              <section className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                <StatCard label="Pending collection" value={formatCurrency(sum(pendingPaymentsList))} icon={<Receipt size={18} />} tone="amber" sub={`${pendingPaymentsList.length} pending · ${overduePaymentsList.length} overdue`} />
                <StatCard label="Attendance pending" value={attendancePendingStudents.length} icon={<ClipboardCheck size={18} />} tone="blue" sub="Students with no records" />
                <StatCard label="Reviews pending" value={reviewsPending.length} icon={<Star size={18} />} tone="slate" sub="Records missing scores" />
                <StatCard label="Collection health" value={paidPayments.length && scopedPayments.length ? `${Math.round((paidPayments.length / scopedPayments.length) * 100)}%` : "—"} icon={<Wallet size={18} />} tone="emerald" sub="Paid share of payments" />
              </section>

              <section className="mt-4 grid gap-4 lg:grid-cols-2">
                <Card>
                  <SectionHeading title="Needs your attention" linkHref="/reminders" linkLabel="View all" />
                  <ul className="space-y-3">
                    <AttentionRow label="Overdue payments" value={String(overduePaymentsList.length)} hint={overduePaymentsList.slice(0, 3).map((p) => `${studentName(p.studentId, scopedStudents)} · ${formatCurrency(Number(p.amount))}`).join("  •  ") || "Nothing overdue. Encore-worthy."} />
                    <AttentionRow label="Memberships expiring" value={String(expiringMembershipsList.length)} hint={expiringMembershipsList.slice(0, 3).map((m) => `${studentName(m.studentId, scopedStudents)} · ${m.planName}`).join("  •  ") || "No renewals due in 30 days."} />
                    <AttentionRow label="Attendance to take" value={String(attendancePendingStudents.length)} hint={attendancePendingStudents.slice(0, 3).map((s) => `${s.firstName} ${s.lastName}`).join("  •  ") || "All students have records."} />
                  </ul>
                </Card>
                <Card>
                  <SectionHeading title="Upcoming batches" linkHref="/batches" linkLabel="View batches" />
                  <div className="space-y-3">
                    {scopedBatches.slice(0, 5).map((batch) => (
                      <div key={batch.id} className="flex items-center justify-between gap-3 rounded-2xl border border-[#2a2a2a] bg-[#161616] p-3.5">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-bold text-white">{batch.batchName}</p>
                          <p className="truncate text-xs text-[#8a8a8a]">
                            {branchName(batch.branchId, branches)} · {formatTimeRange(batch.startTime, batch.endTime)} · Cap {batch.capacity}
                          </p>
                        </div>
                        <span className={batch.active ? "d7-pill-green" : "d7-pill-slate"}>{batch.active ? "Scheduled" : "Inactive"}</span>
                      </div>
                    ))}
                    {!scopedBatches.length && (
                      <p className="rounded-2xl bg-[#161616] p-5 text-sm text-[#b3b3b3]">
                        No batches assigned. Ask your branch head to assign batches.
                      </p>
                    )}
                  </div>
                </Card>
              </section>

              <section className="mt-4">
                <Card>
                  <SectionHeading title="Quick actions" />
                  <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                    {[
                      { href: "/attendance/new", label: "Mark attendance", hint: "Single student entry" },
                      { href: "/attendance/batch", label: "Batch attendance", hint: "Whole class at once" },
                      { href: "/payments/new", label: "Record payment", hint: "Cash, UPI, card" },
                      { href: "/memberships/new", label: "New membership", hint: "Assign a plan" },
                    ].map((a) => (
                      <Link key={a.href} href={a.href} className="group rounded-2xl border border-[#2a2a2a] bg-[#161616] p-4 transition hover:border-[#ff1a1a]/60 hover:bg-[#ff1a1a]/5">
                        <p className="flex items-center justify-between text-sm font-bold text-white">
                          {a.label} <ArrowRight size={16} className="text-[#6b6b6b] transition group-hover:translate-x-0.5 group-hover:text-[#ff4d4d]" />
                        </p>
                        <p className="mt-1 text-xs text-[#8a8a8a]">{a.hint}</p>
                      </Link>
                    ))}
                  </div>
                </Card>
              </section>
            </>
          )}
        </main>
      </div>
    </div>
  );
}

function SectionHeading({ title, linkHref, linkLabel }: { title: string; linkHref?: string; linkLabel?: string }) {
  return (
    <div className="mb-4 flex items-center justify-between">
      <h2 className="text-base font-extrabold tracking-tight text-white">{title}</h2>
      {linkHref ? (
        <Link href={linkHref} className="inline-flex items-center gap-1 text-sm font-bold text-[#ff6b6b] hover:text-white">
          {linkLabel} <ArrowRight size={15} />
        </Link>
      ) : null}
    </div>
  );
}

function AttentionRow({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <li className="flex items-start justify-between gap-4 rounded-2xl border border-[#2a2a2a] bg-[#161616] p-3.5">
      <div className="min-w-0">
        <p className="text-sm font-bold text-white">{label}</p>
        <p className="mt-0.5 truncate text-xs leading-5 text-[#8a8a8a]">{hint}</p>
      </div>
      <span className="d7-pill-violet shrink-0">{value}</span>
    </li>
  );
}

function normalizedRoles(roles: string[] | undefined): string[] {
  return (roles ?? []).map((role) => (role.startsWith("ROLE_") ? role.slice(5) : role));
}
function firstName(full?: string | null) {
  if (!full) return "there";
  return full.split(" ")[0];
}
function daypart() {
  const h = new Date().getHours();
  if (h < 12) return "morning";
  if (h < 17) return "afternoon";
  return "evening";
}
function branchName(id: number, branches: Branch[]) {
  return branches.find((b) => b.id === id)?.name || "Branch";
}
function daysUntil(date: string) {
  return Math.ceil((new Date(`${date}T00:00:00`).getTime() - new Date().setHours(0, 0, 0, 0)) / 86400000);
}
function studentName(studentId: number, students: Student[]): string {
  const s = students.find((item) => item.id === studentId);
  return s ? `${s.firstName} ${s.lastName}` : `Student #${studentId}`;
}
function sum(records: Payment[]) {
  return records.reduce((total, payment) => total + Number(payment.amount), 0);
}
function percentage(records: Attendance[]) {
  return records.length
    ? Number(((records.filter((r) => r.status === "PRESENT").length * 100) / records.length).toFixed(1))
    : 0;
}
function average(records: Attendance[]) {
  const scored = records.filter((record) => record.performanceScore != null);
  return scored.length
    ? (scored.reduce((total, record) => total + (record.performanceScore || 0), 0) / scored.length).toFixed(1)
    : "0.0";
}
