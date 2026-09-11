"use client";

import { Download } from "lucide-react";
import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { Card, LoadingState, PageHeader, StatCard } from "@/components/ui/card";
import { formatCurrency } from "@/lib/currency";
import { getInvoices, type Invoice } from "@/lib/invoices";
import { getPayments, type Payment } from "@/lib/payments";

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen">
      <Sidebar />
      <div className="lg:pl-[272px]">
        <Navbar />
        <main className="d7-page">{children}</main>
      </div>
    </div>
  );
}

function toCsv(rows: string[][]): string {
  return rows.map((r) => r.map((c) => `"${String(c ?? "").replace(/"/g, '""')}"`).join(",")).join("\n");
}

function download(name: string, csv: string) {
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export default function ReportsPage() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [error, setError] = useState("");
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    Promise.all([getPayments("", 0, 100), getInvoices()])
      .then(([p, inv]) => {
        setPayments(p.content);
        setInvoices(inv.content);
        setLoaded(true);
      })
      .catch((e) => setError(e instanceof Error ? e.message : "Unable to load reports."));
  }, []);

  const paid = payments.filter((p) => p.paymentStatus === "PAID");
  const revenue = paid.reduce((t, p) => t + Number(p.amount), 0);
  const byStatus = ["PAID", "PENDING", "PROCESSING", "FAILED", "REFUNDED", "CANCELLED"].map((s) => ({
    status: s,
    count: payments.filter((p) => p.paymentStatus === s).length,
    total: payments.filter((p) => p.paymentStatus === s).reduce((t, p) => t + Number(p.amount), 0),
  }));
  const months: Record<string, number> = {};
  for (const p of paid) {
    const key = p.paymentDate.slice(0, 7);
    months[key] = (months[key] ?? 0) + Number(p.amount);
  }
  const monthRows = Object.entries(months).sort().reverse().slice(0, 12);

  return (
    <Shell>
      <PageHeader
        eyebrow="Finance"
        title="Reports"
        description="Revenue, payment and invoice summaries with CSV export."
      />
      {error && <p className="d7-error mb-6">{error}</p>}
      {!loaded ? (
        <Card><LoadingState label="Loading reports…" rows={5} /></Card>
      ) : (
        <>
          <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Total revenue" value={formatCurrency(revenue)} tone="emerald" sub={`${paid.length} paid payments`} />
            <StatCard label="Invoices generated" value={invoices.length} tone="blue" sub="Across all branches" />
            <StatCard label="Failed payments" value={byStatus.find((s) => s.status === "FAILED")?.count ?? 0} tone="rose" sub="Needs follow-up" />
            <StatCard label="Pending collection" value={formatCurrency(byStatus.find((s) => s.status === "PENDING")?.total ?? 0)} tone="amber" sub="Outstanding" />
          </section>

          <section className="mt-4 grid gap-4 lg:grid-cols-2">
            <Card>
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-base font-extrabold text-white">Revenue by month</h2>
                <button
                  onClick={() => download("dance7-revenue.csv", toCsv([["Month", "Revenue"], ...monthRows.map(([m, t]) => [m, String(t)])]))}
                  className="d7-btn-secondary !px-3 !py-1.5 !text-xs"
                >
                  <Download size={13} /> CSV
                </button>
              </div>
              {monthRows.length ? (
                <ul className="space-y-2">
                  {monthRows.map(([m, t]) => (
                    <li key={m} className="flex items-center justify-between rounded-xl border border-[#2a2a2a] bg-[#161616] px-3.5 py-2.5 text-sm">
                      <span className="font-bold text-white">{m}</span>
                      <span className="text-[#4ade80]">{formatCurrency(t)}</span>
                    </li>
                  ))}
                </ul>
              ) : <p className="text-sm text-[#8a8a8a]">No collections yet.</p>}
            </Card>
            <Card>
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-base font-extrabold text-white">Payments by status</h2>
                <button
                  onClick={() => download("dance7-payments.csv", toCsv([["Payment", "Student", "Membership", "Amount", "Status", "Date", "RazorpayPayment"], ...payments.map((p) => [String(p.id), String(p.studentId), String(p.membershipId), String(p.amount), p.paymentStatus, p.paymentDate, (p as { razorpayPaymentId?: string }).razorpayPaymentId ?? ""])]))}
                  className="d7-btn-secondary !px-3 !py-1.5 !text-xs"
                >
                  <Download size={13} /> CSV
                </button>
              </div>
              <ul className="space-y-2">
                {byStatus.map((s) => (
                  <li key={s.status} className="flex items-center justify-between rounded-xl border border-[#2a2a2a] bg-[#161616] px-3.5 py-2.5 text-sm">
                    <span className="font-bold text-white">{s.status} · {s.count}</span>
                    <span className="text-[#b3b3b3]">{formatCurrency(s.total)}</span>
                  </li>
                ))}
              </ul>
              <button
                onClick={() => download("dance7-invoices.csv", toCsv([["Invoice", "Payment", "Student", "Amount", "Date", "Status"], ...invoices.map((i) => [i.invoiceNumber, String(i.paymentId), String(i.studentId), String(i.amount), i.invoiceDate, i.status])]))}
                className="d7-btn-secondary mt-4 !px-3 !py-1.5 !text-xs"
              >
                <Download size={13} /> Invoices CSV
              </button>
            </Card>
          </section>
        </>
      )}
    </Shell>
  );
}
