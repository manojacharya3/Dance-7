# Dance7 UI Redesign — Master Plan (Modern SaaS, v2)

> Inspired by modern SaaS principles (premium feel, large typography, strong hierarchy,
> spacious layout, rounded cards, mobile-first, bold CTAs, clean white backgrounds,
> minimal clutter). **Not a copy of any reference design** — original implementation.

## 0. Implementation status (this repo)

| Phase | Scope | Status | Build |
|-------|-------|--------|-------|
| 1 | Design system, navigation (sidebar/topnav/workspace), dashboard, login/register | ✅ Done | ✅ `npm run build` passes |
| 2 | CRUD pages: students, instructors, batches, memberships (+ shared filters/forms/tables token migration) | ✅ Done | ✅ `npm run build` passes |
| 3 | Payments, invoices, reminders, attendance, admin/users, feedback | ✅ Done (token migration + filter-bar rewrite) | ✅ `npm run build` passes |
| 4 | Mobile optimization (bottom nav, stacked filters, table scroll, safe-area padding) | ✅ Done | ✅ `npm run build` passes |

All business logic, data fetching, role scoping and APIs are untouched. Only presentation
(classes, layout shells, shared components) changed.

---

## 1. Before / After design rationale

### Before
- Cream canvas (`#faf9f6` / `#f5f7f2`), dark olive sidebar (`#18232b`), lime accent (`#d5f45b`),
  teal links (`#21816b`), Arial body font.
- Sidebar: single flat list, no grouping, no workspace context, 256px dark panel.
- Topbar: 80px tall, static breadcrumb text, icon-only actions, no search input.
- Dashboard: small `text-3xl font-semibold` heading, 8 identical stat tiles, dense
  analytics grids, dark CTA button.
- Auth: dark full-bleed screens with glass card — heavy, off-brand for a daytime business app.
- Filters: cramped `p-4` bar, beige inputs (`#fdfcf9`), no search icon, selects squeeze on mobile.
- Tables: beige thead (`#fdfcf9`), low-contrast uppercase labels, no rounded container standard.
- Errors used custom coral (`#fff0ed` / `#b84639`); focus rings used teal.

### After
- **Clean white SaaS canvas**: page `slate-50 (#F8FAFC)`, cards pure white, ink `slate-900`,
  muted `slate-500`, borders `slate-200/100`. Teal/lime legacy tokens retained in
  `tailwind.config.ts` as `accent` for backward compatibility only.
- **White grouped sidebar (272px)**: Studio / Finance / System sections, active item is a
  solid violet pill, gradient `D7` logo, branch **workspace switcher** card, user card footer.
- **Sticky blurred topbar (64px)**: contextual page title, inline student search (desktop),
  reminder bell with dot, avatar gradient, mobile drawer + **mobile bottom tab bar**
  (Home / Students / Batches / Payments / More).
- **Dashboard**: greeting headline (`Good morning, {name}`), role eyebrow, revenue hero stat
  with INR formatting, attention queue card (overdue / expiring / attendance), upcoming
  classes list, quick-action grid (mark attendance, batch attendance, record payment,
  new membership). Developer scope gets a dedicated summary.
- **Auth**: split-screen SaaS — violet gradient brand panel (desktop) + white form panel,
  `d7-input` fields with labels/hints, loading spinner button, alert roles preserved.
- **Filters**: `d7-filterbar` — search input with icon, 2-col grid selects on mobile,
  inline row on desktop, `aria-label`s preserved.
- **Tables/cards/forms**: standardized on `d7-card`, `d7-table-wrap` pattern, slate thead,
  violet hover rows, `d7-pill-*` status tones, `d7-error`/`d7-success` banners,
  violet focus rings (`focus:border-violet-500 focus:ring-violet-100`).
- **Typography**: Inter system stack, `text-3xl→4xl extrabold tracking-tight` H1s,
  `11–12px bold uppercase` eyebrows/labels, 15px relaxed body.

---

## 2. Design tokens

