# User Delete Confirmation — Administration → Users

Scope: only `Administration → Users`. No backend changes. No redesign.

## Files modified

- `app/admin/users/page.tsx`
  - Added confirmation dialog state (`confirmUser`, `deleting`) and success banner state (`success`).
  - Replaced direct `disable(id)` call with `requestDelete(user)` → `confirmDelete()` flow.
  - Reused existing `disableManagedUser(id)` API from `lib/admin-users.ts` (no API changes).
  - `lib/admin-users.ts` — reused as-is, not modified.

## Confirmation dialog added

Reuses the existing modal pattern (`fixed inset-0 z-10 flex items-center justify-center bg-[#18232b]/40 p-6` + `rounded-2xl bg-white p-6`).

- Trigger: `UserX` disable button now calls `requestDelete(user)` instead of deleting immediately.
- Dialog (`role="dialog" aria-modal="true"`):
  - Title: `Confirm User Deletion`
  - Message: `Are you sure you want to delete/disable this user?`
  - User block:
    - `User` label
    - `{User Name}` → `confirmUser.fullName`
    - `{User Email}` → `confirmUser.email`
  - Buttons:
    - `Cancel` → `cancelDelete()` (disabled while deleting)
    - `Delete User` → `confirmDelete()` (shows `Deleting...` while in-flight, disabled to prevent double-click)

## Refresh logic added

In `confirmDelete()`:

1. Calls existing API: `await disableManagedUser(target.id)`.
2. Immediate UI update: `setUsers((current) => current.filter((u) => u.id !== target.id))` — row removed without page reload.
3. Success message: `setSuccess("User disabled successfully")` rendered in green banner (`bg-[#eaf7f0] text-[#1a7a4c]`), reusing the existing inline notification pattern.
4. Closes dialog: `setConfirmUser(null)`.
5. Auto refresh: `await load()` re-fetches `getManagedUsers()` + branches + instructors to stay in sync with server.

No `window.location.reload()` is used.

## Error handling

- On failure: `setError("Unable to delete user. Please try again.")` in the existing red banner (`bg-[#fff0ed] text-[#b84639]`).
- Row is NOT removed (filter only runs on success).
- Dialog stays open so the user can retry or Cancel.

## User experience improvements

- No more accidental deletes — explicit confirmation with name/email context.
- No stale rows — deleted/disabled user disappears immediately + list re-fetches.
- No manual page refresh needed.
- Clear feedback: success vs. error banners, `Deleting...` pending state, double-submit guard.
- Accessible dialog: `role="dialog"`, `aria-modal="true"`, `aria-labelledby="confirm-user-deletion-title"`.
- Existing create/edit form, table layout, styles, and backend behavior unchanged.
