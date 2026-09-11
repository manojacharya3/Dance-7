"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { ArrowRight, Loader2, Sparkles } from "lucide-react";
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
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-6 py-12">
      <p className="text-sm font-medium text-slate-500">{message}</p>
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
    <main className="grid min-h-screen bg-white lg:grid-cols-2">
      {/* Brand panel — light SaaS, not dark */}
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-violet-700 via-violet-600 to-indigo-600 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white text-sm font-black text-violet-700">D7</span>
          <span>
            <span className="block text-lg font-extrabold tracking-tight">Dance7</span>
            <span className="block text-xs font-medium text-white/70">The Art Factory</span>
          </span>
        </div>
        <div>
          <p className="d7-pill mb-5 bg-white/15 text-white ring-white/20">Studio management, simplified</p>
          <h2 className="max-w-md text-4xl font-extrabold leading-[1.1] tracking-tight xl:text-5xl">
            Run your entire dance studio from one calm dashboard.
          </h2>
          <p className="mt-4 max-w-md text-[15px] leading-7 text-white/80">
            Students, batches, memberships, payments, attendance and reminders — designed for owners, branch heads and
            instructors.
          </p>
          <div className="mt-8 grid max-w-md grid-cols-3 gap-3">
            {[
              ["2k+", "Students"],
              ["120+", "Batches"],
              ["99%", "Fee clarity"],
            ].map(([v, l]) => (
              <div key={l} className="rounded-2xl bg-white/10 p-4 ring-1 ring-inset ring-white/15">
                <p className="text-2xl font-extrabold">{v}</p>
                <p className="text-xs font-medium text-white/70">{l}</p>
              </div>
            ))}
          </div>
        </div>
        <p className="flex items-center gap-2 text-xs font-medium text-white/60">
          <Sparkles size={14} /> Secure workspace · Role-based access · Mobile ready
        </p>
      </div>

      {/* Form panel */}
      <div className="flex items-center justify-center px-5 py-10 sm:px-10">
        <div className="w-full max-w-md">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-violet-600 to-indigo-600 text-sm font-black text-white">D7</span>
            <span className="text-lg font-extrabold tracking-tight text-slate-900">Dance7</span>
          </div>
          <p className="d7-eyebrow">Welcome back</p>
          <h1 className="d7-h1 mt-2">Sign in to your studio.</h1>
          <p className="d7-sub">Use your workspace email to continue to Dance7.</p>
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
              {submitting ? (<><Loader2 size={17} className="animate-spin" /> Signing in…</>) : (<>Continue to Dance7 <ArrowRight size={17} /></>)}
            </button>
            <p className="text-center text-sm text-slate-500">
              New to Dance7? <Link href="/register" className="font-bold text-violet-700 hover:text-violet-800">Create an account</Link>
            </p>
          </form>
        </div>
      </div>
    </main>
  );
}
