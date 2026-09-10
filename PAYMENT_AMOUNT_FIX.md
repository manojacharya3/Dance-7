# Payment Amount Fix

## Root Cause

The PaymentForm amount field was numeric with a positive minimum but did not declare a decimal step. Browser validation could reject decimal values as invalid, and the currency context was not explicitly separated from the numeric input.

## Files Changed

- `components/payment-form.tsx`
- `PAYMENT_AMOUNT_FIX.md`

## Fix Applied

- Added `step="0.01"` to numeric Payment amount inputs.
- Kept the positive minimum of `0.01`.
- Displayed `INR` as a separate label outside the input.
- No backend changes were needed.

## Backend Verification

- `PaymentDTO.amount` is `BigDecimal` with positive `@DecimalMin` validation.
- `Payment.amount` is `BigDecimal` with database precision 12 and scale 2.

Accepted examples include `500`, `2000`, `2500`, `10000`, and `2500.50`.
