"use client";

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
    <main className="grid min-h-screen bg-white lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-violet-700 via-violet-600 to-indigo-600 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white text-sm font-black text-violet-700">D7</span>
          <span>
            <span className="block text-lg font-extrabold tracking-tight">Dance7</span>
            <span className="block text-xs font-medium text-white/70">The Art Factory</span>
          </span>
        </div>
        <div>
          <p className="d7-pill mb-5 bg-white/15 text-white ring-white/20">Set up in minutes</p>
          <h2 className="max-w-md text-4xl font-extrabold leading-[1.1] tracking-tight xl:text-5xl">
            Create your studio workspace today.
          </h2>
          <p className="mt-4 max-w-md text-[15px] leading-7 text-white/80">
            Invite instructors, add branches and batches, and track every fee with confidence.
          </p>
        </div>
        <p className="text-xs font-medium text-white/60">No credit card required · Cancel anytime</p>
      </div>
      <div className="flex items-center justify-center px-5 py-10 sm:px-10">
        <div className="w-full max-w-md">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-violet-600 to-indigo-600 text-sm font-black text-white">D7</span>
            <span className="text-lg font-extrabold tracking-tight text-slate-900">Dance7</span>
          </div>
          <p className="d7-eyebrow">Set up your workspace</p>
          <h1 className="d7-h1 mt-2">Create your account.</h1>
          <p className="d7-sub">One account gives you access to your studio dashboard.</p>
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
            <p className="text-center text-sm text-slate-500">
              Already registered? <Link href="/login" className="font-bold text-violet-700 hover:text-violet-800">Sign in</Link>
            </p>
          </form>
        </div>
      </div>
    </main>
  );
}
