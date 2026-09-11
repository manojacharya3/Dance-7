"use client";

import Link from "next/link";
import { ArrowRight, Inbox, MessagesSquare, Settings2, Sparkles, TrendingUp, UsersRound } from "lucide-react";
import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { Card, LoadingState, PageHeader, StatCard } from "@/components/ui/card";
import { aiAdmin, getBranches, type Analytics, type BranchOption, type Diagnostics } from "@/lib/ai-chat";

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

export default function AiAdminPage() {
  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [branch, setBranch] = useState("whitefield");
  const [data, setData] = useState<Analytics | null>(null);
  const [diag, setDiag] = useState<Diagnostics | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getBranches().then(setBranches).catch(() => undefined);
  }, []);
  useEffect(() => {
    setData(null);
    setDiag(null);
    setError("");
    aiAdmin.analytics(branch).then(setData).catch((e) => setError(e instanceof Error ? e.message : "Unable to load analytics."));
    aiAdmin.diagnostics(branch).then(setDiag).catch(() => undefined);
  }, [branch]);

  return (
    <Shell>
      <PageHeader
        eyebrow="AI Assistant"
        title="Front-desk command"
        description="Monitor conversations, intents and captured leads for each branch."
        actions={
          <>
            <select aria-label="Select branch" value={branch} onChange={(e) => setBranch(e.target.value)} className="d7-input sm:w-52">
              {branches.map((b) => (
                <option key={b.id} value={b.slug}>{b.name}</option>
              ))}
              {!branches.length && <option value="whitefield">Whitefield</option>}
            </select>
            <Link href="/admin/ai/knowledge" className="d7-btn-secondary"><Settings2 size={16} /> Knowledge</Link>
            <Link href="/admin/ai/leads" className="d7-btn-primary"><Inbox size={16} /> Leads inbox</Link>
          </>
        }
      />
      {error && <p className="d7-error mb-6">{error}</p>}
      {diag && (
        <Card className="mb-4">
          <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
            <span className="font-extrabold text-white">Data check · {diag.branchName} (id {diag.branchId})</span>
            <span className="text-[#8a8a8a]">dataset: {diag.datasetVersion ?? "not seeded"}</span>
            {Object.entries(diag.counts).map(([k, v]) => (
              <span key={k} className={v === 0 && k !== "offers" && k !== "leads" && k !== "conversations" ? "font-bold text-[#ff8080]" : "text-[#b3b3b3]"}>
                {k}: {v}
              </span>
            ))}
          </div>
        </Card>
      )}
      {!data ? (
        <Card><LoadingState label="Loading analytics…" rows={4} /></Card>
      ) : (
        <>
          <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Conversations" value={data.conversations} icon={<MessagesSquare size={18} />} tone="brand" sub="Visitor chat sessions" />
            <StatCard label="Messages" value={data.messages} icon={<Sparkles size={18} />} tone="blue" sub="Turns served" />
            <StatCard label="Leads captured" value={data.leads} icon={<UsersRound size={18} />} tone="emerald" sub={`${data.leadsNew} new`} />
            <StatCard label="Top intent" value={data.topIntents[0]?.intent ?? "—"} icon={<TrendingUp size={18} />} tone="amber" sub={data.topIntents[0] ? `${data.topIntents[0].count} turns` : "No turns yet"} />
          </section>
          <section className="mt-4 grid gap-4 lg:grid-cols-2">
            <Card>
              <h2 className="mb-4 text-base font-extrabold text-white">Top intents</h2>
              {data.topIntents.length ? (
                <ul className="space-y-2.5">
                  {data.topIntents.map((t) => (
                    <li key={t.intent} className="flex items-center justify-between rounded-xl border border-[#2a2a2a] bg-[#161616] px-3.5 py-2.5 text-sm">
                      <span className="font-bold text-white">{t.intent}</span>
                      <span className="d7-pill-violet">{t.count}</span>
                    </li>
                  ))}
                </ul>
              ) : <p className="text-sm text-[#8a8a8a]">No conversations yet.</p>}
            </Card>
            <Card>
              <h2 className="mb-4 text-base font-extrabold text-white">Lead funnel</h2>
              {data.leadsByStatus.length ? (
                <ul className="space-y-2.5">
                  {data.leadsByStatus.map((t) => (
                    <li key={t.status} className="flex items-center justify-between rounded-xl border border-[#2a2a2a] bg-[#161616] px-3.5 py-2.5 text-sm">
                      <span className="font-bold text-white">{t.status}</span>
                      <span className="d7-pill-green">{t.count}</span>
                    </li>
                  ))}
                </ul>
              ) : <p className="text-sm text-[#8a8a8a]">No leads yet.</p>}
              <Link href="/admin/ai/leads" className="mt-4 inline-flex items-center gap-1 text-sm font-bold text-[#ff6b6b] hover:text-white">
                Open leads inbox <ArrowRight size={15} />
              </Link>
            </Card>
          </section>
        </>
      )}
    </Shell>
  );
}
