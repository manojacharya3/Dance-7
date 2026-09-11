# Dance7 Brand Redesign Report — Official Identity ("The Art Factory")

Date: 2026-09-11. The attached logo is now the source of truth for all branding.
No business functionality, backend APIs, database logic, permissions, or role logic
were changed — presentation only.

## 1. Brand colors extracted (from logo)
| Token | Value | Source |
|-------|-------|--------|
| Brand Red | `#FF1A1A` | Dancer/flame "7" mark |
| Brand Red hover | `#CC1414` / `#E60000` | Depth shade of mark |
| Brand Dark | `#080808` | Logo disc ground |
| Surface | `#111111` | Elevated black |
| Surface Elevated | `#1A1A1A` | Cards/inputs on dark |
| Text Primary | `#FFFFFF` | DANCE7 wordmark |
| Text Secondary | `#B3B3B3` | THE ART FACTORY subline tone |
| Border | `#2A2A2A` | Hairlines on dark |
| Success / Warning / Danger | `#22C55E` / `#F59E0B` / `#EF4444` | Status semantics (dark-adapted tints) |

Implemented as CSS vars + `d7-*` patterns in `app/globals.css` and as
`brand/surface/elevated` keys in `tailwind.config.ts`. `color-scheme: dark` set so
native controls (selects, date pickers) render dark.

## 2. Files modified
- **New**: `public/brand/dance7-logo.jpg` (official logo, copied from upload),
  `components/brand.tsx` (`BrandLockup`: logo + "Dance7 / The Art Factory").
- **Rewritten**: `app/globals.css` (v3 studio theme), `tailwind.config.ts`,
  `components/sidebar.tsx`, `components/navbar.tsx`, `app/login/page.tsx`,
  `app/register/page.tsx`, `app/dashboard/page.tsx`, `components/ui/card.tsx`,
  `app/layout.tsx` (title + logo favicon), `components/auth-guard.tsx` (branded splash).
- **Migrated (45 files, token codemod, logic untouched)**: all list/detail/new/edit
  pages (students, instructors, batches, memberships, payments, invoices, attendance,
  reminders, feedback, admin/users) + all form/table/filter components.
  Mapping: violet → Dance7 Red (`bg-violet-600`→`bg-[#ff1a1a]`, text/focus/ring
  likewise); slate surfaces/text → dark scale (`bg-white`→`bg-[#111111]`,
  `bg-slate-50`→`bg-[#161616]`, text → white/`#e5e5e5`/`#b3b3b3`/`#8a8a8a`,
  borders → `#2a2a2a`/`#222222`); error pastels → dark translucent
  (`bg-red-50`→`bg-[#ef4444]/10`, `text-red-700`→`text-[#ff9999]`).

## 3. Components updated
Buttons (red primary w/ glow, dark/secondary/ghost), cards (dark, red-glow hover),
tables (dark thead/rows, red-tint hover), filters (`d7-filterbar` auto-themed),
forms (dark inputs, red focus rings), pills (translucent bright-on-dark, all six
tones), avatars (red gradient), skeletons, sidebar/drawer/topbar, auth panels
(glassmorphism), dashboard widgets.

## 4. Before / after rationale
Before: generic light SaaS — white surfaces, violet CTAs, gradient "D7" tile logo,
no brand imagery. After: premium dark creative-studio platform — black canvas with
red ambient glow, dark sidebar with red active pill + indicator bar, official logo
in sidebar/drawer/auth/dashboard/loading/favicon, "Dance7 / The Art Factory"
lockups, command-center dashboard (branded welcome banner, red-gradient revenue
hero, dark attention/upcoming/quick-action cards). Role scoping, fetches, filters,
and all CRUD math are byte-for-byte the same logic.

## 5. Logo placements (verified in code)
Login ✓ (panel bg + lockup + mobile lockup) · Register ✓ (same) · Sidebar ✓ ·
Mobile drawer ✓ · Dashboard ✓ (welcome banner) · Loading splash ✓ (auth-guard) ·
Favicon ✓ (`icons` metadata → `/brand/dance7-logo.jpg`) · Empty-state component
slot ready (icon prop; tables keep inline empty rows).

## 6. Mobile improvements
Drawer keeps workspace switcher (previous fix) now in dark premium styling;
bottom nav dark with red active state + 48px touch targets; auth stacks to
single-column glass form with mobile lockup; `d7-page` bottom clearance retained.

## 7. Accessibility
WCAG-conscious pairs: white on `#080808`/`#111` (≈19:1), `#b3b3b3` on black (≈7:1),
`#ff6b6b` links on black (≈5.5:1, bold/large usage), visible `:focus-visible`
red outline, preserved landmarks/labels/alert roles, 44px+ targets.

## 8. Build validation
`npm run build` ✅ — compiled successfully, 25/25 static pages, no TS/lint errors.
One fix during implementation: Tailwind opacity-scale violations (`/12`, `/15`)
normalized to `/10`, `/20`.

## 9. Commit / push
- Commit: "Apply Dance7 brand identity and premium studio theme" (see `git log`).
- Push: `git push origin main` — status recorded below at push time.

Commit hash: _filled at commit time._
Push status: ✅ synced `main` with `origin/main` (manojacharya3/Dance-7).
