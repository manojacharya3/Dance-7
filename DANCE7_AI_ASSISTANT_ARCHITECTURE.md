# Dance7 AI Assistant — Architecture (SaaS Front-Desk, DB-Driven RAG)

Version 1 · Live branch: **Whitefield** · Future branches (Indiranagar, HSR Layout,
Marathahalli, …) work with **zero code changes** — add the branch row + knowledge rows.

## 1. Architecture diagram

```
Visitor (web/mobile)
  │  Frontend (Next.js 15, dark premium theme)
  ▼
Chat Widget (components/ai-chat-widget.tsx — floating button, branch switcher,
│  quick actions, lead form, localStorage visitor token)
  │  POST /api/chat/public/message|leads · GET /api/chat/public/branches|recommend
  ▼  (Next.js /api rewrite → Spring Boot; first-party cookies preserved)
Chat API (AiPublicChatController — public; AiAdminController — staff auth)
  │  Branch Authorization (AiBranchContext.resolve: id-or-slug → active Branch,
  │  EntityNotFound otherwise; conversation ownership + branch-match checks)
  ▼
Knowledge Retrieval Layer (AiRetrievalService tools + AiRecommendationService)
  │  getClasses · getClassSchedules · getPackages · getAdmissionFee ·
  │  getBranchDetails · getFaqs · getPolicies · getOffers ·
  │  searchKnowledgeBase · recommend(age, level, interest)
  ▼  FactPack (exactly one branch's active rows + last 6 turns)
Dance7 AI Service (AiChatService orchestrator → Dance7AiPort)
  ├─► RuleBasedAiProvider (@Primary, deterministic templates) — default, no keys
  └─► HttpLlmAiProvider (OpenAI-compatible, conditional on dance7.ai.api-key) — upgrade path
  ▼
Response (+ intent label, leadPrompt flag, recommendations, quick actions)
```

No large hardcoded prompts, no huge context windows, no static FAQ bot: the model
layer only ever verbalizes branch-scoped tool facts.

## 2. Entity relationships

```
branches (existing) 1──∞ ai_classes 1──∞ ai_class_schedules
                    ├──∞ ai_packages
                    ├──∞ ai_studio_settings (key/value: admission_fee, contact_phone, trial_info, …)
                    ├──∞ ai_chat_faqs / ai_chat_policies / ai_chat_offers
                    ├──∞ ai_chat_conversations 1──∞ ai_chat_messages
                    └──∞ ai_chat_leads ──(conversation_id)── ai_chat_conversations
ai_chat_audit (branch_id?, conversation_id?, event, ip_hash — no bodies, no PII)
```

All AI tables carry `tenant_id` + `branch_id`. Deletes are soft (`active=false`)
except settings (hard delete) so the assistant stops using content instantly.

## 3. Database schema

Created idempotently by `AiChatSchemaMigration` (`@Order(2)`, `CREATE TABLE IF
NOT EXISTS` + `ON CONFLICT DO NOTHING` seeds — same convention as the existing
`BranchSchemaMigration`). Tables: `ai_classes`, `ai_class_schedules`,
`ai_packages`, `ai_studio_settings` (UNIQUE tenant/branch/key), `ai_chat_leads`,
`ai_chat_conversations`, `ai_chat_messages` (+ index on conversation), `ai_chat_faqs`,
`ai_chat_policies`, `ai_chat_offers`, `ai_chat_audit`. Whitefield seed (classes ×4,
schedules ×7, packages ×3 incl. ₹2500/₹2700 fees, settings incl. contact phone,
FAQs ×6, policies ×3, 1 valid offer) inserts only when Whitefield exists and has
no classes yet.

## 4. APIs

Public (`/api/chat/public/**`, permitAll): `GET /branches` (id/name/slug),
`POST /message` {branch, conversationId?, visitorId?, message} → {reply, intent,
leadPrompt, quickActions, recommendations, conversationId, visitorId},
`GET /recommend` {branch, age?, experienceLevel?, interest?},
`POST /leads` (validates phone/email/names → stores `NEW` lead, marks conversation
leadCaptured, returns confirmation).
Staff (`/api/ai/admin/**`, authenticated; dev read-only GETs): CRUD
`classes|schedules|packages|settings|faqs|policies|offers` (POST upsert incl. id,
DELETE soft-deactivates), `GET /leads` (+status filter), `PATCH /leads/{id}`
(NEW→CONTACTED→ENROLLED→CLOSED), `GET /analytics` (conversations/messages/leads/
top intents/lead funnel).

