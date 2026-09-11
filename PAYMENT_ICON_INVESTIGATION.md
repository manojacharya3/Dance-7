# Payment Icon Investigation (Dollar $ hunt)

## Result: no dollar icon or symbol exists anywhere in the UI code

Searched, case-sensitively and insensitively:
- `DollarSign`, `CurrencyDollar*`, `Dollar`, `Banknote`, `Coins`, `HandCoins`,
  `PiggyBank`, `Wallet`, `BadgeCent`, `CircleDot` → **zero hits** in
  `app/`, `components/`, `lib/`.
- Literal `$` currency symbols → **zero hits** (remaining `$` occurrences are
  JS template-literal `${...}` interpolation, not currency).
- Custom SVGs: **none** — no `.svg` files under `app/`, `components/`, or `public/`.
- `USD` / `en-US` currency formatting → none (the single `en-US` hit is a
  month-name label in `students/[id]/performance`).

## Payment-adjacent icons actually in use (all currency-neutral)

| Location | Icon | File |
|----------|------|------|
| Sidebar → Payments nav | `Receipt` | `components/sidebar.tsx` |
| Dashboard Revenue stat | `Receipt` | `app/dashboard/page.tsx` |
| Sidebar → Memberships nav | `CreditCard` | `components/sidebar.tsx` |
| Memberships table actions | `Eye/Pencil/Trash2` | `components/membership-table.tsx` |
| Payments table actions | `Eye/FileText/Pencil/Trash2` | `components/payment-table.tsx` |
| Payments page header | `Plus` | `app/payments/page.tsx` |

Lucide's `Receipt` is a receipt slip with lines and `CreditCard` a card with a
stripe — neither glyph contains a `$`. Amounts everywhere render via
`lib/currency.ts` (`en-IN`/`INR` → `₹`).

## Component / file / source list for dollar visuals

None found — there is nothing to list. (`DollarSign` exists in the installed
`lucide-react@0.468.0` package but is imported nowhere; `IndianRupee` and
`BadgeIndianRupee` are likewise available if a rupee glyph is ever wanted.)

## Changes made

None — business logic and visuals preserved. With zero modifications there was
nothing to build (last `npm run build` green), nothing to commit (empty commits
prohibited), and nothing to push.

## If a `$` is still visible on screen

It cannot come from this codebase: send a screenshot + URL and the exact
element will be traced (likely candidates outside code: stale cached bundle,
wrong deployment, or a browser extension overlay).
