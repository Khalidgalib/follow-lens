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

## Brand: Galivo (parent company)

**Galivo** is a separate umbrella company identity, not FollowLens itself — FollowLens is the first
of several apps planned to launch under it (locked decision, the way Microsoft has many products).
The **canonical Galivo logo** is an abstract "G" mark (a thin ring + inward crossbar) on a soft white
glow-circle badge, in Galivo's own indigo/cyan palette — that's the actual brand asset, explored and
finalized in a Claude Artifact this session ("Galivo Logo Concepts"), not defined anywhere in this
repo. `app/.../ui/GalivoMark.kt` holds only an **app-specific derivative** of that mark's geometry
(same "G" shape, recolored to a vivid gold + soft glow) sized for FollowLens's own `ScreenHeader` —
FollowLens intentionally keeps its own warm-gold identity distinct from Galivo's own indigo/cyan, the
same way Word/Excel/PowerPoint don't all turn Microsoft-blue. Don't "fix" the mark's color back
toward indigo inside this app without asking — that was tried and explicitly reverted this session.

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

## Session handoff (2026-09-18, final) — delete this section once it's stale

Everything below is committed on `feat/v1-build-fixes-and-import-ui`, working tree clean, HEAD at
`0afefc3`. Since the file-upload-import handoff (commit `e44bdd7`), in order:

- Profile-link icon opens the web (snackbar fallback instead of silently doing nothing).
- Theme-wide button ripple via `LocalIndication` **+** `LocalRippleConfiguration` together —
  Material3 components (`Button`/`IconButton`/`Surface`'s clickable) ignore plain `LocalIndication`
  and build their own ripple from `LocalRippleConfiguration`; only `selectable`/other plain-Foundation
  widgets read `LocalIndication`. Needed both to cover every button in the app.
- A cross-platform `ScrollPositionIndicator` (custom-drawn, not the desktop-only
  `VerticalScrollbar`) on all five tabs.
- Dashboard is the permanent home screen — no more separate full-screen import gate. With no data
  it shows zeros + an inline "Get started" `UploadPanel` (in `UploadPanel.kt`, formerly
  `ImportScreen.kt`); with data, a "Last import" card with an "Update data" button reveals the same
  panel inline for re-import.
- `ExportParser.detectKind` flags a file dropped in the wrong upload slot (Following file in the
  Followers slot, etc.) — fixed once already, since its real-HTML-header check initially didn't
  match the actual export format (see the export-format section above).
- Dashboard's stat tiles are clickable: Following/Followers open new drill-down `AccountListScreen`s
  with a back button (returns to Dashboard), Not Back jumps to the existing LIST tab, Fans is a
  fourth tile added this session that jumps to the existing FANS tab.
- The Galivo brand mark sits in `ScreenHeader` in a vivid gold with a soft glow (see Brand section
  above) — `FollowLensColors.accent`/`accentStrong` were also brightened app-wide in the same pass.

`:core-diff:jvmTest` passes; all three targets (desktop/iOS-sim/Android) compile clean.

**App-distribution logistics explored this session** (no code involved, just process — worth keeping
so it isn't re-discovered from scratch): to let friends sideload-test the Android build outside the
Play Store, `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` is enough
(debug signing is fine for this, unrelated to a future release signing key). Two real gotchas hit
while sharing that file:
- **Google Drive refuses to generate a share link for a raw `.apk`** ("Sorry, sharing is unavailable
  at this time") — a blanket executable-file restriction, not specific to this file. Fix: zip it
  first: Drive can't see the extension inside a zip.
- **WhatsApp's in-app browser can't download zip/apk files opened by tapping a link inside a
  chat** — hangs silently on Android, shows "unsupported file" on iPhone — even though the same link
  works fine in a real browser or another app's in-app browser (confirmed via Instagram). The link
  itself is fine; tell recipients to long-press the link → "Open in Safari/Chrome" rather than
  tapping it directly. WeTransfer's dedicated download page avoids this problem entirely and doesn't
  need the zip step — use that instead if this comes up again.

**Not yet done**: the real app icon (iOS `AppIcon` catalog / Android adaptive icon) using the Galivo
mark — deliberately deferred as separate, bigger work with its own platform safe-zone rules.
Onboarding and CSV export (build order item 6) haven't been started. No store-submission prep yet
(item 7) — the user is currently leaning toward Google Play first (one-time $25 fee vs. Apple's
$99/year), but hasn't enrolled in either developer program yet.
