"use client";

import Link from "next/link";
import {
  BadgeIndianRupee,
  Bell,
  CalendarCheck,
  CreditCard,
  FileText,
  Home,
  LayoutGrid,
  MessageSquarePlus,
  UserRound,
  UsersRound,
} from "lucide-react";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { currentUser, type AuthUser } from "@/lib/auth";
import { getBranches, type Branch } from "@/lib/branches";
import { cn } from "@/lib/utils";

export type NavItem = { label: string; href: string; icon: typeof Home; section: string };

export const navItems: NavItem[] = [
  { label: "Overview", href: "/dashboard", icon: Home, section: "Studio" },
  { label: "Students", href: "/students", icon: UserRound, section: "Studio" },
  { label: "Attendance", href: "/attendance", icon: CalendarCheck, section: "Studio" },
  { label: "Instructors", href: "/instructors", icon: UsersRound, section: "Studio" },
  { label: "Batches", href: "/batches", icon: LayoutGrid, section: "Studio" },
  { label: "Memberships", href: "/memberships", icon: CreditCard, section: "Finance" },
  { label: "Payments", href: "/payments", icon: BadgeIndianRupee, section: "Finance" },
  { label: "Invoices", href: "/invoices", icon: FileText, section: "Finance" },
  { label: "Reminders", href: "/reminders", icon: Bell, section: "Finance" },
  { label: "Feedback", href: "/feedback", icon: MessageSquarePlus, section: "System" },
  { label: "Users", href: "/admin/users", icon: UsersRound, section: "System" },
];

export function normalizeRoles(roles: string[] | undefined): string[] {
  return (roles ?? []).map((role) => role.toUpperCase().replace(/^ROLE_/, ""));
}

export function visibleNavItems(user: AuthUser | null): NavItem[] {
  const roles = normalizeRoles(user?.roles);
  const isOwner = user?.email.toLowerCase() === "owner@dance7.com" || roles.includes("ADMIN") || roles.includes("OWNER");
  const isInstructor = roles.includes("INSTRUCTOR");
  const isDeveloper = roles.includes("DEVELOPER");
  return navItems.filter((item) => {
    if (item.href === "/admin/users") return Boolean(isOwner || isDeveloper);
    if (isDeveloper)
      return ["/dashboard", "/students", "/instructors", "/batches", "/memberships", "/payments", "/invoices", "/reminders", "/feedback", "/admin/users"].includes(item.href);
    if (isInstructor) return ["/dashboard", "/students", "/attendance", "/batches", "/feedback"].includes(item.href);
    return true;
  });
}

export function Sidebar() {
  const pathname = usePathname();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [workspace, setWorkspace] = useState("");

  useEffect(() => {
    currentUser().then(setUser).catch(() => undefined);
    getBranches().then(setBranches).catch(() => undefined);
  }, []);

  const visibleItems = visibleNavItems(user);
  const sections = ["Studio", "Finance", "System"].map((section) => ({
    section,
    items: visibleItems.filter((i) => i.section === section),
  }));

  return (
    <aside className="fixed inset-y-0 left-0 hidden w-[272px] flex-col border-r border-slate-200/80 bg-white lg:flex">
      <Link href="/dashboard" className="flex items-center gap-3 px-6 pb-5 pt-6">
        <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-violet-600 to-indigo-600 text-sm font-black text-white shadow-sm">
          D7
        </span>
        <span>
          <span className="block text-[17px] font-extrabold tracking-tight text-slate-900">Dance7</span>
          <span className="block text-xs font-medium text-slate-400">The Art Factory</span>
        </span>
      </Link>

      {/* Workspace switcher */}
      <div className="px-4 pb-4">
        <label className="mb-1.5 block px-2 text-[11px] font-bold uppercase tracking-[0.12em] text-slate-400">
          Workspace
        </label>
        <div className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 p-2 pl-3">
          <span className="d7-avatar !h-8 !w-8 text-[11px]">{(user?.fullName || "D7").slice(0, 2).toUpperCase()}</span>
          <select
            aria-label="Switch workspace branch"
            value={workspace}
            onChange={(e) => setWorkspace(e.target.value)}
            className="w-full bg-transparent text-sm font-semibold text-slate-800 outline-none"
          >
            <option value="">All branches</option>
            {branches.map((b) => (
              <option key={b.id} value={String(b.id)}>
                {b.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <nav className="flex-1 space-y-5 overflow-y-auto px-4 pb-4">
        {sections.map(({ section, items }) => {
          if (!items.length) return null;
          return (
            <div key={section}>
              <p className="mb-1.5 px-2 text-[11px] font-bold uppercase tracking-[0.12em] text-slate-400">{section}</p>
              <div className="space-y-1">
                {items.map(({ label, href, icon: Icon }) => {
                  const active = pathname === href || (href !== "/dashboard" && pathname.startsWith(href));
                  return (
                    <Link
                      key={href}
                      href={href}
                      className={cn(
                        "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition",
                        active
                          ? "bg-violet-600 font-semibold text-white shadow-sm"
                          : "font-medium text-slate-500 hover:bg-slate-100 hover:text-slate-900"
                      )}
                    >
                      <Icon size={18} strokeWidth={active ? 2.4 : 1.8} />
                      {label}
                    </Link>
                  );
                })}
              </div>
            </div>
          );
        })}
      </nav>

      <div className="border-t border-slate-100 p-4">
        <div className="flex items-center gap-3 rounded-2xl bg-slate-50 p-3">
          <span className="d7-avatar">{(user?.fullName || "DU").slice(0, 2).toUpperCase()}</span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-bold text-slate-900">{user?.fullName || "Dance7 user"}</p>
            <p className="truncate text-xs text-slate-400">{user?.email || "Studio workspace"}</p>
          </div>
        </div>
      </div>
    </aside>
  );
}
