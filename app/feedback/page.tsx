"use client";

import Link from "next/link";
import { MessageSquarePlus, Plus, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { getBranches, type Branch } from "@/lib/branches";
import { currentUser, type AuthUser } from "@/lib/auth";
import { getFeedbackList, updateFeedbackNotes, updateFeedbackStatus, type Feedback, type FeedbackStatus } from "@/lib/feedback";

const STATUSES: Array<"" | FeedbackStatus> = ["", "OPEN", "IN_PROGRESS", "RESOLVED"];
const CATEGORIES = ["", "BUG", "IMPROVEMENT", "FEATURE_REQUEST"];

export default function FeedbackPage() {
  const [items, setItems] = useState<Feedback[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [category, setCategory] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [me, setMe] = useState<AuthUser | null>(null);
  const [notesDrafts, setNotesDrafts] = useState<Record<number, string>>({});
  const [savingNotes, setSavingNotes] = useState<number | null>(null);

  useEffect(() => {
    currentUser().then(setMe).catch(() => undefined);
  }, []);

  useEffect(() => {
    getBranches().then(setBranches).catch(() => undefined);
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getFeedbackList({ search, status, category, page: 0, size: 50 })
      .then((page) => {
        if (!cancelled) setItems(page.content);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : "Unable to load feedback.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [search, status, category]);

  const branchMap = new Map(branches.map((b) => [b.id, b.name]));
  const triageRoles = (me?.roles ?? []).map((role) => role.toUpperCase().replace(/^ROLE_/, ""));
  const canTriage = triageRoles.includes("OWNER") || triageRoles.includes("ADMIN") || triageRoles.includes("DEVELOPER");

  async function handleStatus(item: Feedback, next: FeedbackStatus) {
    try {
      const updated = await updateFeedbackStatus(item.id, next);
      setItems((current) => current.map((entry) => (entry.id === item.id ? updated : entry)));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to update status.");
    }
  }

  async function handleNotes(item: Feedback) {
    const draft = notesDrafts[item.id] ?? item.internalNotes ?? "";
    setSavingNotes(item.id);
    try {
      const updated = await updateFeedbackNotes(item.id, draft);
      setItems((current) => current.map((entry) => (entry.id === item.id ? updated : entry)));
      setNotesDrafts((current) => ({ ...current, [item.id]: updated.internalNotes ?? "" }));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to save internal notes.");
    } finally {
      setSavingNotes(null);
    }
  }

  return (
    <div className="min-h-screen bg-[#faf9f6]">
      <Sidebar />
      <div className="lg:pl-64">
        <Navbar />
        <main className="px-6 py-8 lg:px-10">
          <div className="mb-8 flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
            <div>
              <p className="mb-2 text-sm font-semibold text-[#21816b]">Pilot feedback</p>
              <h1 className="text-3xl font-semibold">Feedback</h1>
              <p className="mt-2 text-sm text-[#7b8285]">Branch heads and instructors report bugs, improvements, and feature requests here.</p>
            </div>
            <Link href="/feedback/new" className="flex w-fit items-center gap-2 rounded-lg bg-[#18232b] px-4 py-2.5 text-sm font-semibold text-white">
              <Plus size={17} />New feedback
            </Link>
          </div>

          <div className="mb-6 flex flex-col gap-3 rounded-xl border border-[#e8e5df] bg-white p-4 lg:flex-row">
            <div className="relative flex-1">
              <Search size={17} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#969d9d]" />
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search by title, description, or reporter"
                className="w-full rounded-lg border px-3 py-2.5 pl-10 text-sm"
              />
            </div>
            <select value={status} onChange={(e) => setStatus(e.target.value)} className="rounded-lg border px-3 py-2.5 text-sm">
              <option value="">All statuses</option>
              {STATUSES.filter(Boolean).map((s) => (
                <option key={s} value={s}>{s.replace("_", " ")}</option>
              ))}
            </select>
            <select value={category} onChange={(e) => setCategory(e.target.value)} className="rounded-lg border px-3 py-2.5 text-sm">
              <option value="">All categories</option>
              {CATEGORIES.filter(Boolean).map((c) => (
                <option key={c} value={c}>{c.replace("_", " ")}</option>
              ))}
            </select>
          </div>

          {error && <p className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}

          <div className="overflow-hidden rounded-xl border border-[#e8e5df] bg-white">
            {loading ? (
              <p className="p-6 text-sm text-[#7b8285]">Loading feedback…</p>
            ) : items.length === 0 ? (
              <div className="flex flex-col items-center gap-3 p-10 text-center">
                <MessageSquarePlus size={28} className="text-[#969d9d]" />
                <p className="text-sm font-semibold">No feedback yet</p>
                <p className="text-sm text-[#7b8285]">Be the first to report an issue or request a feature.</p>
                <Link href="/feedback/new" className="rounded-lg bg-[#18232b] px-4 py-2 text-sm font-semibold text-white">Submit feedback</Link>
              </div>
            ) : (
              <ul className="divide-y divide-[#eeeae3]">
                {items.map((item) => (
                  <li key={item.id} className="p-5">
                    <div className="flex flex-wrap items-center gap-2 text-xs">
                      <span className="rounded-full bg-[#eef4f2] px-2.5 py-1 font-semibold text-[#21816b]">{item.category.replace("_", " ")}</span>
                      <span className="rounded-full bg-[#fff4d6] px-2.5 py-1 font-semibold text-[#8a6d00]">{item.priority}</span>
                      <span className="rounded-full bg-[#18232b] px-2.5 py-1 font-semibold text-white">{item.status.replace("_", " ")}</span>
                      <span className="text-[#969d9d]">{branchMap.get(item.branchId) ?? `Branch #${item.branchId}`} · {item.createdBy}</span>
                    </div>
                    <h2 className="mt-2 text-base font-semibold text-[#18232b]">{item.title}</h2>
                    <p className="mt-1 text-sm text-[#4b5560]">{item.description}</p>
                    {(item.internalNotes || canTriage) && (
                      <div className="mt-3 rounded-lg bg-[#faf9f6] p-3">
                        <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[#969d9d]">Internal notes</p>
                        {item.internalNotes && notesDrafts[item.id] === undefined && (
                          <p className="mt-1 text-sm text-[#4b5560]">{item.internalNotes}</p>
                        )}
                        {canTriage && (
                          <div className="mt-2 flex flex-col gap-2">
                            <textarea
                              value={notesDrafts[item.id] ?? item.internalNotes ?? ""}
                              onChange={(e) => setNotesDrafts((current) => ({ ...current, [item.id]: e.target.value }))}
                              placeholder="Add internal notes (developers only)"
                              rows={2}
                              className="w-full rounded-lg border px-3 py-2 text-sm"
                            />
                            <button
                              onClick={() => handleNotes(item)}
                              disabled={savingNotes === item.id}
                              className="w-fit rounded-lg bg-[#18232b] px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-60"
                            >
                              {savingNotes === item.id ? "Saving..." : "Save notes"}
                            </button>
                          </div>
                        )}
                      </div>
                    )}
                    <div className="mt-3 flex gap-2">
                      {(["OPEN", "IN_PROGRESS", "RESOLVED"] as FeedbackStatus[]).map((next) => (
                        <button
                          key={next}
                          disabled={item.status === next}
                          onClick={() => handleStatus(item, next)}
                          className="rounded-lg border px-3 py-1.5 text-xs font-semibold disabled:opacity-40"
                        >
                          {next.replace("_", " ")}
                        </button>
                      ))}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
