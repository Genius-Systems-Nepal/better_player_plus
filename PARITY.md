# better_player → better_player_plus parity checklist

GSN fork comparison for Nettv cutover. Source of truth for missing features that must land in this package before `player_package` switches dependencies.

**Build plan:** [`docs/MIGRATION_PLAN.md`](docs/MIGRATION_PLAN.md)

## Package identity

| | Old (`player_package/betterplayer`) | This package |
|--|--|--|
| Pub name | `better_player` 1.3.1 | `better_player_plus` 1.1.5 |
| Android engine | ExoPlayer 2.17.1 | Media3 ExoPlayer 1.6.1 (Quanteec-compatible) |
| iOS | ObjC + Swift VuDRM | Swift |

## Android gaps (must close)

| Gap | Status | Files |
|--|--|--|
| PiP permission: `checkPictureInPicturePermission` / `openPictureInPicturePermissionSettings` | Ported | `BetterPlayerPlugin.kt`, Dart video_player layer |
| Media3 IMA ads (real loader, ad view, events, `isAdPlaying`, `disposeAdView`) | Ported | `BetterPlayer.kt`, `build.gradle.kts` |
| Quanteec P2P (Media3) | **Ported** — `quanteecMedia3Exoplayer:1.8.33` + `quanteecPlugin:1.8.33`; `quanteecConfig` on `create` wires `QuanteecMedia3ExoCore`, data source factory, bandwidth meter, and `setPlayer` on each source load. Falls back to direct CDN if setup fails. | `BetterPlayer.kt`, `BetterPlayerPlugin.kt`, `build.gradle.kts` |
| PlaybackException `errorCode` in event details | Ported | `BetterPlayer.kt` |
| RTMP (`extension-rtmp`) | **Dropped** — no RTMP URLs in Nettv / `player_package`. Re-add `media3-datasource-rtmp` only if needed. | — |

## iOS gaps ported from GSN better_player

| Gap | Status | Files |
|--|--|--|
| VuDRM FairPlay (inline base64 cert, CKC JSON variants, skd content id) | Ported | `BetterPlayerVuDrmAssetsLoaderDelegate.swift` |
| File URI handling (`file:`, `file://`, absolute paths) | Ported | `SwiftBetterPlayerPlugin.swift` |
| Ads timeline APIs (`disposeAdView`, `isAdPlaying`, `contentDuration`, `contentPosition`) | Already in plus | `BetterPlayer.swift`, `SwiftBetterPlayerPlugin.swift` |
| Quanteec P2P | Already in plus | `QuanteecHelper.swift`, `QuanteecIOS.podspec` (v1.0.26 xcframeworks from GitHub; iOS 13) |

Remaining iOS work is **regression QA** (FairPlay/VuDRM, IMA, PiP, nerd stats) — see [`docs/IOS_REGRESSION_QA.md`](docs/IOS_REGRESSION_QA.md).

## Dart API gaps

| API | Status |
|--|--|
| `VideoPlayerController.checkPipPermission` | Ported |
| `VideoPlayerController.openPipPermissionSettings` | Ported |
| `VideoPlayerValue.errorCode` (TRP) | Ported — set from PlatformException.details |
| DASH ASMS parsing (`BetterPlayerDashUtils`) | Ported | `lib/src/dash/better_player_dash_utils.dart`, `better_player_asms_utils.dart` |

## Cutover gate

Do not point `player_package` at this package until:

1. Device matrix in [`docs/CUTOVER_MATRIX.md`](docs/CUTOVER_MATRIX.md) and [`docs/IOS_REGRESSION_QA.md`](docs/IOS_REGRESSION_QA.md) is signed off
2. Quanteec Android: accepted degradation **or** adapter integrated

Follow full rollout steps in [`docs/MIGRATION_PLAN.md`](docs/MIGRATION_PLAN.md).

**Note:** Phase 4 QA and `betterplayer` submodule removal are **deferred**. The nested `player_package/betterplayer` checkout remains unchanged for rollback until explicit cleanup.
