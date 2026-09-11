"use client";

import Link from "next/link";
import { Bell, LogOut, Menu, Search, X } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { currentUser, logout, type AuthUser } from "@/lib/auth";
import { getBranches, type Branch } from "@/lib/branches";
import { visibleNavItems } from "@/components/sidebar";

const TITLES: Record<string, string> = {
  "/dashboard": "Studio performance",
  "/students": "Students",
  "/attendance": "Attendance",
  "/instructors": "Instructors",
  "/batches": "Batches",
  "/memberships": "Memberships",
  "/payments": "Payments",
  "/invoices": "Invoices",
  "/reminders": "Reminders",
  "/feedback": "Feedback",
  "/admin/users": "User management",
};

export function Navbar() {
  const router = useRouter();
  const pathname = usePathname();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [branches, setBranches] = useState<Branch[]>([]);
  const [workspace, setWorkspace] = useState("");

  useEffect(() => {
    currentUser().then(setUser).catch(() => undefined);
    getBranches().then(setBranches).catch(() => undefined);
  }, []);
  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  async function handleLogout() {
    await logout().catch(() => undefined);
    router.replace("/login");
  }

  const items = visibleNavItems(user);
  const title = Object.entries(TITLES).find(([key]) => pathname.startsWith(key))?.[1] ?? "Dance7";

  return (
    <>
      <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/85 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-7xl items-center gap-3 px-4 sm:px-6 lg:px-8">
          <button
            aria-label="Open navigation menu"
            onClick={() => setMenuOpen(true)}
            className="d7-icon-btn lg:hidden"
          >
            <Menu size={20} />
          </button>
          <div className="min-w-0">
            <p className="truncate text-[15px] font-extrabold tracking-tight text-slate-900">{title}</p>
            <p className="hidden text-xs text-slate-400 sm:block">Dance7 workspace · The Art Factory</p>
          </div>
          <div className="ml-auto flex items-center gap-1.5">
            <form
              className="mr-1 hidden items-center md:flex"
              onSubmit={(e) => {
                e.preventDefault();
                router.push(query ? `/students?search=${encodeURIComponent(query)}` : "/students");
              }}
            >
              <label className="relative block">
                <Search size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Search students…"
                  aria-label="Search students"
                  className="d7-input w-56 !bg-slate-50 pl-9"
                />
              </label>
            </form>
            <Link aria-label="Open reminders" href="/reminders" className="d7-icon-btn relative">
              <Bell size={19} />
              <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-violet-600 ring-2 ring-white" />
            </Link>
            <Link aria-label="Search students" href="/students" className="d7-icon-btn md:hidden">
              <Search size={19} />
            </Link>
            <button aria-label="Log out" onClick={handleLogout} className="d7-icon-btn">
              <LogOut size={19} />
            </button>
            <div className="d7-avatar ml-1 hidden sm:flex" title={user?.fullName || "Dance7"}>
              {(user?.fullName || "D7").slice(0, 2).toUpperCase()}
            </div>
          </div>
        </div>
      </header>

      {menuOpen && (
        <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation menu">
          <div className="absolute inset-0 bg-slate-900/50" onClick={() => setMenuOpen(false)} />
          <aside className="absolute inset-y-0 left-0 flex w-[300px] flex-col bg-white px-4 py-5 shadow-2xl">
            <div className="mb-5 flex items-center justify-between px-1">
              <Link href="/dashboard" onClick={() => setMenuOpen(false)} className="flex items-center gap-2.5">
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-violet-600 to-indigo-600 text-xs font-black text-white">
                  D7
                </span>
                <span className="text-base font-extrabold tracking-tight text-slate-900">Dance7</span>
              </Link>
              <button aria-label="Close navigation menu" onClick={() => setMenuOpen(false)} className="d7-icon-btn">
                <X size={19} />
              </button>
            </div>
            <div className="mb-4 rounded-2xl border border-slate-200 bg-slate-50 p-2 pl-3">
              <label className="mb-1 block px-1 text-[11px] font-bold uppercase tracking-[0.12em] text-slate-400">
                Workspace
              </label>
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
            <nav className="flex-1 space-y-1 overflow-y-auto">
              {items.map(({ label, href, icon: Icon }) => {
                const active = pathname === href || (href !== "/dashboard" && pathname.startsWith(href));
                return (
                  <Link
                    key={href}
                    href={href}
                    onClick={() => setMenuOpen(false)}
                    className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm ${
                      active ? "bg-violet-600 font-semibold text-white" : "font-medium text-slate-600 hover:bg-slate-100"
                    }`}
                  >
                    <Icon size={18} />
                    {label}
                  </Link>
                );
              })}
            </nav>
            <button onClick={handleLogout} className="d7-btn-secondary mt-4 w-full">
              <LogOut size={16} /> Log out
            </button>
          </aside>
        </div>
      )}

      {/* Mobile bottom navigation */}
      <nav aria-label="Primary" className="fixed inset-x-0 bottom-0 z-30 border-t border-slate-200 bg-white/95 backdrop-blur lg:hidden">
        <div className="mx-auto grid max-w-lg grid-cols-5 px-2 py-1.5">
          {[
            { label: "Home", href: "/dashboard" },
            { label: "Students", href: "/students" },
            { label: "Batches", href: "/batches" },
            { label: "Payments", href: "/payments" },
            { label: "More", href: "/reminders" },
          ].map((tab) => {
            const item = items.find((i) => i.href === tab.href) ?? visibleNavItems(user).find((i) => i.href === "/dashboard");
            const Icon = item?.icon ?? Menu;
            const active = pathname.startsWith(tab.href);
            return (
              <Link
                key={tab.href + tab.label}
                href={tab.href}
                className={`flex flex-col items-center gap-0.5 rounded-xl py-1.5 text-[11px] font-semibold ${
                  active ? "text-violet-700" : "text-slate-400"
                }`}
              >
                <Icon size={20} />
                {tab.label}
              </Link>
            );
          })}
        </div>
      </nav>
    </>
  );
}
