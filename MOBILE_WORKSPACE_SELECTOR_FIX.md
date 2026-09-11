# Mobile Workspace Selector Fix

## Root cause

There is no discrete workspace-switcher component: "Workspace" is the sidebar's
nav section heading (single-tenant `default` workspace), and the desktop sidebar
is `hidden` below the `lg` breakpoint. So on phones/tablets the **entire**
navigation — not just a selector — was unreachable, while desktop was unaffected.

## Files modified

- `components/sidebar.tsx` — nav config exported as `navItems` (+ `NavItem`
  type); role filtering extracted into exported `visibleNavItems(user)` (same
  OWNER/DEVELOPER/INSTRUCTOR rules, desktop rendering byte-identical).
- `components/navbar.tsx` — hamburger (`Menu`, `lg:hidden`) opens a mobile
  drawer (`fixed inset-0 z-50`, backdrop-tap + X to close, closes on route
  change) rendering the same role-filtered Workspace nav with active-link
  highlight. Desktop header unchanged (button/drawer never render at `lg+`).

## Mobile UX changes

- Phones/tablets: tap hamburger (top app bar) → drawer with full Workspace nav →
  tap destination (drawer auto-closes). Backdrop dismiss, scrollable list,
  `role="dialog"`/`aria-modal` + labels. Visible on every page (Navbar is
  global) whenever logged in.
- Desktop: zero change — sidebar `lg:flex` as before; shared filter helper keeps
  both in sync.

## Build validation result

- `npx tsc --noEmit` → no errors in nav components.
- `npm run build` → success, all routes compiled.

## Commit / push

- Commit: `Add mobile workspace selector access` (hash below after push).
- Push: `origin/main`.
