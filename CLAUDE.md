# FollowLens — project context for Claude Code

Auto-loaded each session. Keep it short; the full plan is `docs/PROJECT_PLAN.md`.

## What this is

A mobile app (iOS + Android) that finds Instagram accounts **you follow that don't follow you
back**, working **100% offline** from the user's own Instagram data export. Later it grows into a
subscription social-media-manager product with an online backend.

Replaces an abandoned Spring Boot scaffold that used to live in
`~/Desktop/Instagram App/Insta Follow Check` (that folder is being deleted).

## Locked decisions — do not relitigate without the user

| Topic | Decision |
|---|---|
| Framework | **Kotlin Multiplatform + Compose Multiplatform**. One language end to end. |
| Backend (future) | **Spring Boot (Kotlin)**, depends on the same `:core-diff` module. Not built yet. |
| Follow/unfollow | **Never automated.** v1 = show the non-follower list. Future = *assisted* review queue (app ranks, one tap opens the profile, human confirms). Automating follow/unfollow = account bans + store rejection; the official IG API has no such endpoint. |
| Future automation that IS allowed | Content scheduling/publishing + comment/DM inbox, **only** via the official Instagram Graph API for Business/Creator accounts. |
| v1 scope | Core + history: non-follower list, fans list, whitelist, snapshot history. Single account. |
| Monetization (v1) | **Free.** No IAP SDK, no ads — keeps the app genuinely offline. |
| Brand | **FollowLens.** Never put "Insta", "Gram", or "IG" in the name/icon — stores reject it. |
| Privacy posture | No account, no network permission in v1. Apple/Play privacy forms = "no data collected". |

## Module layout

- `core-diff/` — pure Kotlin. Parses the IG export JSON + does the set math. Zero platform deps.
  Heavily unit-tested. **This is the seam**: identical code runs on-device now and server-side later.
- `data/` — SQLDelight (SQLite) local store: snapshot history + whitelist.
- `app/` — Compose Multiplatform UI (androidMain / iosMain entry points).
- `backend/` — future Spring Boot service. Does not exist yet.

## Instagram export format (what `core-diff` parses)

User gets it from: Instagram → Settings → Accounts Center → Your information and permissions →
Download your information → "Some of your information" → **Followers and following** → format JSON.
Link arrives by email (minutes to ~a day).

Inside the ZIP, `connections/followers_and_following/`:
- `followers_1.json` (`_2`, ...) — **top-level JSON array** of entries
- `following.json` — object, key `relationships_following` → array

Entry: `{ "string_list_data": [ { "value": "USERNAME", "timestamp": 1700000000, "href": "..." } ] }`

Core algorithm: `notFollowingBack = following − followers` (case-insensitive), then apply whitelist.

## Build order

1. ✅ Repo scaffold (this).
2. `core-diff` — parser + diff engine + full test suite. (Parser/engine/tests already stubbed in.)
3. `data` — SQLDelight schema + repositories.
4. `app` — import flow → non-follower list → dashboard → fans → history → settings.
5. Figma design pass; Material 3 theme + charts (Koala Plot / compose-charts).
6. Onboarding + copy; `instagram://user?username=` deep links; CSV export.
7. Store assets, privacy policy page, TestFlight / Play internal builds.
8. Public submission.

## Guardrails

- Never add code that logs into Instagram, scrapes it, or uses a private API.
- Never add `android.permission.INTERNET` (or iOS networking) to the v1 app.
- Keep `core-diff` free of platform/framework imports so the backend can reuse it.
