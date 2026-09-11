# Razorpay Payment Automation Report — Dance7

Date: 2026-09-11. Production-grade online payments integrated into the existing
Students → Memberships → Payments → Invoices architecture. No existing business
logic changed (offline flows, enums only extended, invoice uniqueness preserved).

## 1. Architecture

```
Parent/Staff (payment detail → Pay Now)
  │  Razorpay Checkout (checkout.js, brand-red theme, order_id from server)
  ▼
POST /api/payments/verify {order_id, payment_id, signature}
  │  HMAC-SHA256 verify (JCA, constant-time) — NEVER trusts frontend success
  ▼  (or verified webhook payment.captured/order.paid — duplicate-safe)
capture(): Payment PAID + paid_at + refs → Membership ACTIVE + start/end from
duration → Invoice (reuse-or-create by payment_id) → PDF render + store →
email receipt → payment_events audit. Every step idempotent and retried safely.
```

## 2. Database changes (`RazorpaySchemaMigration`, @Order(3), additive IF NOT EXISTS)

- `payments`: `razorpay_order_id` (+index), `razorpay_payment_id`,
  `razorpay_signature`, `paid_at`, `receipt_number`.
- `PaymentStatus` extended: PENDING, PROCESSING, PAID, FAILED, REFUNDED, CANCELLED
  (existing PAID/PENDING revenue queries unaffected; no CHECK constraints existed).
- `invoices`: `pdf_data BYTEA`, `sent_at`.
- New `payment_events` audit table (event/detail, no secrets).

## 3. APIs created

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `POST /api/payments/create-order {paymentId}` | staff | Creates Razorpay order (paise), stores order id, marks PROCESSING; reuses open order |
| `POST /api/payments/verify` | staff | Verifies checkout signature → capture pipeline → returns invoice |
| `POST /api/payments/webhook` | public + HMAC | `payment.captured`/`order.paid` → capture; `payment.failed` → FAILED; unknown orders acked; duplicates acked |
| `GET /api/payments/razorpay/status` | staff | `{enabled, keyId, currency, emailEnabled}` (drives Pay Now visibility) |
| `GET /api/payments/{id}/invoice` | staff | Invoice lookup for a payment |
| `GET /api/invoices/{id}/pdf` | staff | PDF download (renders on first request, then stored bytes) |
| `POST /api/invoices/{id}/send` | staff | Re-render (if needed) + resend email |

## 4. Razorpay integration details

- No third-party SDK: orders via `RestClient` + Basic auth (5 s/15 s timeouts);
  signatures via JCA HMAC-SHA256 with constant-time compare.
- Disabled by default (`DANCE7_RAZORPAY_ENABLED=false`); needs `RAZORPAY_KEY_ID`,
  `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET` for webhooks. Razorpay
  method mapped onto existing `PaymentMethod` (UPI/CARD/BANK_TRANSFER).
- Amounts in paise (`toPaise`, HALF_UP, exact).

## 5. Webhook implementation

Raw-body HMAC verification before parsing; tenant resolved from the stored order
(order ids globally unique); `payment.failed` marks FAILED; captures are
duplicate-safe (already-PAID → ack + audit); unknown orders acked to stop retries.

## 6. Invoice generation flow

Existing `generateFromPayment` reused (unique per payment — duplicate invoices
impossible); OpenPDF renders branded "DANCE7 — THE ART FACTORY" receipt
(number, dates, student, branch, plan, amount, reference, status) stored to
`pdf_data`; download via attachment response.

## 7. Email automation flow

`spring-boot-starter-mail`, gated by `DANCE7_EMAIL_ENABLED` + SMTP env; subject
"Payment Confirmation - Dance7" with amount, invoice number, PDF attachment;
`sent_at` set only on success; failures logged and never fail payments; resend
endpoint resets and retries.

## 8. Parent & admin experience

- Payment detail: **Pay Now** (Razorpay Checkout → verify → invoice notice),
  **Download Invoice**, resend email, gateway refs, paid-at; unconfigured studios
  see a guidance banner.
- Dashboard: **Finance today** strip (today's revenue, pending, failed, month,
  invoice count). New **/reports** page (sidebar → Finance): revenue by month,
  payments by status, CSV exports for revenue/payments/invoices.
- Payment history remains visible per student + payments list (new statuses render).

## 9. Security controls

Server-side signature verification on every capture; webhook HMAC before parse;
idempotency on verify/webhook/invoice/PDF/email; `payment_events` audit trail;
secrets env-only (never logged/returned; only publishable key_id exposed);
existing role filters unchanged (webhook explicitly public, signature-gated).

## 10. Testing & builds

- `mvn test` ✅ — 13 tests, 0 failures (9 existing + 4 new HMAC/paise/gating tests).
- `mvn compile` ✅ (via test run). New deps: `openpdf`, `spring-boot-starter-mail`
  (both Maven Central, compile-verified).
- `npm run build` ✅ — 29/29 pages incl. `/reports`.
- Manual pilot checklist: test-mode success, failure, cancellation, duplicate
  webhook replay, PDF download, email receipt, membership ACTIVE + dates.

## 11. Commit / push

- Commit: "Add Razorpay payments with automated invoicing" (see `git log`).
- Push: `origin/main` (manojacharya3/Dance-7) — synced.
