"use client";
import Link from "next/link";
import { ArrowLeft, BadgeCheck, Download, Loader2, Pencil, Wallet } from "lucide-react";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { formatCurrency } from "@/lib/currency";
import {
  createRazorpayOrder,
  downloadInvoicePdf,
  getPayment,
  invoiceForPayment,
  razorpayStatus,
  resendInvoice,
  verifyRazorpayPayment,
  type Payment,
} from "@/lib/payments";

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void };
  }
}

function loadCheckout(): Promise<void> {
  if (window.Razorpay) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Could not load Razorpay Checkout. Check your connection and try again."));
    document.body.appendChild(script);
  });
}

export default function PaymentDetailsPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [payment, setPayment] = useState<Payment | null>(null);
  const [onlineEnabled, setOnlineEnabled] = useState(false);
  const [invoiceId, setInvoiceId] = useState<number | null>(null);
  const [invoiceNumber, setInvoiceNumber] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [paying, setPaying] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const reload = useCallback(() => {
    getPayment(params.id).then(setPayment).catch((e) => setError(e instanceof Error ? e.message : "Unable to load payment."));
    razorpayStatus().then((s) => setOnlineEnabled(s.enabled)).catch(() => setOnlineEnabled(false));
    invoiceForPayment(Number(params.id))
      .then((inv) => {
        setInvoiceId(inv.id);
        setInvoiceNumber(inv.invoiceNumber);
      })
      .catch(() => {
        setInvoiceId(null);
        setInvoiceNumber("");
      });
  }, [params.id]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function payNow() {
    if (!payment) return;
    setError("");
    setNotice("");
    setPaying(true);
    try {
      await loadCheckout();
      const order = await createRazorpayOrder(payment.id);
      const Razorpay = window.Razorpay;
      if (!Razorpay) throw new Error("Razorpay Checkout failed to load.");
      const checkout = new Razorpay({
        key: order.keyId,
        order_id: order.orderId,
        amount: order.amountPaise,
        currency: order.currency,
        name: "Dance7 — The Art Factory",
        description: `Payment #${payment.id} · ${formatCurrency(Number(payment.amount))}`,
        theme: { color: "#FF1A1A" },
        handler: async (response: { razorpay_payment_id: string; razorpay_order_id: string; razorpay_signature: string }) => {
          try {
            const inv = await verifyRazorpayPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            setNotice(`Payment verified. Invoice ${inv.invoiceNumber} generated and emailed.`);
            reload();
          } catch (e) {
            setError(e instanceof Error ? e.message : "Verification failed. Contact the studio with your payment reference.");
          } finally {
            setPaying(false);
          }
        },
        modal: { ondismiss: () => setPaying(false) },
      });
      checkout.open();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to start online payment.");
      setPaying(false);
    }
  }

  async function download() {
    if (invoiceId == null) return;
    setDownloading(true);
    try {
      const blob = await downloadInvoicePdf(invoiceId);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${invoiceNumber || `invoice-${invoiceId}`}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to download invoice.");
    } finally {
      setDownloading(false);
    }
  }

  async function resend() {
    if (invoiceId == null) return;
    try {
      await resendInvoice(invoiceId);
      setNotice("Receipt re-sent to the student email on file.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to resend invoice.");
    }
  }

  const payable = payment && payment.paymentStatus !== "PAID" && payment.paymentStatus !== "CANCELLED" && payment.paymentStatus !== "REFUNDED";

  return (
    <Shell>
      <Link href="/payments" className="mb-6 inline-flex items-center gap-2 text-sm text-[#b3b3b3]">
        <ArrowLeft size={16} />Back to payments
      </Link>
      {error ? <p className="rounded-lg bg-[#ef4444]/10 p-4 text-sm text-[#ff9999]">{error}</p> : null}
      {notice ? <p className="mb-4 rounded-lg bg-[#22c55e]/10 p-4 text-sm text-[#4ade80]">{notice}</p> : null}
      {!payment ? (
        <p className="text-sm text-[#b3b3b3]">Loading payment...</p>
      ) : (
        <>
          <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="mb-2 text-sm font-semibold text-[#ff6b6b]">Payment details</p>
              <h1 className="text-3xl font-semibold text-white">Payment #{payment.id}</h1>
              <p className="mt-2 text-sm text-[#b3b3b3]">
                Student #{payment.studentId} · Membership #{payment.membershipId}
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              {payable && onlineEnabled && (
                <button onClick={payNow} disabled={paying} className="d7-btn-primary">
                  {paying ? <Loader2 size={16} className="animate-spin" /> : <Wallet size={16} />}
                  {paying ? "Processing…" : "Pay Now"}
                </button>
              )}
              {invoiceId != null && (
                <button onClick={download} disabled={downloading} className="d7-btn-secondary">
                  {downloading ? <Loader2 size={16} className="animate-spin" /> : <Download size={16} />}
                  Download Invoice
                </button>
              )}
              <button onClick={() => router.push(`/payments/${payment.id}/edit`)} className="d7-btn-secondary">
                <Pencil size={16} />Edit payment
              </button>
            </div>
          </div>
          {payable && !onlineEnabled && (
            <p className="mb-4 rounded-xl border border-[#2a2a2a] bg-[#161616] p-4 text-sm text-[#b3b3b3]">
              Online payment is not configured for this studio yet. Record cash/UPI manually or ask an
              administrator to enable Razorpay.
            </p>
          )}
          <section className="grid gap-5 rounded-xl border border-[#2a2a2a] bg-[#111111] p-6 sm:grid-cols-2">
            <Detail label="Amount" value={formatCurrency(Number(payment.amount))} />
            <Detail label="Date" value={payment.paymentDate} />
            <Detail label="Method" value={payment.paymentMethod} />
            <Detail label="Status" value={payment.paymentStatus} />
            <Detail label="Razorpay order" value={payment.razorpayOrderId || "—"} />
            <Detail label="Razorpay payment" value={payment.razorpayPaymentId || "—"} />
            <Detail label="Receipt" value={payment.receiptNumber || "—"} />
            <Detail label="Paid at" value={payment.paidAt ? new Date(payment.paidAt).toLocaleString("en-IN") : "—"} />
            <Detail label="Invoice" value={invoiceId != null ? `#${invoiceId} · ${invoiceNumber}` : "Not generated yet"} />
            <Detail label="Remarks" value={payment.remarks || "None"} />
          </section>
          {invoiceId != null && (
            <div className="mt-4 flex flex-wrap items-center gap-3 rounded-xl border border-[#22c55e]/30 bg-[#22c55e]/5 p-4 text-sm">
              <BadgeCheck size={17} className="text-[#4ade80]" />
              <span className="text-[#e5e5e5]">Receipt ready — download the PDF or resend it to the student email.</span>
              <button onClick={resend} className="d7-btn-secondary !px-3 !py-1.5 !text-xs">Resend email</button>
              <Link href={`/invoices/${invoiceId}`} className="text-sm font-bold text-[#ff6b6b] hover:text-white">
                Open invoice
              </Link>
            </div>
          )}
        </>
      )}
    </Shell>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen">
      <Sidebar />
      <div className="lg:pl-[272px]">
        <Navbar />
        <main className="max-w-5xl d7-page">{children}</main>
      </div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase tracking-[0.1em] text-[#8a8a8a]">{label}</p>
      <p className="mt-1 break-all text-sm text-[#e5e5e5]">{value}</p>
    </div>
  );
}
