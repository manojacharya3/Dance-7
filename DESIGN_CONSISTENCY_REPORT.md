# Design Consistency Report — Dance7 v2 (post-redesign)

Date: 2026-09-11 · Scope: all `app/` routes + `components/`. Verified by token grep
(zero remaining legacy hex: `#18232b #d5f45b #faf9f6 #f5f7f2 #fdfcf9 #e8e5df #eeeae3
#dcdad4 #21816b #969d9d #7b8285 #667078 #fff0ed #b84639 #f0c9a5 #e2ff7b`,
`focus-[#21816b]`, `divide-[#eeeae3]` — all migrated) and component review.

| Dimension | Standard | Coverage | Result |
|-----------|----------|----------|--------|
| Typography | Inter stack; `d7-eyebrow` / `d7-h1` / `d7-sub`; 11px uppercase table heads | All rewritten shells + migrated pages (headers already used eyebrow+H1 pattern; now violet/slate) | ✅ |
| Colors | slate-50 canvas, white surfaces, violet-600 primary, slate text scale | 100% token migration; legacy `accent` tokens kept in config for compat only | ✅ |
| Buttons | `d7-btn-primary/secondary/dark/ghost/danger`; violet-600 CTAs | New pages use `d7-btn-*`; migrated list CTAs use equivalent violet-600 rounded styles | ✅ |
| Cards | `d7-card` (20px radius, slate-200/80, card shadow, p-5/6) | Dashboard, filters wrap, detail sections; legacy `rounded-xl border bg-white p-6` sections map to same visual | ✅ |
| Tables | Slate-50 thead, 11px uppercase slate-400 heads, violet hover rows, scroll containers | All table components migrated | ✅ |
| Forms | Labeled `d7-input` / slate inputs, violet focus rings, hints, alert banners | All form components migrated incl. focus states | ✅ |
| Pills/status | `d7-pill-*` six-tone set | Dashboard + detail badges; table status cells use equivalent green/amber/slate pills | ✅ |
| Empty states | `EmptyState` component (icon + title + action) + inline "No … found" rows | Inline variants everywhere; shared component available for Phase 5 rollout | ⚠️ acceptable |
| Loading states | `LoadingState` skeletons; inline "Loading…" text | Dashboard uses skeletons; list/detail pages use inline text | ⚠️ acceptable |
| Spacing | `d7-page` (max-w-7xl, px-4/6/8, pb-16) | All 32 migrated mains converted (`d7-page`, detail keeps max-w-5xl/2xl via utility precedence) | ✅ |
| Shadows / radius | card/pop shadows; 20px cards, 12px inputs, full pills | Enforced via `d7-*` layer | ✅ |

## Residual P2 polish (not blockers)
1. Legacy local `Metric`/`Stat` helpers in list pages predate `StatCard` — visually
   aligned now; unify onto `StatCard` in Phase 5.
2. Wire shared `EmptyState`/`LoadingState` into every list page (currently
   dashboard-only for skeletons).
3. Bottom-nav "More" fallback icon for roles without `/reminders` (cosmetic).

Conclusion: the unified design system is applied consistently across all modules.
