# Miniapp Learning Flow Implementation Plan

> Branch: `feat/soft-exam-v1`
>
> Source of truth: `docs/specs/soft-exam-v1.md` and `docs/implementation-plan.md`

## Goal

Deliver Phase 7 as a production-oriented uni-app + Vue 3 + TypeScript + Pinia WeChat mini program without fake business data. The miniapp must be API-driven, degrade explicitly when a server capability is not yet available, and keep the Learning Engine on the server.

## Task 1 — Client foundation

**Tests first**
- request error normalization
- API capability mapping
- daily target/profile validation helpers

**Implementation**
- `miniapp/` uni-app Vue 3 + TypeScript scaffold
- app manifest, pages config, global theme
- HTTP client with request-id, auth token, timeout and normalized errors
- Pinia session/app stores
- typed API contracts
- Node-based unit test runner for pure TypeScript modules
- miniapp CI

**Done when**
- the project has deterministic scripts for test/typecheck/build
- no credentials or fake business records are committed

## Task 2 — Onboarding and assessment

**Tests first**
- onboarding validation
- assessment answer-state transitions

**Implementation**
- welcome/login page
- exam profile setup
- optional assessment intro
- assessment question flow, confidence capture and submit result
- explicit unavailable/error states

## Task 3 — Home and today learning

**Tests first**
- task progress aggregation
- task route resolution

**Implementation**
- learning home dashboard
- today plan page
- task cards for wrong review / weak knowledge / new knowledge / real exam
- refresh, loading, empty and error states
- no invented mastery percentage when evidence is absent

## Task 4 — Course and video learning

**Tests first**
- video progress threshold and reporting policy
- duration formatting

**Implementation**
- course list/detail
- video detail/player
- transcript area
- progress restore/report
- completion feedback at the server-defined >=85% threshold

## Task 5 — Practice, analysis, wrong questions and favorites

**Tests first**
- objective-answer selection rules
- confidence/source payload mapping
- favorite persistence behavior

**Implementation**
- practice entry and question runner
- single/multiple choice interaction
- submit/result/analysis view
- wrong-question list and review entry
- favorites page; local persistence is clearly scoped to the device until a server favorites API exists

## Task 6 — AI entry, mine and weekly report

**Tests first**
- AI capability degradation
- weekly summary formatting

**Implementation**
- AI explain/chat entry
- graceful unavailable state when Phase 5 server API is absent
- profile/settings page
- weekly report with assessed/pending mastery semantics
- privacy-friendly local session reset

## Task 7 — Release hardening

**Tests first**
- critical navigation/capability scenarios

**Implementation**
- common skeleton/empty/error components
- accessibility and touch-target review
- README with WeChat DevTools/HBuilderX and CLI instructions
- environment template
- CI full miniapp checks
- mark Phase 7 complete only after all focused tests pass and self-review is done

## Constraints

- Do not hard-code real exam/user/question data into business flows.
- Do not calculate mastery in the miniapp.
- Do not label AI-generated content as real exam content.
- Do not hide missing backend capabilities behind fake success states.
- WeChat secrets, API keys and production URLs must remain environment/config values.
