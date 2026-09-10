"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { login } from "@/lib/auth";

export default function LoginPage() {
  return <Suspense fallback={<main className="flex min-h-screen items-center justify-center bg-[#18232b] px-6 py-12 text-white">Checking your Dance7 session...</main>}><LoginForm /></Suspense>;
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
    <main className="flex min-h-screen items-center justify-center bg-[#18232b] px-6 py-12 text-white">
      <div className="w-full max-w-md">
        <div className="mb-10 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#d5f45b] text-sm font-black text-[#18232b]">D7</div>
          <span className="text-xl font-bold tracking-tight">Dance7</span>
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/[0.06] p-8 shadow-2xl">
          <p className="mb-2 text-sm font-medium text-[#d5f45b]">Welcome back</p>
          <h1 className="mb-8 text-3xl font-semibold tracking-tight">Sign in to your studio.</h1>
          <form onSubmit={handleSubmit} className="space-y-5">
            <label className="block text-sm text-white/70">Email<input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@studio.com" className="mt-2 w-full rounded-lg border border-white/15 bg-white/10 px-4 py-3 text-white outline-none placeholder:text-white/30 focus:border-[#d5f45b]" /></label>
            <label className="block text-sm text-white/70">Password<input required minLength={8} type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Your password" className="mt-2 w-full rounded-lg border border-white/15 bg-white/10 px-4 py-3 text-white outline-none placeholder:text-white/30 focus:border-[#d5f45b]" /></label>
            {error && <p className="rounded-lg bg-[#b84639]/20 px-3 py-2 text-sm text-[#ffb8ad]">{error}</p>}
            <button disabled={submitting} type="submit" className="block w-full rounded-lg bg-[#d5f45b] px-4 py-3 text-center font-semibold text-[#18232b] transition hover:bg-[#e2ff7b] disabled:opacity-60">{submitting ? "Signing in..." : "Continue to Dance7"}</button>
            <p className="text-center text-sm text-white/60">New to Dance7? <Link href="/register" className="font-semibold text-[#d5f45b] hover:text-white">Create an account</Link></p>
          </form>
        </div>
      </div>
    </main>
  );
}
