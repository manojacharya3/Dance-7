# Whitefield AI Data Audit — Dance7 Assistant Knowledge Base

Date: 2026-09-11 · Scope: Whitefield branch only. Previous placeholder seed replaced
by the verified spec dataset (`dataset_version = whitefield-v2`, auto-applied on
backend boot; leads/conversations never touched).

## 1. Audit findings (old seed vs spec)

| Area | Expected (spec) | Was seeded | Verdict |
|------|-----------------|------------|---------|
| Classes | 7: Sub Juniors, Juniors, Level 1 Kids, Level 2 Kids, Freestyle Adults Beginners, Level 1 Adults, Bharatanatyam | 4 wrong (Kids/Adult/Bharatanatyam/Contemporary, wrong ages/fees) | ❌ replaced |
| Schedules | 17 slots (MWF 4:30/5:30/6:30 PM kids; Tue/Thu 5–6:30 PM L2; Mon/Wed 7:30 PM adult beginners; Thu/Fri 7:30 PM adult L1; Sat 2:30 PM + Sun 9 AM classical) | 7 wrong slots | ❌ replaced |
| Packages | 1 Mo ₹2,500 · 3 Mo ₹6,750 (10% OFF) · 6 Mo ₹13,005 (15% OFF) · 12 Mo ₹23,040 (20% OFF), exact prices | 3 wrong (2500/2700/7000) | ❌ replaced |
| Admission fee | ₹500, separate from packages | ₹1000 | ❌ fixed |
| Branch phone | 9731067867 | 9731067867 | ✅ kept |
| Trial policy | exact "don't have" script | invented trial booking ❌ | ❌ replaced (setting + FAQ) |
| Offers | none published | invented "Welcome offer" ❌ | ❌ removed (table empty = honest "no offers") |
| Refund/makeup/holiday policies | must NOT be invented | invented ❌ | ❌ replaced with "Unlisted policies → contact branch" |
| Instructor names | must NOT be invented | none seeded ✅ | ✅ (slots carry no instructor; replies omit it) |
| Address | must NOT be invented | honest placeholder setting | ✅ removed (replies omit address) |
| Lead statuses | NEW/CONTACTED/TRIAL/ENROLLED/LOST | missing TRIAL/LOST | ❌ added (CLOSED retained for legacy rows) |
| Class fee column | per-class fee in model | missing | ❌ added (`ai_classes.fee_amount`, entity/DTO/CRUD) |

## 2. Fixes applied (code, not just data)

- `AiChatSchemaMigration`: versioned reseed (`whitefield-v2`) — wipes branch AI
  knowledge and inserts the exact 7/17/4/8-settings/8-FAQ/2-policy dataset; user
  data preserved; boot-safe try/catch retained.
- `AiChatService`: bare join interest → new `JOIN` clarification flow (child/adult +
  age, spec wording); `RECOMMEND` without age/level/category signals → `CLARIFY`
  instead of guessing; `trial_info` setting threaded into facts.
- `RuleBasedAiProvider`: spec-exact admission ("The one-time admission fee is ₹500."),
  contact ("📞 9731067867"), trial (setting/FAQ or honest unknown — never invented),
  fee list (exact packages + admission line), recommendation card format
  (name/age/days/time/fee + package follow-up), lead reply (no enrollment claims),
  `leadPrompt` on LEAD/FEES/RECOMMEND/JOIN.
- `AiRecommendationService`: recommendations now carry age range, schedule and fee
  from the database (new `fee_amount` column).
- Widget: exact spec welcome message; `whitespace-pre-wrap` bubbles for card format.
- Admin: class fee field; lead statuses TRIAL/LOST end-to-end.
- Tests: `refineIntent`/`hasCategorySignal` coverage (10 tests total).

## 3. Answer verification (spec questions → source)

| Question | Answer source (all DB retrieval) |
|----------|----------------------------------|
| "What classes do you offer?" | `getClasses` (7 live rows) |
| "My child is 4 / 10 years old" | recommender age fit → Sub Juniors / Level 1 Kids + live schedule + ₹2,500 fee |
| "I am an adult beginner" | level fit → Freestyle Adults Beginners, Mon/Wed 7:30–8:30 PM |
| "Tell me about Bharatanatyam" | keyword fit → Bharatanatyam, all ages, Sat/Sun slots |
| "What are the package options?" | `getPackages` exact 2500/6750/13005/23040 + admission ₹500 |
| "How much is admission?" | `admission_fee` setting → "The one-time admission fee is ₹500." |

No reply path contains branch facts in code (verified: phone/fee/schedule strings
exist only in migration seed data, admin-editable, effective next turn).

## 4. Validation

- `mvn test` ✅ — 10 tests, 0 failures (covers JOIN split, CLARIFY signals,
  fallback sentence, intent matrix, guardrails).
- `mvn compile` ✅ — BUILD SUCCESS (via test run).
- `npm run build` ✅ — 28/28 pages, `/admin/ai*` routes generated.

## 5. Commit / push

- Commit: "Complete Whitefield chatbot knowledge base" (see `git log`).
- Push: `origin/main` (manojacharya3/Dance-7) — synced.
