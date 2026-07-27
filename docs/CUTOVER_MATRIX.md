# Cutover verification matrix — better_player_plus

Must pass before **production sign-off** and before removing `submodules/player_package/betterplayer`.

> **Status:** Phase 4 device QA is **deferred**. The `betterplayer` nested submodule is **retained** for rollback until post-completion cleanup (Phase 5).

## Android

| Scenario | Pass |
|--|--|
| HLS clear + Widevine DRM VOD | |
| Live + DVR (`contentDuration` / `contentPosition`) | |
| IMA ads (`adsUrl`) — real loader, not stub events | |
| Quanteec: config logs gate warning; playback still works without P2P | |
| PiP permission check → settings when denied → enable PiP when allowed | |
| Banner / New on Nettv inline players | |
| Nerd stats / bitrate / TRP receives `PlaybackException.errorCode` details | |
| Quality / audio / subtitle via ASMS | |

## iOS

See [IOS_REGRESSION_QA.md](IOS_REGRESSION_QA.md).

## Code cutover status

- [x] `player_package` depends on `../better_player_plus`
- [x] `player_package` imports use `package:better_player_plus/...`
- [x] App + package imports use `package:better_player_plus/...`
- [x] Phase 1 parity items in [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md) complete
- [ ] Device matrix above signed off (**deferred**)
- [ ] Remove nested `betterplayer` submodule (**deferred** — keep in repo for now)
