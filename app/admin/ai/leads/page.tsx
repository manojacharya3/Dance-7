"use client";

import { Check, Loader2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { Card, LoadingState, PageHeader } from "@/components/ui/card";
import { aiAdmin, getBranches, type BranchOption, type ChatLead } from "@/lib/ai-chat";

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

const STATUSES = ["ALL", "NEW", "CONTACTED", "TRIAL", "ENROLLED", "LOST"];

export default function AiLeadsPage() {
  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [branch, setBranch] = useState("whitefield");
  const [status, setStatus] = useState("ALL");
  const [leads, setLeads] = useState<ChatLead[] | null>(null);
  const [error, setError] = useState("");
  const [updating, setUpdating] = useState<number | null>(null);

  useEffect(() => {
    getBranches().then(setBranches).catch(() => undefined);
  }, []);

  function reload(b = branch, s = status) {
    setLeads(null);
    setError("");
    aiAdmin.leads(b, s).then(setLeads).catch((e) => setError(e instanceof Error ? e.message : "Unable to load leads."));
  }

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branch, status]);

  async function setLeadStatus(id: number, next: string) {
    setUpdating(id);
    try {
      await aiAdmin.leadStatus(branch, id, next);
      reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to update lead.");
    } finally {
      setUpdating(null);
    }
  }

  return (
    <Shell>
      <PageHeader
        eyebrow="AI Assistant"
        title="Leads inbox"
        description="Every callback request captured by the assistant, newest first."
        actions={
          <>
            <select aria-label="Select branch" value={branch} onChange={(e) => setBranch(e.target.value)} className="d7-input sm:w-44">
              {branches.map((b) => (
                <option key={b.id} value={b.slug}>{b.name}</option>
              ))}
              {!branches.length && <option value="whitefield">Whitefield</option>}
            </select>
            <select aria-label="Filter by status" value={status} onChange={(e) => setStatus(e.target.value)} className="d7-input sm:w-44">
              {STATUSES.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </>
        }
      />
      {error && <p className="d7-error mb-6">{error}</p>}
      {!leads ? (
        <Card><LoadingState label="Loading leads…" rows={4} /></Card>
      ) : !leads.length ? (
        <Card><p className="p-8 text-center text-sm text-[#8a8a8a]">No leads in this view yet.</p></Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {leads.map((lead) => (
            <Card key={lead.id}>
              <div className="mb-3 flex items-start justify-between gap-3">
                <div>
                  <p className="text-base font-extrabold text-white">{lead.studentName || lead.parentName || `Lead #${lead.id}`}</p>
                  <p className="text-xs text-[#8a8a8a]">{new Date(lead.createdAt).toLocaleString()} · {lead.phone}</p>
                </div>
                <span className={lead.status === "NEW" ? "d7-pill-amber" : lead.status === "ENROLLED" ? "d7-pill-green" : "d7-pill-slate"}>{lead.status}</span>
              </div>
              <dl className="space-y-1.5 text-sm">
                {[
                  ["Parent", lead.parentName],
                  ["Age", lead.age ? String(lead.age) : ""],
                  ["Email", lead.email],
                  ["Interested class", lead.interestedClass],
                  ["Preferred batch", lead.preferredBatch],
                  ["Start date", lead.preferredStartDate],
                  ["Message", lead.message],
                ].filter(([, v]) => v).map(([k, v]) => (
                  <div key={k} className="flex gap-2">
                    <dt className="w-32 shrink-0 font-semibold text-[#8a8a8a]">{k}</dt>
                    <dd className="min-w-0 text-[#e5e5e5]">{v}</dd>
                  </div>
                ))}
              </dl>
              <div className="mt-4 flex flex-wrap gap-2">
                {["CONTACTED", "TRIAL", "ENROLLED", "LOST"].filter((s) => s !== lead.status).map((s) => (
                  <button key={s} disabled={updating === lead.id} onClick={() => setLeadStatus(lead.id, s)} className="d7-btn-secondary !px-3 !py-1.5 !text-xs">
                    {updating === lead.id ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />} Mark {s}
                  </button>
                ))}
              </div>
            </Card>
          ))}
        </div>
      )}
    </Shell>
  );
}
