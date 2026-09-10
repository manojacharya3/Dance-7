"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { getBranches, type Branch } from "@/lib/branches";
import { createFeedback, type FeedbackCategory, type FeedbackPriority } from "@/lib/feedback";

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

  useEffect(() => {
    getBranches().then(setBranches).catch((e) => setError(e instanceof Error ? e.message : "Unable to load branches."));
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !description.trim() || !branchId) {
      setError("Title, description, and branch are required.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      await createFeedback({ tenantId: "default", title: title.trim(), description: description.trim(), category, priority, branchId });
      router.push("/feedback");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to submit feedback.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#faf9f6]">
      <Sidebar />
      <div className="lg:pl-64">
        <Navbar />
        <main className="mx-auto max-w-2xl px-6 py-8 lg:px-10">
          <Link href="/feedback" className="mb-6 inline-flex items-center gap-2 text-sm text-[#667078]">
            <ArrowLeft size={16} />Back to feedback
          </Link>
          <h1 className="text-3xl font-semibold text-[#18232b]">New feedback</h1>
          <p className="mt-2 text-sm text-[#7b8285]">Available to Branch Heads and Instructors. Owners can view all feedback.</p>

          {error && <p className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}

          <form onSubmit={handleSubmit} className="mt-6 space-y-4 rounded-xl border border-[#e8e5df] bg-white p-6">
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
                  <option value="BUG">BUG</option>
                  <option value="IMPROVEMENT">IMPROVEMENT</option>
                  <option value="FEATURE_REQUEST">FEATURE_REQUEST</option>
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
            <button type="submit" disabled={saving} className="w-full rounded-lg bg-[#18232b] px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">
              {saving ? "Submitting…" : "Submit feedback"}
            </button>
          </form>
        </main>
      </div>
    </div>
  );
}
