"use client";

import Image from "next/image";
import { Loader2, MessageCircle, Send, Sparkles, User, X } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { getBranches, sendMessage, submitLead, type BranchOption, type ChatReply } from "@/lib/ai-chat";

type Msg = { role: "user" | "assistant"; text: string };

const QUICK_ACTIONS: [string, string][] = [
  ["View Classes", "What classes do you offer?"],
  ["Kids Classes", "Do you have classes for kids?"],
  ["Adult Classes", "What adult classes do you have?"],
  ["Bharatanatyam", "Do you teach Bharatanatyam?"],
  ["Fees & Packages", "What are the fees and packages?"],
  ["Contact Studio", "How do I contact the studio?"],
];

const VISITOR_KEY = "dance7-ai-visitor";

export function AiChatWidget() {
  const [open, setOpen] = useState(false);
  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [branch, setBranch] = useState("");
  const [visitorId, setVisitorId] = useState<string | undefined>(undefined);
  const [conversationId, setConversationId] = useState<number | undefined>(undefined);
  const [messages, setMessages] = useState<Msg[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [leadPrompt, setLeadPrompt] = useState(false);
  const [leadOpen, setLeadOpen] = useState(false);
  const [error, setError] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    try {
      const saved = localStorage.getItem(VISITOR_KEY);
      if (saved) setVisitorId(saved);
    } catch {
      /* private mode */
    }
    getBranches()
      .then((list) => {
        setBranches(list);
        const preferred = list.find((b) => b.slug === "whitefield") ?? list[0];
        if (preferred) setBranch((current) => current || preferred.slug);
      })
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, open]);

  const greet = useCallback(
    (slug: string) => {
      const name = branches.find((b) => b.slug === slug)?.name ?? "Whitefield";
      setMessages([
        {
          role: "assistant",
          text: `Hi! Welcome to Dance7 ${name}. Ask me about classes, timings, fees — or tap an option below to explore.`,
        },
      ]);
      setConversationId(undefined);
      setLeadPrompt(false);
    },
    [branches]
  );

  useEffect(() => {
    if (open && messages.length === 0 && branch) greet(branch);
  }, [open, messages.length, branch, greet]);

  function switchBranch(slug: string) {
    setBranch(slug);
    greet(slug);
  }

  async function ask(text: string) {
    const clean = text.trim();
    if (!clean || sending || !branch) return;
    setInput("");
    setError("");
    setMessages((m) => [...m, { role: "user", text: clean }]);
    setSending(true);
    try {
      const res: ChatReply = await sendMessage({ branch, conversationId, visitorId, message: clean });
      setConversationId(res.conversationId);
      if (res.visitorId && res.visitorId !== visitorId) {
        setVisitorId(res.visitorId);
        try {
          localStorage.setItem(VISITOR_KEY, res.visitorId);
        } catch {
          /* ignore */
        }
      }
      setMessages((m) => [...m, { role: "assistant", text: res.reply }]);
      setLeadPrompt(res.leadPrompt);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to reach the studio assistant.");
    } finally {
      setSending(false);
    }
  }

  return (
    <>
      {/* Floating button */}
      <button
        aria-label={open ? "Close Dance7 assistant" : "Open Dance7 assistant"}
        onClick={() => setOpen((o) => !o)}
        className="fixed bottom-20 right-4 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-[#ff1a1a] text-white shadow-[0_0_28px_rgba(255,26,26,0.55)] transition hover:bg-[#e60000] active:scale-95 lg:bottom-6 lg:right-6"
      >
        {open ? <X size={24} /> : <MessageCircle size={24} />}
      </button>

      {open && (
        <div
          role="dialog"
          aria-label="Dance7 studio assistant"
          className="fixed inset-x-3 bottom-36 top-16 z-40 flex flex-col overflow-hidden rounded-[20px] border border-[#2a2a2a] bg-[#0b0b0b] shadow-[0_24px_64px_-16px_rgba(0,0,0,0.9)] sm:inset-x-auto sm:right-6 sm:top-auto sm:h-[560px] sm:w-[380px] sm:bottom-24 lg:bottom-24"
        >
          {/* Header */}
          <div className="border-b border-[#1f1f1f] bg-gradient-to-r from-[#1c0a0a] to-[#0b0b0b] p-4">
            <div className="flex items-center gap-3">
              <Image
                src="/brand/dance7-logo.jpg"
                alt="Dance7 — The Art Factory"
                width={40}
                height={40}
                className="rounded-full object-cover ring-1 ring-[#ff1a1a]/50"
              />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-extrabold uppercase tracking-[0.08em] text-white">Dance7 Assistant</p>
                <p className="text-[10px] font-semibold uppercase tracking-[0.28em] text-[#8a8a8a]">The Art Factory</p>
              </div>
              <select
                aria-label="Choose branch"
                value={branch}
                onChange={(e) => switchBranch(e.target.value)}
                className="max-w-[128px] rounded-xl border border-[#2a2a2a] bg-[#1a1a1a] px-2 py-2 text-xs font-semibold text-white outline-none focus:border-[#ff1a1a]"
              >
                {branches.map((b) => (
                  <option key={b.id} value={b.slug}>
                    {b.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Messages */}
          <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto p-4" aria-live="polite">
            {messages.map((m, i) => (
              <div key={i} className={`flex gap-2 ${m.role === "user" ? "justify-end" : "justify-start"}`}>
                {m.role === "assistant" && (
                  <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[#ff1a1a]/15 text-[#ff6b6b]">
                    <Sparkles size={14} />
                  </span>
                )}
                <p
                  className={`max-w-[80%] rounded-2xl px-3.5 py-2.5 text-sm leading-6 ${
                    m.role === "user"
                      ? "rounded-br-md bg-[#ff1a1a] font-medium text-white"
                      : "rounded-bl-md border border-[#2a2a2a] bg-[#161616] text-[#e5e5e5]"
                  }`}
                >
                  {m.text}
                </p>
                {m.role === "user" && (
                  <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-white/10 text-[#b3b3b3]">
                    <User size={14} />
                  </span>
                )}
              </div>
            ))}
            {sending && (
              <div className="flex items-center gap-2 text-sm text-[#8a8a8a]">
                <Loader2 size={15} className="animate-spin text-[#ff6b6b]" /> Finding the latest studio info…
              </div>
            )}
            {error && <p className="d7-error">{error}</p>}
            {leadPrompt && !leadOpen && (
              <button onClick={() => setLeadOpen(true)} className="d7-btn-primary w-full !py-2.5 text-sm">
                Share my details for a callback
              </button>
            )}
          </div>

          {/* Quick actions */}
          <div className="flex gap-2 overflow-x-auto border-t border-[#1f1f1f] px-3 py-2.5">
            {QUICK_ACTIONS.map(([label, prompt]) => (
              <button
                key={label}
                onClick={() => ask(prompt)}
                disabled={sending}
                className="shrink-0 rounded-full border border-[#2a2a2a] bg-[#161616] px-3 py-1.5 text-xs font-semibold text-[#e5e5e5] transition hover:border-[#ff1a1a]/60 hover:text-white disabled:opacity-50"
              >
                {label}
              </button>
            ))}
          </div>

          {/* Input */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              ask(input);
            }}
            className="flex items-center gap-2 border-t border-[#1f1f1f] p-3"
          >
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask about classes, fees…"
              aria-label="Ask the studio assistant"
              maxLength={1000}
              className="d7-input"
            />
            <button type="submit" aria-label="Send message" disabled={sending || !input.trim()} className="d7-btn-primary !px-3.5">
              <Send size={16} />
            </button>
          </form>
        </div>
      )}

      {leadOpen && (
        <LeadForm
          branch={branch}
          conversationId={conversationId}
          visitorId={visitorId}
          onClose={() => setLeadOpen(false)}
          onDone={(confirmation) => {
            setLeadOpen(false);
            setLeadPrompt(false);
            setMessages((m) => [...m, { role: "assistant", text: confirmation }]);
          }}
        />
      )}
    </>
  );
}

function LeadForm({
  branch,
  conversationId,
  visitorId,
  onClose,
  onDone,
}: {
  branch: string;
  conversationId?: number;
  visitorId?: string;
  onClose: () => void;
  onDone: (confirmation: string) => void;
}) {
  const [form, setForm] = useState({ studentName: "", parentName: "", age: "", phone: "", email: "", interestedClass: "", preferredBatch: "", preferredStartDate: "", message: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      const res = await submitLead({
        branch,
        conversationId,
        visitorId,
        studentName: form.studentName || undefined,
        parentName: form.parentName || undefined,
        age: form.age ? Number(form.age) : undefined,
        phone: form.phone,
        email: form.email || undefined,
        interestedClass: form.interestedClass || undefined,
        preferredBatch: form.preferredBatch || undefined,
        preferredStartDate: form.preferredStartDate || undefined,
        message: form.message || undefined,
      });
      onDone(res.confirmation);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save your details.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 p-3 backdrop-blur-sm sm:items-center" role="dialog" aria-modal="true" aria-label="Request a callback">
      <form onSubmit={submit} className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-[20px] border border-[#2a2a2a] bg-[#111111] p-5 sm:p-6">
        <div className="mb-4 flex items-start justify-between">
          <div>
            <p className="d7-eyebrow">Request a callback</p>
            <h2 className="mt-1 text-lg font-extrabold text-white">Share your details</h2>
          </div>
          <button type="button" aria-label="Close lead form" onClick={onClose} className="d7-icon-btn">
            <X size={18} />
          </button>
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          <label className="block text-sm text-[#b3b3b3]">Student name<input value={form.studentName} onChange={set("studentName")} className="d7-input mt-1.5" placeholder="Student" /></label>
          <label className="block text-sm text-[#b3b3b3]">Parent name<input value={form.parentName} onChange={set("parentName")} className="d7-input mt-1.5" placeholder="Parent" /></label>
          <label className="block text-sm text-[#b3b3b3]">Age<input value={form.age} onChange={set("age")} inputMode="numeric" className="d7-input mt-1.5" placeholder="7" /></label>
          <label className="block text-sm text-[#b3b3b3]">Phone *<input required value={form.phone} onChange={set("phone")} inputMode="tel" className="d7-input mt-1.5" placeholder="97310 67867" /></label>
          <label className="block text-sm text-[#b3b3b3] sm:col-span-2">Email<input type="email" value={form.email} onChange={set("email")} className="d7-input mt-1.5" placeholder="you@example.com" /></label>
          <label className="block text-sm text-[#b3b3b3]">Interested class<input value={form.interestedClass} onChange={set("interestedClass")} className="d7-input mt-1.5" placeholder="Kids Dance Batch" /></label>
          <label className="block text-sm text-[#b3b3b3]">Preferred batch<input value={form.preferredBatch} onChange={set("preferredBatch")} className="d7-input mt-1.5" placeholder="Weekend morning" /></label>
          <label className="block text-sm text-[#b3b3b3] sm:col-span-2">Preferred start date<input value={form.preferredStartDate} onChange={set("preferredStartDate")} className="d7-input mt-1.5" placeholder="Next Monday" /></label>
          <label className="block text-sm text-[#b3b3b3] sm:col-span-2">Message<textarea value={form.message} onChange={set("message")} rows={2} className="d7-input mt-1.5" placeholder="Anything we should know?" /></label>
        </div>
        {error && <p className="d7-error mt-3">{error}</p>}
        <button type="submit" disabled={saving} className="d7-btn-primary mt-4 w-full">
          {saving ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />} {saving ? "Sending…" : "Request callback"}
        </button>
      </form>
    </div>
  );
}
