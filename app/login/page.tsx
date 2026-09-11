"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { ArrowRight, Loader2 } from "lucide-react";
import { login } from "@/lib/auth";

export default function LoginPage() {
  return (
    <Suspense fallback={<AuthSplash message="Checking your Dance7 session…" />}>
      <LoginForm />
    </Suspense>
  );
}

function AuthSplash({ message }: { message: string }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#080808] px-6 py-12">
      <div className="flex flex-col items-center gap-4">
        <Image src="/brand/dance7-logo.jpg" alt="Dance7 — The Art Factory" width={72} height={72} className="rounded-full object-cover ring-1 ring-white/20" />
        <p className="text-sm font-medium text-[#b3b3b3]">{message}</p>
      </div>
    </main>
  );
}

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await login(email, password);
      router.replace(searchParams.get("next") || "/dashboard");
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : "Unable to sign in.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="grid min-h-screen bg-[#080808] text-white lg:grid-cols-2">
      {/* Brand panel */}
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
          <p className="d7-pill-violet mb-5 !bg-[#ff1a1a]/20 !text-white ring-white/20">Where artists are made</p>
          <h2 className="max-w-md text-4xl font-extrabold leading-[1.05] tracking-tight xl:text-5xl">
            The stage is set. <span className="text-[#ff4d4d]">Run the show.</span>
          </h2>
          <p className="mt-4 max-w-md text-[15px] leading-7 text-white/70">
            Students, batches, memberships, fees, attendance and reminders — one
            command center for your entire academy.
          </p>
          <div className="mt-8 grid max-w-md grid-cols-3 gap-3">
            {[
              ["2k+", "Students"],
              ["120+", "Batches"],
              ["99%", "Fee clarity"],
            ].map(([v, l]) => (
              <div key={l} className="rounded-2xl border border-white/20 bg-white/5 p-4 backdrop-blur">
                <p className="text-2xl font-extrabold">{v}</p>
                <p className="text-xs font-medium text-white/60">{l}</p>
              </div>
            ))}
          </div>
        </div>
        <p className="relative text-xs font-medium uppercase tracking-[0.24em] text-white/50">
          Dance7 · The Art Factory
        </p>
      </div>

      {/* Form panel */}
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
            <p className="d7-eyebrow">Welcome back</p>
            <h1 className="d7-h1 mt-2">Sign in to your studio.</h1>
            <p className="d7-sub">Use your workspace email to enter the command center.</p>
            <form onSubmit={handleSubmit} className="mt-8 space-y-5">
              <div>
                <label htmlFor="email" className="d7-label">Email</label>
                <input id="email" required type="email" autoComplete="email" value={email}
                  onChange={(e) => setEmail(e.target.value)} placeholder="you@studio.com" className="d7-input" />
              </div>
              <div>
                <label htmlFor="password" className="d7-label">Password</label>
                <input id="password" required minLength={8} type="password" autoComplete="current-password" value={password}
                  onChange={(e) => setPassword(e.target.value)} placeholder="Your password" className="d7-input" />
              </div>
              {error && <p role="alert" className="d7-error">{error}</p>}
              <button disabled={submitting} type="submit" className="d7-btn-primary w-full !py-3 text-[15px]">
                {submitting ? (<><Loader2 size={17} className="animate-spin" /> Signing in…</>) : (<>Enter Dance7 <ArrowRight size={17} /></>)}
              </button>
              <p className="text-center text-sm text-[#b3b3b3]">
                New to Dance7? <Link href="/register" className="font-bold text-[#ff4d4d] hover:text-white">Create an account</Link>
              </p>
            </form>
          </div>
        </div>
      </div>
    </main>
  );
}
