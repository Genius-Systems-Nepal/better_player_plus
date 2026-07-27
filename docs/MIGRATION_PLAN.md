# better_player → better_player_plus migration plan

Build plan for moving Nettv (`player_package` + app) onto `better_player_plus` without losing working features from either fork.

**Source forks**

| | `player_package/betterplayer` | `better_player_plus` |
|--|--|--|
| Pub name | `better_player` 1.3.1 | `better_player_plus` 1.1.5 |
| Android | ExoPlayer 2.17.1 | Media3 1.8.0 |
| iOS | ObjC + Swift VuDRM | Swift |

**Tracking docs**

- Feature parity checklist: [`../PARITY.md`](../PARITY.md)
- Android device matrix: [`CUTOVER_MATRIX.md`](CUTOVER_MATRIX.md)
- iOS regression QA: [`IOS_REGRESSION_QA.md`](IOS_REGRESSION_QA.md)

---

## Phase 0 — Feature registry (do not skip)

Lock every capability Nettv uses today. Anything not on this list is out of scope unless product adds it.

### Nettv-critical APIs (must work after cutover)

| Feature | Android | iOS | `player_package` consumer |
|---------|---------|-----|---------------------------|
| HLS / file / network playback | ✓ | ✓ | `PlayerHelper.playVideo` |
| Widevine DRM | ✓ | — | `DrmConfigs` |
| FairPlay EZDRM | — | ✓ | `BetterPlayerDrmConfiguration` |
| FairPlay VuDRM / Lionsgate | — | ✓ | `extraParams.drm_token`, inline cert |
| IMA / VAST ads | ✓ | ✓ | `adsUrl`, `adStarted`/`adEnded` |
| `isAdPlaying` | ✓ | ✓ | progress UI hides during ads |
| `disposeAdView` | ✓ | ✓ | ad teardown |
| Live + DVR timeline | ✓ | ✓ | `contentDuration` / `contentPosition` |
| PiP enter/exit | ✓ | ✓ | landscape player |
| PiP permission (Android) | ✓ | N/A | `checkPipPermission`, `openPipPermissionSettings` |
| Quanteec P2P | ✓ (Exo 2.x) | ✓ | `quanteecConfig` |
| ASMS quality / audio / subtitle | ✓ | ✓ | settings panel |
| DASH ASMS (`.mpd`) | ✓ | ✓ | DASH quality/subtitle discovery |
| Nerd stats + bitrate | ✓ | ✓ | debug overlay, TRP |
| TRP `errorCode` | ✓ | ✓ | `VideoPlayerValue.errorCode` |
| Cache / preCache | ✓ | ✓ | offline / prefetch |

### Intentional non-goals

| Feature | Decision |
|---------|----------|
| RTMP | **Dropped** — not used in Nettv; re-add `media3-datasource-rtmp` only if required |
| `playWhenReadyTrue/False` | **Not ported** — defined in old Dart layer but never wired on Android native |

---

## Phase 1 — Close gaps in `better_player_plus` (this repo)

Work here **before** switching `player_package` dependency.

### 1.1 Dart layer

| # | Task | Source | Target | Status |
|---|------|--------|--------|--------|
| D1 | Port DASH MPD ASMS parser | `betterplayer/lib/src/dash/better_player_dash_utils.dart` | `lib/src/dash/better_player_dash_utils.dart` | Done |
| D2 | Route ASMS parse by stream type | `betterplayer/lib/src/asms/better_player_asms_utils.dart` | `lib/src/asms/better_player_asms_utils.dart` | Done |
| D3 | Export dash utils from barrel if needed | — | `lib/better_player_plus.dart` | Optional |

**Acceptance:** `.mpd` URL populates `betterPlayerAsmsTracks`, audio tracks, subtitles.

### 1.2 Android (Media3)

