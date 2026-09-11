# Payments Icon Replacement (sidebar navigation)

## Finding

The Payments menu item (`components/sidebar.tsx:16`) used Lucide `Receipt` —
verified by direct inspection. No `DollarSign`, `CurrencyDollarIcon`, or custom
SVG existed anywhere in the repo (full project search, plus the earlier
`PAYMENT_ICON_INVESTIGATION.md` audit). `Receipt` is a currency-neutral receipt
glyph, but per the explicit request it was replaced with the first-preference
rupee icon.

## File modified

- `components/sidebar.tsx` (2 lines: import + menu config).

## Icon replacement

- Previous icon: `Receipt` (neutral; no `$` glyph).
- New icon: `BadgeIndianRupee` (verified exported by installed
  `lucide-react@0.468.0`; first item in the requested preference order).
- Navigation unchanged: same label `Payments`, same `href: "/payments"`, same
  role-visibility logic. No payment logic, calculations, or currency formatting
  touched (dashboard Revenue stat keeps `Receipt`, out of scope).

## Build validation result

- `npm run build` → success, all routes compiled, no type errors.

## Commit / push

- Commit: `Replace payments sidebar dollar icon` (hash below after push).
- Push: `origin/main`.
