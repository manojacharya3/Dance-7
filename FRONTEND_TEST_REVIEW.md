# Dance7 Frontend Test Review

## Reviewed Areas

- Navigation: root redirect, login/register links, sidebar Students route, auth guard redirects.
- Sidebar: implemented routes are Overview and Students; dead feature links were removed.
- Forms: login, registration, and StudentForm have browser-required fields and submit loading state.
- Responsiveness: Tailwind responsive grids and horizontal table overflow are present.
- Loading: auth guard, student list, student detail/edit, and form submit have loading states.
- Errors: auth and student API failures display inline messages in most flows.

## Missing UI Improvements

1. Handle 401 responses centrally by attempting refresh or redirecting once; currently each page handles errors independently.
2. Add a delete error state in `app/students/page.tsx`; failed delete currently leaves an unhandled rejection.
3. Debounce search and cancel stale requests.
4. Add field-level server validation messages.
5. Add accessible status/live regions for loading and errors.
6. Add focus management after navigation and modal confirmation.
7. Add responsive mobile navigation; sidebar is hidden below `lg` with no replacement menu.
8. Add real user identity to navbar instead of static `Alex`/`AM`.
9. Disable or wire static dashboard buttons.
10. Add image validation/preview for `studentPhotoUrl`.
11. Add confirm/error toast rather than browser `window.confirm`.
12. Add automated keyboard/screen-reader checks.
