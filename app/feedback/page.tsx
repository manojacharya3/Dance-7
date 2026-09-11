"use client";

import Link from "next/link";
import { ChevronLeft, ChevronRight, Download, Expand, Loader2, MessageSquarePlus, Paperclip, Plus, Search, X } from "lucide-react";
import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { getBranches, type Branch } from "@/lib/branches";
import { currentUser, type AuthUser } from "@/lib/auth";
import {
  attachmentUrl,
  deleteAttachment,
  getFeedbackList,
  listAttachments,
  updateFeedbackNotes,
  updateFeedbackStatus,
  type Feedback,
  type FeedbackAttachment,
  type FeedbackStatus,
} from "@/lib/feedback";

const STATUSES: Array<"" | FeedbackStatus> = ["", "OPEN", "IN_PROGRESS", "RESOLVED"];
const CATEGORIES = ["", "BUG", "UI_ISSUE", "MOBILE_ISSUE", "PERFORMANCE", "CHATBOT", "PAYMENT", "IMPROVEMENT", "FEATURE_REQUEST", "OTHER"];

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
  const [galleries, setGalleries] = useState<Record<number, FeedbackAttachment[]>>({});
  const [loadingGallery, setLoadingGallery] = useState<number | null>(null);
  const [lightbox, setLightbox] = useState<{ items: FeedbackAttachment[]; index: number } | null>(null);

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

  async function toggleGallery(item: Feedback) {
    if (galleries[item.id]) {
      setGalleries((current) => {
        const next = { ...current };
        delete next[item.id];
        return next;
      });
      return;
    }
    setLoadingGallery(item.id);
    try {
      const list = await listAttachments(item.id);
      setGalleries((current) => ({ ...current, [item.id]: list }));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to load screenshots.");
    } finally {
      setLoadingGallery(null);
    }
  }

  async function removeAttachment(item: Feedback, attachmentId: number) {
    if (!window.confirm("Delete this screenshot?")) return;
    try {
      await deleteAttachment(attachmentId);
      setGalleries((current) => ({
        ...current,
        [item.id]: (current[item.id] ?? []).filter((a) => a.id !== attachmentId),
      }));
      if (lightbox) {
        const items = lightbox.items.filter((a) => a.id !== attachmentId);
        setLightbox(items.length ? { items, index: Math.min(lightbox.index, items.length - 1) } : null);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to delete screenshot.");
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
    <div className="min-h-screen">
      <Sidebar />
      <div className="lg:pl-[272px]">
        <Navbar />
        <main className="d7-page">
          <div className="mb-8 flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
            <div>
              <p className="mb-2 text-sm font-semibold text-[#ff6b6b]">Pilot feedback</p>
              <h1 className="text-3xl font-semibold">Feedback</h1>
              <p className="mt-2 text-sm text-[#b3b3b3]">Branch heads and instructors report bugs, improvements, and feature requests here.</p>
            </div>
            <Link href="/feedback/new" className="flex w-fit items-center gap-2 rounded-lg bg-[#ff1a1a] px-4 py-2.5 text-sm font-semibold text-white">
              <Plus size={17} />New feedback
            </Link>
          </div>

          <div className="mb-6 flex flex-col gap-3 rounded-xl border border-[#2a2a2a] bg-[#111111] p-4 lg:flex-row">
            <div className="relative flex-1">
              <Search size={17} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#8a8a8a]" />
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

          {error && <p className="mb-4 rounded-lg border border-[#ef4444]/40 bg-[#ef4444]/10 px-4 py-3 text-sm text-[#ff9999]">{error}</p>}

          <div className="overflow-hidden rounded-xl border border-[#2a2a2a] bg-[#111111]">
            {loading ? (
              <p className="p-6 text-sm text-[#b3b3b3]">Loading feedback…</p>
            ) : items.length === 0 ? (
              <div className="flex flex-col items-center gap-3 p-10 text-center">
                <MessageSquarePlus size={28} className="text-[#8a8a8a]" />
                <p className="text-sm font-semibold">No feedback yet</p>
                <p className="text-sm text-[#b3b3b3]">Be the first to report an issue or request a feature.</p>
                <Link href="/feedback/new" className="rounded-lg bg-[#ff1a1a] px-4 py-2 text-sm font-semibold text-white">Submit feedback</Link>
              </div>
            ) : (
              <ul className="divide-y divide-[#222222]">
                {items.map((item) => (
                  <li key={item.id} className="p-5">
                    <div className="flex flex-wrap items-center gap-2 text-xs">
                      <span className="rounded-full bg-[#eef4f2] px-2.5 py-1 font-semibold text-[#ff6b6b]">{item.category.replace("_", " ")}</span>
                      <span className="rounded-full bg-[#fff4d6] px-2.5 py-1 font-semibold text-[#8a6d00]">{item.priority}</span>
                      <span className="rounded-full bg-[#ff1a1a] px-2.5 py-1 font-semibold text-white">{item.status.replace("_", " ")}</span>
                      <span className="text-[#8a8a8a]">{branchMap.get(item.branchId) ?? `Branch #${item.branchId}`} · {item.createdBy}</span>
                    </div>
                    <h2 className="mt-2 text-base font-semibold text-white">{item.title}</h2>
                    <p className="mt-1 text-sm text-[#4b5560]">{item.description}</p>
                    <div className="mt-3">
                      <button
                        onClick={() => toggleGallery(item)}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-[#2a2a2a] bg-[#161616] px-3 py-1.5 text-xs font-semibold text-[#e5e5e5] hover:border-[#ff1a1a]/60"
                      >
                        {loadingGallery === item.id ? (
                          <Loader2 size={13} className="animate-spin" />
                        ) : (
                          <Paperclip size={13} />
                        )}
                        {galleries[item.id] ? `Hide screenshots (${galleries[item.id].length})` : "View screenshots"}
                      </button>
                      {galleries[item.id] && (
                        galleries[item.id].length === 0 ? (
                          <p className="mt-2 text-xs text-[#8a8a8a]">No screenshots attached.</p>
                        ) : (
                          <ul className="mt-2 grid grid-cols-3 gap-2 sm:grid-cols-5">
                            {galleries[item.id].map((shot, i) => (
                              <li key={shot.id} className="group relative overflow-hidden rounded-xl border border-[#2a2a2a] bg-[#161616]">
                                <button
                                  onClick={() => setLightbox({ items: galleries[item.id], index: i })}
                                  className="block w-full"
                                  aria-label={`Open ${shot.fileName} fullscreen`}
                                >
                                  {/* eslint-disable-next-line @next/next/no-img-element */}
                                  <img src={attachmentUrl(shot)} alt={shot.fileName} loading="lazy" className="h-20 w-full object-cover" />
                                </button>
                                <p className="truncate px-1.5 py-1 text-[10px] text-[#b3b3b3]">{shot.fileName}</p>
                                <button
                                  onClick={() => removeAttachment(item, shot.id)}
                                  aria-label={`Delete ${shot.fileName}`}
                                  className="absolute right-1 top-1 rounded-full bg-black/70 p-1 text-white opacity-0 transition group-hover:opacity-100 hover:bg-[#ff1a1a] focus:opacity-100"
                                >
                                  <X size={13} />
                                </button>
                              </li>
                            ))}
                          </ul>
                        )
                      )}
                    </div>
                    {(item.internalNotes || canTriage) && (
                      <div className="mt-3 rounded-lg bg-[#161616] p-3">
                        <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8a8a8a]">Internal notes</p>
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
                              className="w-fit rounded-lg bg-[#ff1a1a] px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-60"
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

          {lightbox && lightbox.items.length > 0 && (
            <div
              className="fixed inset-0 z-50 flex flex-col bg-black/90 backdrop-blur-sm"
              role="dialog"
              aria-modal="true"
              aria-label="Screenshot preview"
              onClick={() => setLightbox(null)}
            >
              <div className="flex items-center justify-between p-4" onClick={(e) => e.stopPropagation()}>
                <p className="truncate text-sm font-semibold text-white">
                  {lightbox.items[lightbox.index].fileName} · {lightbox.index + 1} of {lightbox.items.length}
                </p>
                <div className="flex items-center gap-1">
                  <a
                    href={attachmentUrl(lightbox.items[lightbox.index])}
                    download={lightbox.items[lightbox.index].fileName}
                    onClick={(e) => e.stopPropagation()}
                    aria-label="Download screenshot"
                    className="rounded-xl p-2.5 text-[#b3b3b3] hover:bg-white/10 hover:text-white"
                  >
                    <Download size={19} />
                  </a>
                  <button aria-label="Expand fullscreen" onClick={() => document.documentElement.requestFullscreen?.().catch(() => undefined)} className="rounded-xl p-2.5 text-[#b3b3b3] hover:bg-white/10 hover:text-white">
                    <Expand size={19} />
                  </button>
                  <button aria-label="Close preview" onClick={() => setLightbox(null)} className="rounded-xl p-2.5 text-[#b3b3b3] hover:bg-white/10 hover:text-white">
                    <X size={19} />
                  </button>
                </div>
              </div>
              <div className="flex flex-1 items-center justify-center gap-2 px-2 pb-6" onClick={(e) => e.stopPropagation()}>
                <button
                  aria-label="Previous screenshot"
                  disabled={lightbox.items.length < 2}
                  onClick={() => setLightbox((lb) => (lb ? { items: lb.items, index: (lb.index - 1 + lb.items.length) % lb.items.length } : lb))}
                  className="rounded-xl p-3 text-white hover:bg-white/10 disabled:opacity-30"
                >
                  <ChevronLeft size={24} />
                </button>
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={attachmentUrl(lightbox.items[lightbox.index])}
                  alt={lightbox.items[lightbox.index].fileName}
                  className="max-h-[75vh] max-w-[86vw] rounded-xl object-contain"
                />
                <button
                  aria-label="Next screenshot"
                  disabled={lightbox.items.length < 2}
                  onClick={() => setLightbox((lb) => (lb ? { items: lb.items, index: (lb.index + 1) % lb.items.length } : lb))}
                  className="rounded-xl p-3 text-white hover:bg-white/10 disabled:opacity-30"
                >
                  <ChevronRight size={24} />
                </button>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
