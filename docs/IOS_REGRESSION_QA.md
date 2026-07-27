# iOS regression QA — better_player_plus

Run on a physical iPhone (or simulator where DRM allows) against builds that use `better_player_plus`.

## Checklist

| Scenario | Pass |
|--|--|
| Clear HLS VOD play / pause / seek | |
| FairPlay EZDRM (certificate URL path) | |
| FairPlay VuDRM / Lionsgate (`drm_token` / inline cert if used) | |
| Live stream + DVR seek (`contentDuration` / `contentPosition`) | |
| IMA / VAST pre-roll or mid-roll (`adsUrl`) — adStarted / adEnded, content resumes | |
| Quanteec when `quanteecConfig` present (iOS helper enabled) | |
| Picture-in-Picture enter / exit | |
| Nerd stats toggle + bitrate updates | |
| Audio track / quality (ASMS) selection | |
| Banner / New on Nettv inline players | |

## Notes

- Plus iOS MethodChannel already includes ads timeline APIs that old ObjC lacked.
- Compare behavior to production `better_player` only when fixing regressions.
