import Image from "next/image";
import Link from "next/link";
import { cn } from "@/lib/utils";

export function BrandLockup({
  href = "/dashboard",
  size = "md",
  onNavigate,
  dark = true,
}: {
  href?: string;
  size?: "sm" | "md" | "lg";
  onNavigate?: () => void;
  dark?: boolean;
}) {
  const dims = size === "lg" ? 52 : size === "sm" ? 36 : 44;
  return (
    <Link href={href} onClick={onNavigate} className="flex items-center gap-3">
      <Image
        src="/brand/dance7-logo.jpg"
        alt="Dance7 — The Art Factory logo"
        width={dims}
        height={dims}
        priority
        className="rounded-full object-cover ring-1 ring-white/20"
        style={{ width: dims, height: dims }}
      />
      <span>
        <span className={cn("block font-extrabold uppercase tracking-[0.08em]", dark ? "text-white" : "text-slate-900", size === "lg" ? "text-xl" : "text-[17px]")}>
          Dance7
        </span>
        <span className={cn("block text-[10px] font-semibold uppercase tracking-[0.32em]", dark ? "text-[#b3b3b3]" : "text-slate-500")}>
          The Art Factory
        </span>
      </span>
    </Link>
  );
}
