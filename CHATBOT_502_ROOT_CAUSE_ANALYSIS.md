# Chatbot 502 — Root Cause Analysis

Date: 2026-09-11 · Symptom: widget loads, greeting shows, `POST /api/chat/public/message`
→ 200 then → **502** (e.g. "What classes do you offer?"). Reviewed: `AiPublicChatController`
(public chat API), `Dance7AiPort`, `RuleBasedAiProvider`, `HttpLlmAiProvider`,
`AiRetrievalService`, `AiBranchContext`, `AiChatService`, `application.yml`, security filters.

## Root cause (ranked)

1. **Unbounded upstream LLM call (primary suspect).** `HttpLlmAiProvider` built its
   `RestClient` with **no connect/read timeouts**. If `DANCE7_AI_ENABLED=true` (+ key)
   is set in the Railway environment, every chat turn blocks on the external LLM;
   a slow/hung upstream holds the request until the Vercel→Railway edge gateway
   gives up → **502**. The old catch block only handled *failures*, never *hangs*.
   Intermittence (200 then 502) matches upstream latency variance.
2. **Cold-database hang (contributing).** Hikari defaults (30 s connection timeout)
   against Neon scale-to-zero can stall a turn past gateway timeouts → edge 502.
   A backend 500 would pass through the rewrite as 500, so a true 502 points at
   timeout/crash at the edge — consistent with (1) and (2).
3. **Ruled out.** Branch resolution (404-mapped, tested) · missing seed data
   (degrades to unknown-info reply, covered) · NPE in retrieval (all Optional-guarded;
   empty tables → graceful reply) · conversation ownership (now 403-mapped).
4. **Real routing bugs found by new tests (fixed).** "What classes do you offer?"
   misrouted to OFFER/FEES ("offer" matched discount patterns); "Is there a trial
   class?" lost to CLASSES ("class" substring); "I want to join, please call me"
   lost to CONTACT ("call"). These produced wrong-but-200 answers, not 502s.

## Stack-trace location

No single throw site: the 502 is generated at the **edge gateway** (no backend
response in time), so there is no backend stack trace. The code sites that could
hold a request open were `HttpLlmAiProvider.generate` (unbounded `RestClient`
call) and any JPA turn during a Neon cold start (unbounded pool wait).

## Files modified

- `ai/service/HttpLlmAiProvider.java` — bounded `ClientHttpRequestFactory`
  (connect 5 s / read 15 s, env-tunable), opt-in flag `dance7.ai.enabled`
  (empty-key can no longer silently activate it), failure logging without secrets.
- `ai/service/AiChatService.java` — full-turn guard: client errors (400/404/403/429)
  still propagate; **any other `Throwable` from retrieval/provider/persist returns
  HTTP 200** with the graceful fallback (branch name + DB-driven phone; Whitefield
  renders exactly: "I'm having trouble retrieving information right now. Please try
  again shortly or contact the Whitefield branch at 9731067867."); empty-LLM-output
  safety net re-verbalizes via rule-based; storage outage degrades without a
  conversation id; SLF4J logging (intent/provider/latency/failures).
- `ai/service/AiRetrievalService.java` — debug logging on empty knowledge hits.
- `config/ApiExceptionHandler` (existing `ApiExceptionHandler.java`) — added
  `SecurityException → 403` and `RateLimitedException → 429` mappings (previously
  unmapped → 500).
- `resources/application.yml` — Hikari fail-fast (`connection-timeout` 10 s,
  `validation-timeout` 5 s) + `dance7.ai.*` defaults.
- `src/test/.../ai/AiChatLogicTest.java` — **new**: 8 unit tests (intent matrix,
  age/level extraction, lead signals, fallback sentence, scoring, guardrails,
  branch helpers). No Spring context needed.
- Intent router fixes: OFFER/FEES patterns no longer match the verb "offer";
  TRIAL and LEAD ordered above CLASSES/CONTACT.

## Validation results

- `mvn test` ✅ — `Tests run: 8, Failures: 0, Errors: 0` (first run caught 4 real
  routing bugs, all fixed and green).
- `mvn compile` ✅ — BUILD SUCCESS.
- Fallback contract: recoverable failures → HTTP 200 + approved sentence;
  contract errors → 400/404/403/429; nothing in the chat path can hold a request
  past ~25 s (LLM 20 s + DB 10 s worst case), inside gateway budgets.

## Remaining ops checks (Railway/Vercel, not code)

- Confirm `DANCE7_AI_ENABLED` is `false`/unset unless an LLM key is intentionally
  configured; otherwise traffic stays on the deterministic provider.
- Watch Railway logs for `Dance7 chat turn failed` / `CHAT_TURN_FAILED` audit rows
  to confirm the fallback path vs. upstream latency.
- If 502s persist after deploy, suspect Neon cold starts — consider minimum compute
  or a lightweight warmer; the app now fails fast (10 s) instead of hanging.

## Commit / push

- Commit: "Harden Dance7 AI chat: timeouts, graceful fallback, intent fixes" (see log).
- Push: `origin/main` (manojacharya3/Dance-7) — synced.
