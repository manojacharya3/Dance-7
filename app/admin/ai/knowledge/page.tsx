"use client";

import { Loader2, Pencil, Plus, Trash2, X } from "lucide-react";
import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { Card, LoadingState, PageHeader } from "@/components/ui/card";
import { aiAdmin, getBranches, type BranchOption } from "@/lib/ai-chat";

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen">
      <Sidebar />
      <div className="lg:pl-[272px]">
        <Navbar />
        <main className="d7-page">{children}</main>
      </div>
    </div>
  );
}

type Field = { key: string; label: string; type: "text" | "number" | "textarea" | "select" | "date"; options?: string[]; required?: boolean; wide?: boolean };
type Tab = { resource: string; label: string; title: (row: any) => string; sub: (row: any) => string; fields: Field[] };

const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
const ACTIVE_OPTS = ["true", "false"];

const TABS: Tab[] = [
  {
    resource: "classes", label: "Classes",
    title: (r) => r.name, sub: (r) => [r.category, r.minAge != null || r.maxAge != null ? `ages ${r.minAge ?? 0}–${r.maxAge ?? "+"}` : "", r.experienceLevel].filter(Boolean).join(" · "),
    fields: [
      { key: "name", label: "Name", type: "text", required: true },
      { key: "category", label: "Category", type: "select", options: ["KIDS", "ADULT", "BHARATANATYAM", "OTHER"] },
      { key: "minAge", label: "Min age", type: "number" },
      { key: "maxAge", label: "Max age", type: "number" },
      { key: "experienceLevel", label: "Level", type: "select", options: ["BEGINNER", "INTERMEDIATE", "ADVANCED", "ALL"] },
      { key: "feeAmount", label: "Fee (₹)", type: "number" },
      { key: "description", label: "Description", type: "textarea", wide: true },
      { key: "active", label: "Active", type: "select", options: ACTIVE_OPTS },
    ],
  },
  {
    resource: "schedules", label: "Schedules",
    title: (r) => `${r.dayOfWeek} ${r.startTime}–${r.endTime}`, sub: (r) => [r.batchLabel, r.instructorName].filter(Boolean).join(" · "),
    fields: [
      { key: "aiClassId", label: "Class", type: "select", options: [], required: true },
      { key: "dayOfWeek", label: "Day", type: "select", options: DAYS, required: true },
      { key: "startTime", label: "Starts", type: "text", required: true },
      { key: "endTime", label: "Ends", type: "text", required: true },
      { key: "batchLabel", label: "Batch label", type: "text" },
      { key: "instructorName", label: "Instructor", type: "text" },
      { key: "active", label: "Active", type: "select", options: ACTIVE_OPTS },
    ],
  },
  {
    resource: "packages", label: "Packages",
    title: (r) => r.name, sub: (r) => [`₹${r.feeAmount}`, r.durationMonths ? `${r.durationMonths} mo` : ""].filter(Boolean).join(" · "),
    fields: [
      { key: "name", label: "Name", type: "text", required: true },
      { key: "durationMonths", label: "Duration (months)", type: "number" },
      { key: "feeAmount", label: "Fee (₹)", type: "number" },
      { key: "admissionFee", label: "Admission fee (₹)", type: "number" },
      { key: "description", label: "Description", type: "textarea", wide: true },
      { key: "active", label: "Active", type: "select", options: ACTIVE_OPTS },
    ],
  },
  {
    resource: "settings", label: "Settings & Fees",
    title: (r) => r.settingKey, sub: (r) => String(r.settingValue ?? "").slice(0, 80),
    fields: [
      { key: "settingKey", label: "Key (e.g. admission_fee, contact_phone)", type: "text", required: true },
      { key: "settingValue", label: "Value", type: "textarea", required: true, wide: true },
    ],
  },
  {
    resource: "faqs", label: "FAQs",
    title: (r) => r.question, sub: (r) => String(r.answer ?? "").slice(0, 80),
    fields: [
      { key: "question", label: "Question", type: "textarea", required: true, wide: true },
      { key: "answer", label: "Answer", type: "textarea", required: true, wide: true },
      { key: "keywords", label: "Keywords (comma separated)", type: "text", wide: true },
      { key: "sortOrder", label: "Order", type: "number" },
      { key: "active", label: "Active", type: "select", options: ACTIVE_OPTS },
    ],
  },
  {
    resource: "policies", label: "Policies",
    title: (r) => r.title, sub: (r) => String(r.body ?? "").slice(0, 80),
    fields: [
      { key: "title", label: "Title", type: "text", required: true },
      { key: "body", label: "Body", type: "textarea", required: true, wide: true },
      { key: "category", label: "Category", type: "text" },
      { key: "active", label: "Active", type: "select", options: ACTIVE_OPTS },
    ],
  },
  {
    resource: "offers", label: "Offers",
    title: (r) => r.title, sub: (r) => [r.validFrom, r.validUntil].filter(Boolean).join(" → "),
    fields: [
      { key: "title", label: "Title", type: "text", required: true },
      { key: "body", label: "Body", type: "textarea", required: true, wide: true },
      { key: "validFrom", label: "Valid from", type: "date" },
      { key: "validUntil", label: "Valid until", type: "date" },
      { key: "active", label: "Active", type: "select", options: ACTIVE_OPTS },
    ],
  },
];