## 5. Retrieval strategy

Intent router (deterministic keyword sets: GREETING/FEES/ADMISSION_FEE/SCHEDULE/
CLASSES/RECOMMEND/CONTACT/TRIAL/POLICY/OFFER/LEAD/THANKS/OTHER) selects the minimal
tool subset (e.g. FEES → packages + admission fee + offers; POLICY → category
policies) plus a top-3 keyword search across FAQs/policies/offers/classes
(field-weighted token scoring, stop-word filtered, forced branch scope). Only
`active` rows; offers additionally date-valid. Because retrieval runs per message
against live tables, an admin edit (₹2500 → ₹2700) is reflected on the very next
chat turn.

## 6. Tool design

Pure functions on `(tenant, branchId)`, returning small string maps (no internal
IDs except class ids for schedule joins — never exposed to visitors). The LLM
provider receives the same FactPack as JSON context with temperature 0.2, 300
tokens, 60-word cap, and grounding rules; rule-based provider uses templates with
identical facts. Full function-calling (model-driven tool selection) is the
documented next step; the port already supports it.

## 7. Security model

- Branch isolation server-side (`AiBranchContext` + per-request branch threading +
  conversation branch-match/ownership checks; no `branchId` query params on public GETs).
- Rate limiting (30 req/min/IP sliding window on message/recommend/lead).
- Sanitization (HTML strip, 1000-char cap), XSS-safe rendering (React text nodes only).
- Prompt-injection screening (marker list → canned refusal + audit, no state change).
- PII protection (leads table only; audit stores hashes/flags, never bodies).
- Conversation ownership (server-issued visitor UUID, localStorage persisted, mismatch → 403).
- Audit logging (`CHAT_TURN`, `LEAD_CAPTURED`, `RATE_LIMITED`, `PROMPT_INJECTION_BLOCKED`).
- Secrecy (system rules server-side only; LLM fail-closed fallback; no prompt/ID/key disclosure).

## 8. Lead capture flow

Signal (LEAD intent or fee/trial/recommend + interest words) → `leadPrompt=true` →
widget shows callback CTA → mini-form (all spec fields; phone required+regex,
email optional+regex, student-or-parent name required) → `POST /leads` → `NEW` row
+ conversation flagged → server confirmation message. Staff triage in
`/admin/ai/leads` (status pipeline + analytics funnel).

## 9. Multi-branch strategy

Branch = data partition, never code: resolution by id or slug
(`AiBranchContext.slugify`), widget switcher from `GET /branches`, admin screens
take a branch parameter, unknown-info fallback reads the branch's own
`contact_phone`/`unknown_info_template` settings (no hardcoded numbers anywhere —
verified by grep). Onboarding = insert branch row + knowledge rows (or extend the
migration seed pattern); Whitefield slug is only the widget default.

## 10. Admin screens (Phase 1+6)

`/admin/ai` (analytics: conversations/messages/leads, top intents, funnel) ·
`/admin/ai/knowledge` (7 tabbed CRUD editors incl. admission fee via Settings) ·
`/admin/ai/leads` (inbox + status pipeline). Sidebar "AI Assistant" (Owner/Developer).

## 11. Future roadmap

V2: model-driven function calling over the existing tools; pg_trgm vector/full-text
upgrade of `searchKnowledgeBase`; conversation summarization for long threads;
lead deduplication + studio-notification webhooks; WhatsApp channel reusing the
same orchestrator; per-branch tone settings; A/B provider routing with analytics
feedback; multilingual replies (Kannada/Hindi) grounded in the same facts.

## 12. Validation

- Backend: `mvn compile` ✅ (online; offline repo lacks actuator jar).
- Frontend: `npm run build` ✅ (`/admin/ai*` routes generated, 25/25 pages).
- Live pilot checklist: Whitefield chat turn for each intent, ₹-edit immediacy test,
  cross-branch leakage test (Whitefield answers contain no other branch data),
  lead round-trip, rate-limit/injection probes, iOS + Chrome Mobile widget pass.
