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

- `core-diff/` — pure Kotlin. Parses the IG export (JSON **and** HTML) + does the set math. Zero
  platform deps beyond kotlinx-serialization/kotlinx-datetime/ksoup (all genuinely KMP). Heavily
  unit-tested. **This is the seam**: identical code runs on-device now and server-side later.
- `data/` — SQLDelight (SQLite) local store: snapshot history + whitelist.
- `app/` — Compose Multiplatform UI (androidMain / iosMain / desktopMain entry points).
- `iosApp/` — the actual Xcode wrapper project (SwiftUI host embedding the KMP `ComposeApp`
  framework via `ComposeUIViewController`). Needed because Compose Multiplatform's iOS target only
  produces a framework, not an app — Xcode still has to build/sign/run it. See
  `iosApp/iosApp.xcodeproj`. Building it needs `xcodebuild -target iosApp -sdk iphonesimulator
  -arch arm64 ONLY_ACTIVE_ARCH=YES SYMROOT=...` (plain `-scheme` reliably reports "no destinations"
  on this machine's Xcode/simulator-runtime combo — don't waste time re-diagnosing that).
- `backend/` — future Spring Boot service. Does not exist yet.

## Instagram export format (what `core-diff` parses)

User gets it from: Instagram → Settings → Accounts Center → Your information and permissions →
Download your information → **Followers and following** → format **JSON or HTML** (both are
accepted now — `ExportParser` auto-detects from content, not file extension) → **date range: All
time** (anything shorter silently truncates the file to that window — this caused a real "56
followers instead of 619" bug that had nothing to do with parsing). Link arrives by email (minutes
to ~a day).

Inside the ZIP, `connections/followers_and_following/`:
- `followers_1.json` (`_2`, ...) / `followers_1.html` — top-level JSON array, or HTML with no
  further per-file split observed in practice
- `following.json` / `following.html` — JSON object, key `relationships_following` → array

**Two confirmed real-world format quirks** (found by testing against actual exports, not just
Instagram's own docs — don't trust the "obvious" shape without a real sample):
- JSON: `followers_1.json` entries have `string_list_data[0].value` = username. `following.json`
  entries instead **omit `value` entirely**, put the username in the entry's own `title`, and use
  an `href` shaped `.../_u/USERNAME` rather than `.../USERNAME`. `ExportParser.collectEntries` tries
  `value` → `title` → parsed-`href` in that order.
- HTML: `following.html` renders each entry as username / blank line / the `_u/`-prefixed URL /
  a date (`"Sep 17, 2026 11:21 am"`); `followers_N.html` renders only username / date — **no
  visible URL line at all** (the username is still an `<a href>`, just never shown as text). The
  parser handles both with one code path: it selects every `<a href*=instagram.com]` in the
  document (structure-agnostic — doesn't care about div classes/nesting) and reads the username
  from the `href` attribute, never from visible text or line position. Timestamps are best-effort
  (regex-matched against a few ancestor levels of each anchor) and fall back to `null` rather than
  failing the import.

Entry (JSON, followers shape): `{ "string_list_data": [ { "value": "USERNAME", "timestamp": 1700000000, "href": "..." } ] }`

Core algorithm: `notFollowingBack = following − followers` (case-insensitive), then apply whitelist.

## Build order

1. ✅ Repo scaffold.
2. ✅ `core-diff` — parser (JSON + HTML) + diff engine + full test suite.
3. ✅ `data` — SQLDelight schema + repositories.
4. ✅ `app` — import flow (file upload, not paste) → dashboard → non-follower list → fans →
   history → settings. All five tabs exist.
5. ✅ Material 3 dark/brass theme + dashboard trend chart (hand-drawn Canvas, no charting library).
   No separate Figma pass happened — the theme was built directly from a visual-direction mockup.
6. Partially done: `instagram://user?username=` deep links **are** wired up (open-profile action on
   each account row). Still missing: onboarding, CSV export.
7. Store assets, privacy policy page, TestFlight / Play internal builds.
8. Public submission.

## Guardrails

- Never add code that logs into Instagram, scrapes it, or uses a private API.
- Never add `android.permission.INTERNET` (or iOS networking) to the v1 app. File picking
  (FileKit) does not need it — Android's Storage Access Framework picker needs no manifest
  permission at all.
- Keep `core-diff` free of platform/framework imports so the backend can reuse it. Third-party libs
  are fine there as long as they're genuinely Kotlin Multiplatform with matching targets (ksoup is;
  plain jsoup is not).
- **Dependency versions are pinned below what's "latest," on purpose.** This project's Kotlin
  version is 2.1.0. Newer releases of both `ksoup` and `FileKit` are compiled with newer Kotlin
  compilers and fail with "Module was compiled with an incompatible version of Kotlin" — Kotlin
  metadata is forward- but not backward-compatible. Current pins: `ksoup = 0.2.4` (0.2.6 needs
  Kotlin 2.3+), `filekit = 0.8.8` (0.10.0+ needs 2.2+, and also renamed the artifact from
  `filekit-compose` to `filekit-dialogs-compose` with a different API — don't blindly follow
  FileKit's current docs, they document the newer artifact/API). Before bumping either dependency,
  bump the project's Kotlin version first and re-verify the whole toolchain (this project has a
  history of toolchain fragility — see git log around the AGP/JDK-21 fix-forward).

## Session handoff (2026-09-18) — delete this section once it's stale

Just landed: the file-upload import feature (commit `e44bdd7`, on branch
`feat/v1-build-fixes-and-import-ui`). `ImportScreen` now has "Upload Following" / "Upload
Followers" buttons (FileKit picker) instead of paste boxes; `ExportParser` auto-detects and parses
either JSON or HTML. All three targets (desktop/iOS-sim/Android) compile clean and
`:core-diff:jvmTest` passes 16/16 including new HTML-format tests. Not yet done: a full manual
walkthrough on a real device/simulator with the user's actual real export files.

**In progress right now**: verifying the real HTML export end-to-end on the iOS Simulator
(`iosApp/`, already built and installed there — see the Xcode build command above). The user just
confirmed they successfully dragged their real `following.html` and `followers_1.html` onto the
Simulator window and saved them to Files → On My iPhone → Downloads. **Next step**: tap "Upload
Following" in the running app, navigate to that Downloads folder, pick `following.html`; repeat for
"Upload Followers" with `followers_1.html`; tap Analyze; confirm the Dashboard numbers look right.

Two things already confirmed true about this user's real data (useful context, not a bug to
re-investigate): their first followers export (56 entries) was date-range-limited, not "All time" —
they re-exported with All time selected, which is the `followers_1.html` now being tested. Real
counts should be roughly Following ≈513, Followers ≈619 (both from earlier JSON/text inspection
this session) — treat a result in that neighborhood as correct, not exact, since export snapshots
drift from live Instagram state.
