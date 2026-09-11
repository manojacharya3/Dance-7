# Mobile UX Validation — Dance7 (post-redesign)

Date: 2026-09-11 · Method: static review of responsive implementation
(`components/navbar.tsx`, `components/sidebar.tsx`, `ManagementFilters`,
`app/globals.css`, list-page shells). Live-device pass still required — see matrix.

## Checklist

| Area | Implementation | Result |
|------|---------------|--------|
| Login page | Split layout collapses to single centered form (`lg:grid-cols-2`, brand panel `hidden lg:flex`); `px-5 py-10`, max-w-md, tap targets ≥44px | ✅ PASS |
| Register page | Same responsive pattern as login | ✅ PASS |
| Dashboard | Stats `grid sm:2 xl:4`; panels `lg:2` stack on mobile; `PageHeader` actions stack (`flex-col sm:flex-row`); greeting wraps (`min-w-0`) | ✅ PASS |
| Sidebar / mobile drawer | Desktop sidebar `hidden lg:flex`; mobile drawer `fixed w-[300px]` with overlay, close button, auto-close on route change, logout CTA | ✅ PASS |
| Workspace switching | Desktop: sidebar branch select. **Defect: mobile drawer had no workspace access** | ⚠️ FOUND → ✅ FIXED (branch select added to drawer, same `getBranches()` source; build green) |
| Bottom navigation | Fixed 5-tab bar (`Home/Students/Batches/Payments/More`), `lg:hidden`, active violet state; page bottom padding `6.5rem` under 1024px so CTAs are never covered | ✅ PASS |
| Students / Payments / Memberships / Invoices lists | Filters `flex-col → lg:flex-row` (`ManagementFilters` uses 2-col select grid on phones); tables in `overflow-x-auto` with `min-w-[1050–1200px]` | ✅ PASS (scroll, not squash) |
| Attendance forms | Single + batch forms use `grid sm:2` sections; date/status/score inputs full-width on mobile | ✅ PASS |
| Filter usability | Search inputs carry `aria-label`s, icon adornment, full-width on mobile | ✅ PASS |
| Text overflow | Titles use `truncate`/`min-w-0`; table cells `whitespace-nowrap` inside horizontal scroll containers; no page-level horizontal scroll (max-w-7xl + px-4) | ✅ PASS |
| Topbar | 64px sticky blurred; desktop search hidden on mobile (`hidden md:flex`), icon search link shown instead; title truncates | ✅ PASS |

## Fix applied in this pass
- `components/navbar.tsx`: mobile drawer now includes the Workspace branch
  switcher (previously desktop-sidebar only). Verified via `npm run build`.

## Residual notes (P2)
- Bottom-nav "More" tab links to `/reminders` for all roles (see role report §2).
- Tables scroll horizontally on phones by design; a future card-list transformation
  for the smallest breakpoint is optional polish, not a defect.
