# Screenshot Capture Matrix — Dance7 v2

How to capture: `npm run dev`, log in per role, 1440px / 768px / 390px viewports.
Tick off during pilot; store under `docs/screenshots/` (not yet created).

## Desktop (1440px)
- [ ] Login (`/login` — split brand + form)
- [ ] Dashboard Owner (`/dashboard` — greeting, revenue, attention queue, classes, quick actions)
- [ ] Dashboard Instructor (scoped tiles + linked-profile state)
- [ ] Dashboard Developer (summary tiles)
- [ ] Students (`/students` — filters, table, pagination)
- [ ] Student detail (`/students/[id]` — profile, membership, outstanding)
- [ ] Instructors (`/instructors`)
- [ ] Batches (`/batches`)
- [ ] Memberships (`/memberships` + `/memberships/new`)
- [ ] Payments (`/payments` + `/payments/new` cascade) + Invoice detail (`/invoices/[id]`)
- [ ] Attendance (`/attendance` + `/attendance/batch?batchId=`) + Reminders (`/reminders`)
- [ ] Feedback (`/feedback`) + Admin/Users (`/admin/users` + create modal)
- [ ] Sidebar grouped nav + workspace switcher + topbar search

## Tablet (768px)
- [ ] Dashboard (2-col stats/panels, drawer via hamburger)
- [ ] Students (stacked filters, scrolling table)
- [ ] Payments (filter bar + table scroll)

## Mobile (390px)
- [ ] Login (stacked form)
- [ ] Dashboard (stacked stats, quick actions, bottom tabs visible)
- [ ] Workspace switch (open drawer → branch select) ← fixed this pass, must capture
- [ ] Students (filter grid, table scroll)
- [ ] Payments (`/payments/new` full-width cascade cards)
- [ ] Memberships (table scroll + status pills)

## States (any viewport)
- [ ] Loading skeleton (dashboard cold load)
- [ ] Empty table ("No … found")
- [ ] Error banner (e.g. unlinked instructor profile)
