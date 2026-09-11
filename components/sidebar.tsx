"use client";

import Link from "next/link";
import {
  BadgeIndianRupee,
  Bell,
  CalendarCheck,
  ChartColumn,
  CreditCard,
  FileText,
  Home,
  LayoutGrid,
  MessageSquarePlus,
  Sparkles,
  UserRound,
  UsersRound,
} from "lucide-react";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { currentUser, type AuthUser } from "@/lib/auth";
import { getBranches, type Branch } from "@/lib/branches";
import { BrandLockup } from "@/components/brand";
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
  { label: "Reports", href: "/reports", icon: ChartColumn, section: "Finance" },
  { label: "Feedback", href: "/feedback", icon: MessageSquarePlus, section: "System" },
  { label: "AI Assistant", href: "/admin/ai", icon: Sparkles, section: "System" },
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
    if (item.href === "/admin/users" || item.href === "/admin/ai") return Boolean(isOwner || isDeveloper);
    if (isDeveloper)
      return ["/dashboard", "/students", "/instructors", "/batches", "/memberships", "/payments", "/invoices", "/reminders", "/reports", "/feedback", "/admin/users", "/admin/ai"].includes(item.href);
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
    <aside className="fixed inset-y-0 left-0 hidden w-[272px] flex-col border-r border-[#1f1f1f] bg-[#0b0b0b]/95 backdrop-blur lg:flex">
      <div className="px-5 pb-4 pt-6">
        <BrandLockup />
      </div>

      {/* Workspace switcher — premium branded header area */}
      <div className="px-4 pb-4">
        <div className="rounded-2xl border border-[#2a2a2a] bg-gradient-to-br from-[#1a1a1a] to-[#101010] p-2 pl-3">
          <label className="mb-1 block px-1 text-[10px] font-bold uppercase tracking-[0.24em] text-[#8a8a8a]">
            Workspace
          </label>
          <div className="flex items-center gap-2">
            <span className="d7-avatar !h-8 !w-8 text-[11px]">{(user?.fullName || "D7").slice(0, 2).toUpperCase()}</span>
            <select
              aria-label="Switch workspace branch"
              value={workspace}
              onChange={(e) => setWorkspace(e.target.value)}
              className="w-full bg-transparent text-sm font-semibold text-white outline-none"
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
      </div>

      <nav className="flex-1 space-y-5 overflow-y-auto px-4 pb-4">
        {sections.map(({ section, items }) => {
          if (!items.length) return null;
          return (
            <div key={section}>
              <p className="mb-1.5 px-2 text-[10px] font-bold uppercase tracking-[0.24em] text-[#6b6b6b]">{section}</p>
              <div className="space-y-1">
                {items.map(({ label, href, icon: Icon }) => {
                  const active = pathname === href || (href !== "/dashboard" && pathname.startsWith(href));
                  return (
                    <Link
                      key={href}
                      href={href}
                      className={cn(
                        "group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition",
                        active
                          ? "bg-[#ff1a1a] font-semibold text-white shadow-[0_0_24px_rgba(255,26,26,0.35)]"
                          : "font-medium text-[#b3b3b3] hover:bg-white/5 hover:text-white hover:backdrop-blur"
                      )}
                    >
                      <span
                        className={cn(
                          "absolute left-0 top-1/2 h-5 w-1 -translate-y-1/2 rounded-full bg-[#ff1a1a] transition",
                          active ? "opacity-100" : "opacity-0 group-hover:opacity-60"
                        )}
                      />
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

      <div className="border-t border-[#1f1f1f] p-4">
        <div className="flex items-center gap-3 rounded-2xl border border-[#2a2a2a] bg-[#141414] p-3">
          <span className="d7-avatar">{(user?.fullName || "DU").slice(0, 2).toUpperCase()}</span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-bold text-white">{user?.fullName || "Dance7 user"}</p>
            <p className="truncate text-xs text-[#8a8a8a]">{user?.email || "Studio workspace"}</p>
          </div>
        </div>
      </div>
    </aside>
  );
}