export default function AiKnowledgePage() {
  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [branch, setBranch] = useState("whitefield");
  const [branchId, setBranchId] = useState<number | null>(null);
  const [tab, setTab] = useState(0);
  const [rows, setRows] = useState<any[] | null>(null);
  const [classes, setClasses] = useState<any[]>([]);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState<any | null>(null);
  const [form, setForm] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const current = TABS[tab];

  useEffect(() => {
    getBranches().then((list) => {
      setBranches(list);
      const match = list.find((b) => b.slug === branch);
      setBranchId(match ? match.id : null);
    }).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    setBranchId(branches.find((b) => b.slug === branch)?.id ?? null);
  }, [branch, branches]);

  function reload() {
    setRows(null);
    setError("");
    aiAdmin.list<any>(current.resource, branch).then(setRows).catch((e) => setError(e instanceof Error ? e.message : "Unable to load."));
    if (current.resource === "schedules") aiAdmin.list<any>("classes", branch).then(setClasses).catch(() => undefined);
  }

  useEffect(() => {
    setEditing(null);
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, branch]);

  function startEdit(row?: any) {
    const init: Record<string, string> = {};
    for (const f of current.fields) {
      const v = row?.[f.key];
      init[f.key] = v == null ? "" : String(v);
    }
    setForm(init);
    setEditing(row ?? {});
  }

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      const payload: Record<string, any> = { id: editing?.id ?? null, branchId };
      for (const f of current.fields) {
        const raw = form[f.key];
        if (raw === "") {
          payload[f.key] = null;
          continue;
        }
        if (f.type === "number") payload[f.key] = Number(raw);
        else if (f.key === "active") payload[f.key] = raw === "true";
        else if (f.key === "aiClassId") payload[f.key] = Number(raw);
        else if (f.key === "sortOrder") payload[f.key] = Number(raw);
        else payload[f.key] = raw;
      }
      await aiAdmin.save(current.resource, payload);
      setEditing(null);
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save.");
    } finally {
      setSaving(false);
    }
  }

  async function remove(id: number) {
    if (!window.confirm("Deactivate this entry? The assistant will stop using it immediately.")) return;
    try {
      await aiAdmin.remove(current.resource, branch, id);
      reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to delete.");
    }
  }

  function optionsFor(f: Field): string[] {
    if (f.key === "aiClassId") return classes.map((c) => `${c.id} — ${c.name}`);
    return f.options ?? [];
  }

  function optionValue(opt: string): string {
    return opt.includes(" — ") ? opt.split(" — ")[0] : opt;
  }

  return (
    <Shell>
      <PageHeader
        eyebrow="AI Assistant"
        title="Knowledge base"
        description="Everything the assistant knows. Changes apply to chat answers immediately — no code changes."
        actions={
          <select aria-label="Select branch" value={branch} onChange={(e) => setBranch(e.target.value)} className="d7-input sm:w-52">
            {branches.map((b) => (
              <option key={b.id} value={b.slug}>{b.name}</option>
            ))}
            {!branches.length && <option value="whitefield">Whitefield</option>}
          </select>
        }
      />
      <div className="mb-5 flex gap-2 overflow-x-auto pb-1">
        {TABS.map((t, i) => (
          <button
            key={t.resource}
            onClick={() => setTab(i)}
            className={`shrink-0 rounded-full px-4 py-2 text-sm font-semibold transition ${
              i === tab ? "bg-[#ff1a1a] text-white shadow-[0_0_20px_rgba(255,26,26,0.4)]" : "border border-[#2a2a2a] bg-[#161616] text-[#b3b3b3] hover:text-white"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>
      {error && <p className="d7-error mb-5">{error}</p>}
      {!rows ? (
        <Card><LoadingState label="Loading…" rows={4} /></Card>
      ) : (
        <Card>
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-base font-extrabold text-white">{current.label} · {rows.length}</h2>
            <button onClick={() => startEdit()} className="d7-btn-primary !px-3.5 !py-2 text-sm">
              <Plus size={15} /> Add
            </button>
          </div>
          {!rows.length ? (
            <p className="rounded-2xl bg-[#161616] p-6 text-center text-sm text-[#8a8a8a]">Nothing here yet — add the first entry.</p>
          ) : (
            <ul className="divide-y divide-[#222222]">
              {rows.map((row) => (
                <li key={row.id} className="flex items-start justify-between gap-3 py-3.5">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-bold text-white">{current.title(row)}</p>
                    <p className="mt-0.5 truncate text-xs text-[#8a8a8a]">{current.sub(row)}</p>
                    {row.active === false && <span className="d7-pill-slate mt-1.5">Inactive</span>}
                  </div>
                  <div className="flex shrink-0 gap-1">
                    <button aria-label="Edit" onClick={() => startEdit(row)} className="d7-icon-btn"><Pencil size={16} /></button>
                    <button aria-label="Deactivate" onClick={() => remove(row.id)} className="d7-icon-btn hover:!text-[#ff8080]"><Trash2 size={16} /></button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>
      )}

      {editing && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 p-3 backdrop-blur-sm sm:items-center" role="dialog" aria-modal="true" aria-label={`Edit ${current.label}`}>
          <form onSubmit={save} className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-[20px] border border-[#2a2a2a] bg-[#111111] p-5 sm:p-6">
            <div className="mb-4 flex items-start justify-between">
              <h2 className="text-lg font-extrabold text-white">{editing.id ? "Edit" : "Add"} · {current.label}</h2>
              <button type="button" aria-label="Close editor" onClick={() => setEditing(null)} className="d7-icon-btn"><X size={18} /></button>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {current.fields.map((f) => (
                <label key={f.key} className={`block text-sm text-[#b3b3b3] ${f.wide ? "sm:col-span-2" : ""}`}>
                  {f.label}{f.required ? " *" : ""}
                  {f.type === "textarea" ? (
                    <textarea required={f.required} value={form[f.key] ?? ""} onChange={(e) => setForm({ ...form, [f.key]: e.target.value })} rows={3} className="d7-input mt-1.5" />
                  ) : f.type === "select" ? (
                    <select value={form[f.key] ?? ""} onChange={(e) => setForm({ ...form, [f.key]: e.target.value })} className="d7-input mt-1.5">
                      <option value="">Select…</option>
                      {optionsFor(f).map((o) => (
                        <option key={o} value={optionValue(o)}>{o}</option>
                      ))}
                    </select>
                  ) : (
                    <input required={f.required} type={f.type === "number" ? "number" : f.type === "date" ? "date" : "text"} value={form[f.key] ?? ""} onChange={(e) => setForm({ ...form, [f.key]: e.target.value })} className="d7-input mt-1.5" />
                  )}
                </label>
              ))}
            </div>
            <button type="submit" disabled={saving} className="d7-btn-primary mt-4 w-full">
              {saving && <Loader2 size={16} className="animate-spin" />} {saving ? "Saving…" : "Save"}
            </button>
          </form>
        </div>
      )}
    </Shell>
  );
}
