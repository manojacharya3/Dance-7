"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ArrowRight, Loader2 } from "lucide-react";
import { register } from "@/lib/auth";

export default function RegisterPage() {
  const router = useRouter();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await register(fullName, email, password);
      router.replace("/dashboard");
    } catch (registrationError) {
      setError(registrationError instanceof Error ? registrationError.message : "Unable to create account.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="grid min-h-screen bg-[#080808] text-white lg:grid-cols-2">
      <div className="relative hidden overflow-hidden lg:flex lg:flex-col lg:justify-between lg:p-12">
        <Image
          src="/brand/dance7-logo.jpg"
          alt=""
          aria-hidden
          fill
          priority
          className="object-cover opacity-25"
        />
        <div className="absolute inset-0 bg-gradient-to-br from-black via-black/70 to-[#ff1a1a]/30" />
        <div className="relative flex items-center gap-4">
          <Image src="/brand/dance7-logo.jpg" alt="Dance7 — The Art Factory logo" width={64} height={64} priority className="rounded-full object-cover ring-2 ring-white/30 shadow-[0_0_32px_rgba(255,26,26,0.5)]" />
          <span>
            <span className="block text-2xl font-extrabold uppercase tracking-[0.08em]">Dance7</span>
            <span className="block text-[11px] font-semibold uppercase tracking-[0.4em] text-white/70">The Art Factory</span>
          </span>
        </div>
        <div className="relative">
          <p className="d7-pill-violet mb-5 !bg-[#ff1a1a]/20 !text-white ring-white/20">Join the factory</p>
          <h2 className="max-w-md text-4xl font-extrabold leading-[1.05] tracking-tight xl:text-5xl">
            Your academy, <span className="text-[#ff4d4d]">center stage.</span>
          </h2>
          <p className="mt-4 max-w-md text-[15px] leading-7 text-white/70">
            Invite instructors, add branches and batches, and track every fee
            with absolute confidence.
          </p>
        </div>
        <p className="relative text-xs font-medium uppercase tracking-[0.24em] text-white/50">
          No credit card required · Cancel anytime
        </p>
      </div>
      <div className="flex items-center justify-center border-t border-[#1f1f1f] px-5 py-10 sm:px-10 lg:border-l lg:border-t-0">
        <div className="w-full max-w-md">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <Image src="/brand/dance7-logo.jpg" alt="Dance7 — The Art Factory logo" width={48} height={48} className="rounded-full object-cover ring-1 ring-white/20" />
            <span>
              <span className="block text-lg font-extrabold uppercase tracking-[0.08em] text-white">Dance7</span>
              <span className="block text-[10px] font-semibold uppercase tracking-[0.32em] text-[#8a8a8a]">The Art Factory</span>
            </span>
          </div>
          <div className="rounded-[20px] border border-[#2a2a2a] bg-white/[0.03] p-6 backdrop-blur sm:p-8">
            <p className="d7-eyebrow">Set up your workspace</p>
            <h1 className="d7-h1 mt-2">Create your account.</h1>
            <p className="d7-sub">One account gives you access to your studio command center.</p>
            <form onSubmit={handleSubmit} className="mt-8 space-y-5">
              <div>
                <label htmlFor="fullName" className="d7-label">Full name</label>
                <input id="fullName" required autoComplete="name" value={fullName}
                  onChange={(e) => setFullName(e.target.value)} placeholder="Asha Sharma" className="d7-input" />
              </div>
              <div>
                <label htmlFor="email" className="d7-label">Email</label>
                <input id="email" required type="email" autoComplete="email" value={email}
                  onChange={(e) => setEmail(e.target.value)} placeholder="you@studio.com" className="d7-input" />
              </div>
              <div>
                <label htmlFor="password" className="d7-label">Password</label>
                <input id="password" required minLength={8} type="password" autoComplete="new-password" value={password}
                  onChange={(e) => setPassword(e.target.value)} placeholder="Minimum 8 characters" className="d7-input" />
                <p className="d7-hint">Use at least 8 characters with a mix of letters and numbers.</p>
              </div>
              {error && <p role="alert" className="d7-error">{error}</p>}
              <button disabled={submitting} type="submit" className="d7-btn-primary w-full !py-3 text-[15px]">
                {submitting ? (<><Loader2 size={17} className="animate-spin" /> Creating…</>) : (<>Create Dance7 account <ArrowRight size={17} /></>)}
              </button>
              <p className="text-center text-sm text-[#b3b3b3]">
                Already registered? <Link href="/login" className="font-bold text-[#ff4d4d] hover:text-white">Sign in</Link>
              </p>
            </form>
          </div>
        </div>
      </div>
    </main>
  );
}
