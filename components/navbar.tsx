"use client";

import Link from "next/link";
import { LogOut, Search } from "lucide-react";
import { useRouter } from "next/navigation";
import { logout } from "@/lib/auth";

export function Navbar() {
  const router = useRouter();
  async function handleLogout() { await logout().catch(() => undefined); router.replace("/login"); }
  return <header className="flex h-20 items-center justify-between border-b border-[#e8e5df] bg-[#faf9f6] px-6 lg:px-10"><div className="flex items-center gap-3 text-sm text-[#667078]"><span className="hidden sm:inline">Dance7 workspace</span><span className="h-1 w-1 rounded-full bg-[#d5f45b]" /><span>Student management</span></div><div className="flex items-center gap-2"><Link aria-label="Search students" href="/students" className="rounded-lg p-2.5 text-[#667078] hover:bg-[#efeee9]"><Search size={18} /></Link><button aria-label="Log out" onClick={handleLogout} className="rounded-lg p-2.5 text-[#667078] hover:bg-[#efeee9]"><LogOut size={18} /></button><div className="ml-2 flex h-9 w-9 items-center justify-center rounded-full bg-[#f0c9a5] text-sm font-bold text-[#6c3e29]">D7</div></div></header>;
}
