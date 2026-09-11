"use client";

import Link from "next/link";
import { ArrowLeft, ImagePlus, Loader2, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { getBranches, type Branch } from "@/lib/branches";
import {
  MAX_SCREENSHOTS,
  createFeedback,
  uploadScreenshot,
  validateScreenshot,
  type FeedbackCategory,
  type FeedbackPriority,
} from "@/lib/feedback";

const CATEGORIES: FeedbackCategory[] = ["BUG", "UI_ISSUE", "MOBILE_ISSUE", "PERFORMANCE", "CHATBOT", "PAYMENT", "IMPROVEMENT", "FEATURE_REQUEST", "OTHER"];

type PendingFile = { file: File; preview: string; progress: number; error?: string };

export default function NewFeedbackPage() {
  const router = useRouter();
  const [branches, setBranches] = useState<Branch[]>([]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState<FeedbackCategory>("BUG");
  const [priority, setPriority] = useState<FeedbackPriority>("MEDIUM");
  const [branchId, setBranchId] = useState(0);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [files, setFiles] = useState<PendingFile[]>([]);
  const [uploading, setUploading] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);

  useEffect(() => {
    getBranches().then(setBranches).catch((e) => setError(e instanceof Error ? e.message : "Unable to load branches."));
  }, []);

  useEffect(() => () => files.forEach((f) => URL.revokeObjectURL(f.preview)), [files]);

  function pickFiles(list: FileList | null) {
    if (!list) return;
    setFiles((current) => {
      const next = [...current];
      for (const file of Array.from(list)) {
        if (next.length >= MAX_SCREENSHOTS) break;
        const problem = validateScreenshot(file);
        next.push({ file, preview: URL.createObjectURL(file), progress: 0, error: problem ?? undefined });
      }
      return next;
    });
    if (fileInput.current) fileInput.current.value = "";
  }

  function removeFile(index: number) {
    setFiles((current) => {
      URL.revokeObjectURL(current[index].preview);
      return current.filter((_, i) => i !== index);
    });
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !description.trim() || !branchId) {
      setError("Title, description, and branch are required.");
      return;
    }
    setSaving(true);
    setUploading(false);
    setError("");
    try {
      const created = await createFeedback({ tenantId: "default", title: title.trim(), description: description.trim(), category, priority, branchId });
      const valid = files.filter((f) => !f.error);
      if (valid.length) {
        setUploading(true);
        const results = await Promise.all(
          valid.map((entry) =>
            uploadScreenshot(created.id, entry.file, (percent) =>
              setFiles((current) => current.map((c) => (c.preview === entry.preview ? { ...c, progress: percent } : c)))
            ).catch((err) => ({ failed: true as const, message: err instanceof Error ? err.message : "Upload failed." }))
          )
        );
        const failed = results.filter((r) => typeof r === "object" && "failed" in r).length;
        if (failed === valid.length) {
          setError("Feedback saved, but screenshot uploads failed. You can retry from the feedback list.");
          return;
        }
        if (failed > 0) setError(`Feedback saved, but ${failed} screenshot(s) failed to upload.`);
      }
      router.push("/feedback");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to submit feedback.");
    } finally {
      setSaving(false);
      setUploading(false);
    }
  }

  return (
    <div className="min-h-screen">
      <Sidebar />
      <div className="lg:pl-[272px]">
        <Navbar />
        <main className="mx-auto max-w-2xl d7-page">
          <Link href="/feedback" className="mb-6 inline-flex items-center gap-2 text-sm text-[#b3b3b3]">
            <ArrowLeft size={16} />Back to feedback
          </Link>
          <h1 className="text-3xl font-semibold text-white">New feedback</h1>
          <p className="mt-2 text-sm text-[#b3b3b3]">Available to Branch Heads and Instructors. Owners can view all feedback.</p>

          {error && <p className="mt-6 rounded-lg border border-[#ef4444]/40 bg-[#ef4444]/10 px-4 py-3 text-sm text-[#ff9999]">{error}</p>}

          <form onSubmit={handleSubmit} className="mt-6 space-y-4 rounded-xl border border-[#2a2a2a] bg-[#111111] p-6">
            <div>
              <label className="mb-1 block text-sm font-semibold">Title</label>
              <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} placeholder="e.g. Attendance save fails for evening batch" className="w-full rounded-lg border px-3 py-2.5 text-sm" />
            </div>
            <div>
              <label className="mb-1 block text-sm font-semibold">Description</label>
              <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={5} placeholder="Steps to reproduce, expected vs actual behaviour…" className="w-full rounded-lg border px-3 py-2.5 text-sm" />
            </div>
            <div className="grid gap-4 sm:grid-cols-3">
              <div>
                <label className="mb-1 block text-sm font-semibold">Category</label>
                <select value={category} onChange={(e) => setCategory(e.target.value as FeedbackCategory)} className="w-full rounded-lg border px-3 py-2.5 text-sm">
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>{c.replace(/_/g, " ")}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-sm font-semibold">Priority</label>
                <select value={priority} onChange={(e) => setPriority(e.target.value as FeedbackPriority)} className="w-full rounded-lg border px-3 py-2.5 text-sm">
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="URGENT">URGENT</option>
                </select>
              </div>
              <div>
                <label className="mb-1 block text-sm font-semibold">Branch</label>
                <select value={branchId} onChange={(e) => setBranchId(Number(e.target.value))} className="w-full rounded-lg border px-3 py-2.5 text-sm">
                  <option value={0}>Select branch</option>
                  {branches.map((branch) => (
                    <option key={branch.id} value={branch.id}>{branch.name}</option>
                  ))}
                </select>
              </div>
            </div>
            <div>
              <label className="mb-1 block text-sm font-semibold">
                Screenshots <span className="font-normal text-[#8a8a8a]">(optional, up to {MAX_SCREENSHOTS}, PNG/JPG/WEBP, 10 MB each)</span>
              </label>
              <input
                ref={fileInput}
                type="file"
                accept="image/png,image/jpeg,image/webp"
                multiple
                onChange={(e) => pickFiles(e.target.files)}
                className="hidden"
                aria-label="Attach screenshots"
              />
              <button
                type="button"
                onClick={() => fileInput.current?.click()}
                disabled={files.length >= MAX_SCREENSHOTS}
                className="flex w-full items-center justify-center gap-2 rounded-lg border border-dashed border-[#3a3a3a] bg-[#161616] px-4 py-4 text-sm font-semibold text-[#e5e5e5] transition hover:border-[#ff1a1a]/60 disabled:opacity-50"
              >
                <ImagePlus size={17} className="text-[#ff6b6b]" />
                {files.length >= MAX_SCREENSHOTS ? "Screenshot limit reached" : "Add screenshots from gallery or camera"}
              </button>
              {files.length > 0 && (
                <ul className="mt-3 grid grid-cols-3 gap-2 sm:grid-cols-5">
                  {files.map((entry, i) => (
                    <li key={entry.preview} className="relative overflow-hidden rounded-xl border border-[#2a2a2a] bg-[#161616]">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img src={entry.preview} alt={entry.file.name} className="h-20 w-full object-cover" />
                      <p className="truncate px-1.5 py-1 text-[10px] text-[#b3b3b3]">{entry.file.name}</p>
                      {entry.error ? (
                        <p className="px-1.5 pb-1.5 text-[10px] font-semibold text-[#ff8080]">{entry.error}</p>
                      ) : uploading || entry.progress > 0 ? (
                        <div className="mx-1.5 mb-1.5 h-1.5 overflow-hidden rounded-full bg-white/10">
                          <div className="h-full rounded-full bg-[#ff1a1a] transition-all" style={{ width: `${entry.progress}%` }} />
                        </div>
                      ) : null}
                      <button
                        type="button"
                        aria-label={`Remove ${entry.file.name}`}
                        onClick={() => removeFile(i)}
                        className="absolute right-1 top-1 rounded-full bg-black/70 p-1 text-white hover:bg-[#ff1a1a]"
                      >
                        <X size={13} />
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <button type="submit" disabled={saving} className="flex w-full items-center justify-center gap-2 rounded-lg bg-[#ff1a1a] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">
              {saving && <Loader2 size={15} className="animate-spin" />}
              {saving ? (uploading ? "Uploading screenshots…" : "Submitting…") : "Submit feedback"}
            </button>
          </form>
        </main>
      </div>
    </div>
  );
}
