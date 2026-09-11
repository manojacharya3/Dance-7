"use client";

import Link from "next/link";
import { BadgeIndianRupee, Bell, CalendarCheck, CreditCard, FileText, Home, MessageSquarePlus, UserRound, UsersRound } from "lucide-react";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { currentUser, type AuthUser } from "@/lib/auth";

export type NavItem = { label: string; href: string; icon: typeof Home };

export const navItems: NavItem[] = [
  { label: "Overview", href: "/dashboard", icon: Home },
  { label: "Students", href: "/students", icon: UserRound },
  { label: "Attendance", href: "/attendance", icon: CalendarCheck },
  { label: "Instructors", href: "/instructors", icon: UsersRound },
  { label: "Batches", href: "/batches", icon: CalendarCheck },
  { label: "Memberships", href: "/memberships", icon: CreditCard },
  { label: "Payments", href: "/payments", icon: BadgeIndianRupee },
  { label: "Invoices", href: "/invoices", icon: FileText },
  { label: "Reminders", href: "/reminders", icon: Bell },
  { label: "Feedback", href: "/feedback", icon: MessageSquarePlus },
  { label: "Administration / Users", href: "/admin/users", icon: UsersRound },
];

export function normalizeRoles(roles: string[] | undefined): string[] {
  return (roles ?? []).map((role) => role.toUpperCase().replace(/^ROLE_/, ""));
}

export function visibleNavItems(user: AuthUser | null): NavItem[] {
  const roles = normalizeRoles(user?.roles);
  const isOwner = user?.email.toLowerCase() === "owner@dance7.com" || roles.includes("ADMIN") || roles.includes("OWNER");
  const isInstructor = roles.includes("INSTRUCTOR");
  const isDeveloper = roles.includes("DEVELOPER");
  return navItems.filter((item) => { if (item.href === "/admin/users") return Boolean(isOwner || isDeveloper); if (isDeveloper) return ["/dashboard", "/students", "/instructors", "/batches", "/memberships", "/payments", "/invoices", "/reminders", "/feedback", "/admin/users"].includes(item.href); if (isInstructor) return ["/dashboard", "/students", "/attendance", "/batches", "/feedback"].includes(item.href); return true; });
}

export function Sidebar() {
  const pathname = usePathname();
  const [user, setUser] = useState<AuthUser | null>(null);
  useEffect(() => { currentUser().then(setUser).catch(() => undefined); }, []);
  const visibleItems = visibleNavItems(user);
  return (
    <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col bg-[#18232b] px-5 py-6 text-white lg:flex">
      <Link href="/dashboard" className="mb-12 flex items-center gap-3 px-2">
        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#d5f45b] text-xs font-black text-[#18232b]">D7</span>
        <span className="text-lg font-bold tracking-tight">Dance7</span>
      </Link>
      <p className="mb-3 px-2 text-[10px] font-bold uppercase tracking-[0.18em] text-white/40">Workspace</p>
      <nav className="space-y-1">
        {visibleItems.map(({ label, href, icon: Icon }) => {
          const active = pathname === href;
          return <Link key={href} href={href} className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition ${active ? "bg-white/10 font-semibold text-[#d5f45b]" : "text-white/60 hover:bg-white/5 hover:text-white"}`}><Icon size={17} strokeWidth={active ? 2.5 : 1.8} />{label}</Link>;
        })}
      </nav>
      <div className="mt-auto border-t border-white/10 pt-4"><span className="flex items-center gap-3 px-3 py-2.5 text-sm text-white/40">Dance7 workspace</span></div>
    </aside>
  );
}
