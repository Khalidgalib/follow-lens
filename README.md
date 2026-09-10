# FollowLens

A mobile app (iOS + Android) that finds the Instagram accounts **you follow that don't follow you
back** — working **100% offline** from your own Instagram data export.

- No login, no password, no account, no network access in v1.
- Built with **Kotlin Multiplatform + Compose Multiplatform**.
- Designed to grow into an online, subscription social-media-manager product later, reusing the
  same core logic on a Spring Boot backend.

## Repository layout

| Module | What it is |
|---|---|
| `core-diff/` | Pure Kotlin. Parses the Instagram export JSON and computes non-followers / fans / mutuals / deltas. Zero platform dependencies, fully unit-tested. Reused verbatim by the future backend. |
| `data/` | SQLDelight (SQLite) local store: snapshot history + whitelist. |
| `app/` | Compose Multiplatform UI (Android + iOS entry points). |
| `docs/` | `PROJECT_PLAN.md` (canonical plan) and `FollowLens-Plan.pdf` (portable copy). |
| `CLAUDE.md` | Context + locked decisions, auto-loaded by Claude Code. |

## Status

Scaffold + `core-diff` parser/diff engine and tests are in place. See `CLAUDE.md` → "Build order".

## Getting started (once toolchain is installed)

Requires JDK 21, Android Studio + the Kotlin Multiplatform plugin, and Xcode (for iOS).

```bash
./gradlew :core-diff:allTests      # run the diff-engine test suite
./gradlew :app:assembleDebug       # build the Android app
```

The Gradle wrapper is not committed yet — run `gradle wrapper` once (or let Android Studio
generate it on first import).

## How a user gets their data

Instagram app → Settings → Accounts Center → Your information and permissions → Download your
information → "Some of your information" → **Followers and following** → format **JSON**. Instagram
emails a download link (minutes to ~a day). The app imports the `.zip` (or the
`followers_1.json` + `following.json` inside it).
