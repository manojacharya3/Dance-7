import * as React from "react";
import { cn } from "@/lib/utils";

export function Card({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("d7-card", className)} {...props} />;
}

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow: string;
  title: string;
  description?: string;
  actions?: React.ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:mb-8 lg:flex-row lg:items-end lg:justify-between">
      <div className="min-w-0">
        <p className="d7-eyebrow">{eyebrow}</p>
        <h1 className="d7-h1 mt-2">{title}</h1>
        {description ? <p className="d7-sub">{description}</p> : null}
      </div>
      {actions ? <div className="flex shrink-0 flex-col gap-2 sm:flex-row sm:items-center">{actions}</div> : null}
    </div>
  );
}

export function StatCard({
  label,
  value,
  sub,
  icon,
  tone = "brand",
}: {
  label: string;
  value: React.ReactNode;
  sub?: string;
  icon?: React.ReactNode;
  tone?: "brand" | "emerald" | "amber" | "blue" | "rose" | "slate";
}) {
  const tones: Record<string, string> = {
    brand: "bg-[#ff1a1a]/10 text-[#ff6b6b]",
    emerald: "bg-[#22c55e]/10 text-[#4ade80]",
    amber: "bg-[#f59e0b]/10 text-[#fbbf24]",
    blue: "bg-[#3b82f6]/10 text-[#93c5fd]",
    rose: "bg-[#ef4444]/10 text-[#ff8080]",
    slate: "bg-white/10 text-[#b3b3b3]",
  };
  return (
    <div className="d7-card d7-card-hover">
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-[#b3b3b3]">{label}</p>
        {icon ? (
          <span className={cn("flex h-9 w-9 items-center justify-center rounded-xl", tones[tone])}>{icon}</span>
        ) : null}
      </div>
      <p className="mt-2 text-3xl font-extrabold tracking-tight text-white">{value}</p>
      {sub ? <p className="mt-1 text-[13px] leading-5 text-[#8a8a8a]">{sub}</p> : null}
    </div>
  );
}

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center px-6 py-14 text-center">
      {icon ? (
        <span className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-[#ff1a1a]/10 text-[#ff6b6b]">
          {icon}
        </span>
      ) : null}
      <p className="text-base font-bold text-white">{title}</p>
      {description ? <p className="mt-1.5 max-w-sm text-sm leading-6 text-[#b3b3b3]">{description}</p> : null}
      {action ? <div className="mt-5">{action}</div> : null}
    </div>
  );
}

export function LoadingState({ rows = 4, label = "Loading…" }: { rows?: number; label?: string }) {
  return (
    <div className="space-y-3 p-4 sm:p-5" aria-live="polite" aria-busy="true">
      <p className="text-sm font-medium text-[#8a8a8a]">{label}</p>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="d7-skeleton h-12" style={{ opacity: 1 - i * 0.12 }} />
      ))}
    </div>
  );
}

export function StatusPill({ tone, children }: { tone: "green" | "red" | "amber" | "slate" | "violet" | "blue"; children: React.ReactNode }) {
  const map = {
    green: "d7-pill-green",
    red: "d7-pill-red",
    amber: "d7-pill-amber",
    slate: "d7-pill-slate",
    violet: "d7-pill-violet",
    blue: "d7-pill-blue",
  } as const;
  return <span className={map[tone]}>{children}</span>;
}