### Colors (`app/globals.css` `:root` + `tailwind.config.ts`)
| Token | Value | Usage |
|-------|-------|-------|
| `--canvas` | `210 40% 98%` (#F8FAFC) | Page background |
| `--surface` | `0 0% 100%` | Cards, topbar, sidebar |
| `--ink` | `222 47% 11%` (#0F172A) | Headings, body |
| `--muted` | `215 16% 47%` (#64748B) | Secondary text |
| `--faint` | `215 20% 65%` (#94A3B8) | Placeholders, hints |
| `--line` | `214 32% 91%` (#E2E8F0) | Borders |
| `--brand` | `262 83% 58%` (#7C3AED violet-600) | Primary CTA, active nav |
| `--brand-strong` | `263 70% 50%` (#6D28D9) | Hover |
| `--brand-soft` | `252 100% 95%` | Focus rings, tints |
| `--success / --warning / --danger` | emerald / amber / red | Pills, banners |

### Typography
- Family: `Inter, ui-sans-serif, system-ui` (`--font-sans`).
- `d7-eyebrow`: 12px bold uppercase, tracking 0.14em, violet-600.
- `d7-h1`: 30px → 36px extrabold tracking-tight, slate-900.
- `d7-sub`: 14–15px, slate-500, relaxed leading.
- Table head: 11px bold uppercase tracking 0.1em slate-400.

### Spacing
- Page: `max-w-7xl, px-4 → sm:6 → lg:8, pt-6/8, pb-16` (`.d7-page`); narrow variant `.d7-page-narrow`.
- Cards: `p-5 → sm:p-6`, `gap-4/6` grids (`sm:2 / xl:4` stats, `lg:2` panels).
- Mobile bottom-nav clearance: `padding-bottom: 6.5rem` under 1024px.

### Shadows
- `--shadow-card`: `0 1px 2px + 0 1px 3px rgba(16,24,40,.06/.08)`.
- `--shadow-pop`: `0 8px 24px -12px rgba(16,24,40,.22)` (hover lift).
- Focus: `0 0 0 4px brand-soft`.

### Border radius
- Cards: 20px (`rounded-[20px]`, `--radius-card`).
- Inputs/buttons: 12px (`rounded-xl`, `--radius-input`).
- Pills/avatar: full (`999px`).

### Component patterns (`app/globals.css` `@layer components`)
`d7-page`, `d7-page-narrow`, `d7-eyebrow`, `d7-h1`, `d7-sub`, `d7-card`,
`d7-card-hover`, `d7-btn` + `d7-btn-primary/dark/secondary/ghost/danger`,
`d7-input`, `d7-label`, `d7-hint`, `d7-error`, `d7-success`,
`d7-pill-*` (green/red/amber/slate/violet/blue),
`d7-table-wrap`, `d7-table-scroll`, `d7-th`, `d7-td`, `d7-row`,
`d7-filterbar`, `d7-icon-btn`, `d7-avatar`, `d7-skeleton`.

---

## 3. Component inventory

### New / rewritten
| Component | Path | Notes |
|-----------|------|-------|
| Design tokens + patterns | `app/globals.css` | Single source of truth |
| Tailwind theme | `tailwind.config.ts` | `surface/brand/faint/success/warning/danger`, shadows |
| `Button` | `components/ui/button.tsx` | primary/dark/secondary/ghost/danger |
| `Card`, `PageHeader`, `StatCard`, `EmptyState`, `LoadingState`, `StatusPill` | `components/ui/card.tsx` | shadcn-style, Tailwind-only, Lucide-ready |
| `Sidebar` | `components/sidebar.tsx` | Grouped nav, workspace switcher, user card; `visibleNavItems()` + role logic preserved |
| `Navbar` | `components/navbar.tsx` | Sticky blurred topbar, search, drawer, bottom tabs |
| `ManagementFilters` | `components/management-filters.tsx` | SaaS filter bar; same props/callbacks |
| Dashboard | `app/dashboard/page.tsx` | Same fetches + role scoping, new widgets |
| Login / Register | `app/login/page.tsx`, `app/register/page.tsx` | Same `login()`/`register()` flows, `?next` preserved |

### Migrated (token codemod, logic untouched)
Students (list/new/detail/edit/batches/performance), Instructors, Batches,
Memberships (+ table/form), Payments (+ table/form), Invoices (+ table/details),
Attendance (+ table/form/batch form), Reminders, Feedback, Admin/Users,
`auth-guard` splash, all form/table components.

### Preserved business functionality (no API/backend changes)
Role scoping (Owner/Branch-head/Instructor/Developer), branch→batch→student
assignment joins, fee/outstanding math (`fee − PAID`), expiring (≤30d) / overdue
buckets, duplicate-safe batch attendance, membership-scoped payment cascade
(branch→membership→student info, txn-ref rules), invoice generation from payment,
feedback triage permissions, user disable/remove confirmations, pagination/search.

---

## 4. Rollout strategy

1. **Merge as-is** — pure presentation diff; `npm run build` green on all phases.
2. **Smoke test by role**: Owner, Branch-head, Instructor, Developer → dashboard scope,
   nav visibility, CRUD, record payment → generate invoice, batch attendance save.
3. **Visual QA matrix**: desktop 1440 / tablet 768 / mobile 390 on login, dashboard,
   students, payments/new, attendance/batch, reminders, admin/users.
4. **Follow-ups (optional)**: dark mode (`class` strategy already in config),
   table→card transformation on smallest screens, skeleton loaders on list pages
   (component `LoadingState` already available), screenshot-driven polish pass.

## 5. Screenshots to capture
- [ ] Login + Register (desktop split + mobile stacked)
- [ ] Dashboard Owner (stats, attention queue, upcoming classes, quick actions)
- [ ] Dashboard Instructor + Developer variants
- [ ] Sidebar (grouped) + workspace switcher + mobile drawer + bottom tabs
- [ ] Students list (filters, table, empty state) + student detail
- [ ] Attendance + batch attendance (prefill states)
- [ ] Payments/new cascade (info cards) + Payments table + invoice detail
- [ ] Memberships table + Reminders buckets + Admin/Users modals
- [ ] Loading state + empty state + error banner examples
- [ ] 390px mobile pass for each of the above

## 6. Implementation roadmap
- [x] Phase 1 — Design system, navigation, dashboard, auth
- [x] Phase 2 — CRUD pages token migration (students/instructors/batches/memberships)
- [x] Phase 3 — Payments/memberships/reports (invoices/reminders/attendance/admin/feedback)
- [x] Phase 4 — Mobile optimization (drawer, bottom nav, stacked filters, scroll tables)
- [ ] Phase 5 (proposed) — Empty/loading states wired into every list, report exports,
      audit of `Metric`/`Stat` legacy helpers → `StatCard`, dark-mode toggle.
