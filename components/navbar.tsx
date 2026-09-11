"use client";

import Link from "next/link";
import { LogOut, Menu, Search, X } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { currentUser, logout, type AuthUser } from "@/lib/auth";
import { visibleNavItems } from "@/components/sidebar";

export function Navbar() {
  const router = useRouter();
  const pathname = usePathname();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  useEffect(() => { currentUser().then(setUser).catch(() => undefined); }, []);
  useEffect(() => { setMenuOpen(false); }, [pathname]);
  async function handleLogout() { await logout().catch(() => undefined); router.replace("/login"); }
  const items = visibleNavItems(user);
  return <><header className="flex h-20 items-center justify-between border-b border-[#e8e5df] bg-[#faf9f6] px-6 lg:px-10"><div className="flex items-center gap-3 text-sm text-[#667078]"><button aria-label="Open navigation menu" onClick={() => setMenuOpen(true)} className="rounded-lg p-2.5 text-[#667078] hover:bg-[#efeee9] lg:hidden"><Menu size={18} /></button><span className="hidden sm:inline">Dance7 workspace</span><span className="h-1 w-1 rounded-full bg-[#d5f45b]" /><span>Student management</span></div><div className="flex items-center gap-2"><Link aria-label="Search students" href="/students" className="rounded-lg p-2.5 text-[#667078] hover:bg-[#efeee9]"><Search size={18} /></Link><button aria-label="Log out" onClick={handleLogout} className="rounded-lg p-2.5 text-[#667078] hover:bg-[#efeee9]"><LogOut size={18} /></button><div className="ml-2 flex h-9 w-9 items-center justify-center rounded-full bg-[#f0c9a5] text-sm font-bold text-[#6c3e29]">D7</div></div></header>{menuOpen && <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation menu"><div className="absolute inset-0 bg-[#18232b]/50" onClick={() => setMenuOpen(false)} /><aside className="absolute inset-y-0 left-0 flex w-72 flex-col bg-[#18232b] px-5 py-6 text-white"><div className="mb-8 flex items-center justify-between px-2"><Link href="/dashboard" onClick={() => setMenuOpen(false)} className="flex items-center gap-3"><span className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#d5f45b] text-xs font-black text-[#18232b]">D7</span><span className="text-lg font-bold tracking-tight">Dance7</span></Link><button aria-label="Close navigation menu" onClick={() => setMenuOpen(false)} className="rounded-lg p-2 text-white/60 hover:bg-white/5 hover:text-white"><X size={18} /></button></div><p className="mb-3 px-2 text-[10px] font-bold uppercase tracking-[0.18em] text-white/40">Workspace</p><nav className="space-y-1 overflow-y-auto">{items.map(({ label, href, icon: Icon }) => { const active = pathname === href; return <Link key={href} href={href} onClick={() => setMenuOpen(false)} className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition ${active ? "bg-white/10 font-semibold text-[#d5f45b]" : "text-white/60 hover:bg-white/5 hover:text-white"}`}><Icon size={17} strokeWidth={active ? 2.5 : 1.8} />{label}</Link>; })}</nav><div className="mt-auto border-t border-white/10 pt-4"><span className="flex items-center gap-3 px-3 py-2.5 text-sm text-white/40">Dance7 workspace</span></div></aside></div>}</>;
}