| # | Task | Status | Notes |
|---|------|--------|-------|
| A1 | PiP permission check + open settings | Ported | Regression only |
| A2 | IMA ads (loader, ad view, events) | Ported | Verify real ads, not stubs |
| A3 | `errorCode` in PlatformException details | Ported | TRP regression |
| A4 | Live/DVR `contentDuration` / `contentPosition` | Ported | Live TV seek bar |
| A5 | Quanteec P2P on Media3 1.8 | **Blocked** | See [Quanteec strategy](#quanteec-strategy) |
| A6 | RTMP | Dropped | Product sign-off |

### 1.3 iOS (Swift)

| # | Task | Source | Target | Status |
|---|------|--------|--------|--------|
| I1 | VuDRM / FairPlay delegate (inline cert, CKC variants) | `betterplayer/.../BetterPlayerVuDrmAssetsLoaderDelegate.swift` | `ios/Classes/BetterPlayerVuDrmAssetsLoaderDelegate.swift` | Done (URLSession, no Alamofire) |
| I2 | File URI handling (`file:`, `file://`, absolute paths) | `betterplayer/.../BetterPlayerPlugin.m` | `ios/Classes/SwiftBetterPlayerPlugin.swift` | Done |
| I3 | Ads timeline MethodChannel | plus native | keep | Already in plus |
| I4 | Quanteec P2P | plus native | keep | Already in plus |
| I5 | FairPlay / IMA / PiP / nerd stats QA | — | device matrix | Pending QA |

**Acceptance:** Lionsgate VuDRM + EZDRM + IMA + live DVR on physical iPhone.

---

## Phase 2 — Switch `player_package`

Location: `submodules/player_package`

| # | Task | Files |
|---|------|-------|
| P1 | Change dependency to `better_player_plus` | `pubspec.yaml` |
| P2 | Replace imports `better_player` → `better_player_plus` | `lib/**/*.dart` |
| P3 | Use native iOS ad/timeline APIs (remove Android-only guards) | `player_controller_view_model.dart` |
| P4 | Compile + unit smoke test | `player_package` |

```yaml
# pubspec.yaml target
better_player_plus:
  path: ../better_player_plus
```

Import map:

```
package:better_player/better_player.dart          → package:better_player_plus/better_player_plus.dart
package:better_player/src/video_player/...        → package:better_player_plus/src/video_player/...
```

---

## Phase 3 — Switch main app

| # | Task |
|---|------|
| M1 | `rg "package:better_player"` — replace all with `better_player_plus` |
| M2 | Known files: `landscape_player_overlays.dart`, `landscape_player_settings_options.dart`, `banner_video_player_page.dart`, legacy `screens/widget/**` |
| M3 | Ensure root app resolves plus via `player_package` or explicit path dep |
| M4 | No player UX changes in this phase — imports only |

---

## Current policy (Nettv)

- **Phase 4 (device QA)** — **deferred**. Do not block ongoing work on the verification matrix; run before production sign-off.
- **`player_package/betterplayer` submodule** — **keep as-is** (no removal, no edits). It remains in the repo for rollback reference until migration is fully complete and explicitly signed off.
- Runtime dependency is already on `better_player_plus` (Phases 2–3). The old submodule is **orphaned from pub resolution** but **not deleted**.

---

## Phase 4 — Device verification (cutover gate) — DEFERRED

### Android — [`CUTOVER_MATRIX.md`](CUTOVER_MATRIX.md)

- [ ] HLS clear + Widevine VOD
- [ ] Live + DVR seek
- [ ] IMA ads (real loader)
- [ ] Quanteec: graceful fallback if P2P unavailable
- [ ] PiP permission flow
- [ ] Banner / inline players
- [ ] Nerd stats + TRP `errorCode`
- [ ] ASMS quality / audio / subtitle (HLS + DASH)

### iOS — [`IOS_REGRESSION_QA.md`](IOS_REGRESSION_QA.md)

- [ ] Clear HLS VOD
- [ ] FairPlay EZDRM
- [ ] FairPlay VuDRM (inline cert + `drm_token`)
- [ ] Live + DVR native timeline
- [ ] IMA ads
- [ ] Quanteec
- [ ] PiP
- [ ] Nerd stats + bitrate
- [ ] ASMS selection
- [ ] Local `file://` playback

**Do not remove nested `betterplayer` submodule until all P0 rows pass and Phase 5 is explicitly approved.**

---

## Phase 5 — Decommission — DEFERRED

| # | Task |
|---|------|
| R1 | Tag last `betterplayer` commit for rollback |
| R2 | Remove `player_package/betterplayer` submodule (**deferred** — keep until post-completion cleanup) |
| R3 | Mark [`PARITY.md`](../PARITY.md) complete |
| R4 | Staged prod rollout with soak period |

---

## Quanteec strategy

Quanteec Media3 adapter for **1.8.0 is not available** (supported: Exo 2.x / Media3 ≤ 1.1.1).

| Option | Pros | Cons |
|--------|------|------|
| **A — Wait for Quanteec** | Full P2P on Media3 1.8 | Timeline unknown |
| **B — Pin Media3 ≤ 1.1.1** | P2P works now | Loses 1.8 fixes; rework later |
| **C — Ship without Android P2P** | Unblocks cutover | Android CDN-only until adapter ships |

**Current behavior in plus:** `quanteecConfig` logs warning; playback continues without P2P.

**Recommendation:** Option C for cutover; track Quanteec adapter; iOS Quanteec remains enabled.

---

## Implementation checklist (better_player_plus repo)

Use this as the sprint board for Phase 1:

```
[x] D1  Port BetterPlayerDashUtils
[x] D2  Fix BetterPlayerAsmsUtils.parse routing
[x] I1  Merge VuDRM delegate (URLSession)
[x] I2  Port file URI resolution in SwiftBetterPlayerPlugin
[ ] A5  Quanteec — decision + integration when adapter ready
[ ] QA  Android CUTOVER_MATRIX signed off (deferred)
[ ] QA  iOS IOS_REGRESSION_QA signed off (deferred)
[x] P1  player_package pubspec switch
[x] P2  player_package import migration
[x] P3  iOS ad/timeline native APIs (Android-only guards removed)
[x] P4  player_package analyze clean (no errors)
[x] M1  App import migration
[ ] R2  Remove betterplayer submodule (deferred — keep for rollback)
```

---

## Risk register

| Risk | Mitigation |
|------|------------|
| VuDRM regression on iOS | Port full GSN delegate; test Lionsgate + EZDRM separately |
| DASH ASMS broken | D1/D2 in this repo before cutover |
| Android Quanteec missing | Document degradation; iOS P2P still works |
| Dual import confusion | grep entire monorepo before release |
| minSdk 24 / iOS 13 | Confirm device support matrix |

---

## Definition of done

Migration is complete when:

1. `player_package` depends only on `better_player_plus`
2. No `package:better_player` imports in app or `player_package`
3. All P0 device matrix scenarios pass on Android and iOS
4. TRP, ads, DRM, live, PiP, ASMS, nerd stats match or exceed current production
5. `betterplayer` submodule removed with rollback tag documented (**deferred** — submodule kept in repo until then)
