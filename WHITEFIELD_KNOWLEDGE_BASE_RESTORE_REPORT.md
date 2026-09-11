# Whitefield Knowledge Base — Restore Report

Date: 2026-09-11. Symptom: Admin → AI Assistant → Knowledge shows `Classes · 0`
with "Nothing here yet", and chat falls back to "I don't have that information…".

## 1. Root cause

The backend was reachable and the branch resolved (an empty list, not a 404),
so the Whitefield knowledge rows were never inserted or were wiped without a
re-run. Contributing defects found in the seed path and fixed:

1. **No self-healing.** The old guard (`if version matches → return`) left an
   empty/partial knowledge base empty forever. Fixed: reseed also triggers when
   `ai_classes` count is 0 for the branch, with a warning log.
2. **Duplicate settings could abort the migration.** Earlier attempts may have left
   duplicate `(tenant, branch, key)` rows, which would fail the unique-index build.
   Fixed: duplicates are deleted (keeping latest) before index creation.
3. **DDL failure could fail boot.** Fixed: table creation and seeding are each
   wrapped — boot never dies on AI schema/seed problems; failures are logged.
4. **No visibility.** There was no way to verify counts without DB access. Fixed:
   new staff endpoint `GET /api/ai/admin/diagnostics?branch=` (tenant, branch id,
   dataset version, per-table counts) surfaced as a "Data check" card on
   `/admin/ai` (zero counts highlighted red, except offers/leads/conversations).
5. **Operational (most likely): the fixed backend may not be deployed yet.**
   The versioned reseed (`whitefield-v2`) only runs on backend boot. If Railway is
   still on an older build, no seed exists. Redeploy is required (see §5).

Ruled out: tenant isolation (admin + chat + seed all use `default`), slug
resolution (tolerant `slugify` match), conversation/lead storage (separate tables,
never wiped by reseed).

## 2. Database counts

Direct database access is not available from this environment, so counts below
are expectations, not live query results. After redeploy, open Admin → AI
Assistant and read the "Data check" card, or call the diagnostics endpoint
(staff auth). If `classes` is still 0 there, check Railway logs for
`Dance7 AI seed failed` / `already present`.

| Table | Before (reported) | Expected after reseed |
|-------|-------------------|-----------------------|
| `ai_classes` | 0 | 7 (Sub Juniors, Juniors, Level 1 Kids, Level 2 Kids, Freestyle Adults Beginners, Level 1 Adults, Bharatanatyam) |
| `ai_class_schedules` | 0 | 17 slots (spec days/timings) |
| `ai_packages` | 0 | 4 (₹2,500 / ₹6,750 / ₹13,005 / ₹23,040) |
| `ai_studio_settings` | 0 | 7 incl. `admission_fee=500`, `contact_phone=9731067867`, `dataset_version=whitefield-v2` |
| `ai_chat_faqs` | 0 | 8 (fees, timings, admission, kids, Bharatanatyam, trial script, contact, join) |
| `ai_chat_policies` | 0 | 2 (admission; unlisted-policies pointer — nothing invented) |
| `ai_chat_offers` | 0 | 0 (none published — honest) |

Requested `SELECT COUNT(*)` statements to run against the database directly:

```sql
SELECT COUNT(*) FROM ai_classes;
SELECT COUNT(*) FROM ai_class_schedules;
SELECT COUNT(*) FROM ai_packages;
SELECT COUNT(*) FROM ai_faqs;      -- note: actual table is ai_chat_faqs
SELECT COUNT(*) FROM ai_policies;  -- note: actual table is ai_chat_policies
SELECT COUNT(*) FROM ai_offers;    -- note: actual table is ai_chat_offers
SELECT id, name FROM branches WHERE LOWER(name) LIKE '%whitefield%';
SELECT setting_key, setting_value FROM ai_studio_settings WHERE branch_id = <whitefield_id>;
```

## 3. Seed data inserted (on backend boot)

Exact Whitefield spec dataset v2: 7 classes with age bands + ₹2,500 fees, 17
schedule slots, 4 packages with exact configured prices, admission ₹500, phone
9731067867, trial/enrollment/unknown scripts as settings + FAQs, 2 non-invented
policies. Replaces any stale placeholder rows for Whitefield only; leads and
conversations preserved. Verified in code review against the approved spec
(names, ages, days, times, prices, phone all match).

## 4. Whitefield branch ID

Resolved at runtime (`branches` row named Whitefield, tenant `default`) and now
displayed in Admin → AI Assistant → "Data check · {name} (id {id})". Not hardcoded.

## 5. Chatbot validation

- Code-level: all six validation questions trace to live retrieval paths
  (classes → `getClasses`; age queries → recommender age fit; Bharatanatyam →
  keyword fit; fees → `getPackages` + admission setting; contact → branch details).
  `mvn test` covers intent routing incl. the exact reported queries.
- Live validation requires the redeployed backend + seed run: ask the six
  questions in the widget and confirm DB-sourced answers (prices/slots/ages).
- If any answer still falls back after redeploy, the diagnostics card pinpoints
  which table is empty.

## 6. Validation results

- `mvn test` ✅ — 9 tests, 0 failures.
- Frontend `npm run build` ✅ — 28/28 static pages.
- Also corrected: `WHITEFIELD_AI_DATA_AUDIT.md` test count (9, not 10).

## 7. Commit / push

- Commit: "Restore Whitefield AI knowledge base" (`7ade99a`).
- Push: `origin/main` (manojacharya3/Dance-7) — ✅ synced.
