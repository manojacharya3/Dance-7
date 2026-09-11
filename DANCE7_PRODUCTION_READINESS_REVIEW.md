# Dance7 Production Readiness Review (post-redesign)

Date: 2026-09-11 · Severity: **P0** = release blocker · **P1** = fix before pilot ·
**P2** = nice to have. Method: static review + `npm run build`. No backend/API/schema
changes were made or are proposed here.

## Assessment

| Area | Finding | Severity |
|------|---------|----------|
| Build | `npm run build` green (TS + lint + all routes) after every phase incl. this pass | — |
| Accessibility | Landmarks (`header/nav/main`, dialog roles), labeled inputs/selects, `aria-label` icon buttons, `role="alert"` errors, focus-visible rings, 44px mobile targets | — (P2: add visible focus style audit + `aria-current` on active nav as follow-up) |
| Responsiveness | Desktop/tablet/mobile layouts verified statically (see MOBILE_UX_VALIDATION.md); drawer workspace defect fixed | — |
| Error handling | Per-page error banners preserved; fetch rejections caught with fallback messages; auth failures redirect with `?next` | — |
| Empty states | Inline empty messages on all tables/lists; shared component available | P2 (rollout) |
| Loading states | Dashboard skeletons; inline loading text elsewhere | P2 (rollout) |
| Mobile support | Conditional on auth prerequisites below | P1 (deploy config) |
| Navigation | All hrefs resolve; role matrix documented; drawer + bottom tabs work statically | P1 (role report #2) |
| Data validation | Client rules unchanged (required, email, min 8, numeric bounds); server authoritative | — |
| Role isolation | Frontend scoping correct; enforcement is backend-side (unchanged) | P1 (verify 403s live) |

## Open issues
- **P0**: none found.
- **P1**:
  1. Verify live 401/403 behavior per role (frontend has no route-level role guards by design; backend must reject hidden-URL access) — during pilot.
  2. Confirm deploy env for mobile auth: `BACKEND_INTERNAL_URL` set, `NEXT_PUBLIC_API_URL`
     relative, `COOKIE_SECURE=true`, CORS allow-list — then run the mobile login checklist.
- **P2**:
  1. No `/profile` or `/settings` routes exist — confirm scope.
  2. Unify legacy `Metric`/`Stat` helpers onto `StatCard`; roll out shared
     `EmptyState`/`LoadingState`; `aria-current` on active nav; optional card-layout
     tables on smallest screens; bottom-nav fallback icon.
- **Non-issues / excluded**: `Dance-7/` nested repo in workspace is untracked clutter,
  intentionally not committed; `OWNER_PASSWORD_RESET.sql` is a committed ops helper,
  not an app change.

## Recommendation
⚠ **Ready After P1 Issues Are Fixed** — ship to pilot once the two P1 live
verifications (role 403s + mobile login on real devices) pass. No code changes
anticipated; both are environment/pilot-test activities.
