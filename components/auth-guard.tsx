"use client";

import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { currentUser } from "@/lib/auth";

const PUBLIC_PATHS = ["/", "/login", "/register"];

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const protectedPath = pathname.startsWith("/dashboard") || pathname.startsWith("/students") || pathname.startsWith("/attendance") || pathname.startsWith("/instructors") || pathname.startsWith("/batches") || pathname.startsWith("/memberships") || pathname.startsWith("/payments") || pathname.startsWith("/invoices") || pathname.startsWith("/reminders") || pathname.startsWith("/admin");
  const [checking, setChecking] = useState(protectedPath);

  useEffect(() => {
    if (!protectedPath || PUBLIC_PATHS.includes(pathname)) {
      setChecking(false);
      return;
    }
    let active = true;
    currentUser().then(() => { if (active) setChecking(false); }).catch(() => { if (active) router.replace(`/login?next=${encodeURIComponent(pathname)}`); });
    return () => { active = false; };
  }, [pathname, protectedPath, router]);

  if (checking) return <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-[#080808] px-6"><Image src="/brand/dance7-logo.jpg" alt="Dance7 — The Art Factory" width={64} height={64} className="rounded-full object-cover ring-1 ring-white/20" /><p className="text-sm font-medium text-[#b3b3b3]">Checking your Dance7 session...</p></div>;
  return children;
}
