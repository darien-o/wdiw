# Device verification log

A link in `providers.json` may be marked `"status": "verified"` only when it has a row here.

## How to test a link

With the provider app installed and signed in on an Android TV device or emulator:

```sh
adb shell am start -a android.intent.action.VIEW \
  -d "<expanded uri>" \
  [--es <extra-key> <extra-value>] \
  <package>
```

Record what the app actually shows: the title page, playback, a search screen, its home screen, or an error.

## Results

| Provider | Package | Link tested | Device / Android | App version | Result | Date |
|---|---|---|---|---|---|---|
| HBO Max | `com.wbd.stream` 7.12.0.68 | `https://play.hbomax.com/search?q=Spirited%20Away` | HiDPT projector (Hi3751V350), Android 9 | 7.12.0.68 | Opens the app's search screen. The query is **not** filled in; the viewer has to type it. | 2026-10-04 |
| HBO Max | `com.wbd.stream` | `https://play.hbomax.com/feature/urn:hbo:feature:GXrHanAQBunUYOAEAAAB3` (Wikidata P8298) | same | 7.12.0.68 | "Content Not Available". Wikidata's HBO Max IDs are from the old platform and no longer resolve. | 2026-10-04 |
| Netflix | `com.netflix.mediaclient` | `https://www.netflix.com/title/{id}` + extra `source=30`, from the app | HiDPT projector (Hi3751V350), Android 9 | not recorded | Opens the title. Reported by the project owner after signing in. | 2026-10-04 |
| Disney+ | `com.disney.disneyplus` | `https://www.disneyplus.com/browse/entity-f6174ebf-cb92-453c-a52b-62bb3576e402` (Wikidata P13902, Toy Story) | HiDPT projector (Hi3751V350), Android 9 | 26.17.0 | Opens the title page, after the profile picker. | 2026-10-04 |
| Disney+ | `com.disney.disneyplus` | `https://www.disneyplus.com/search?q=toy%20story` | HiDPT projector (Hi3751V350), Android 9 | 26.17.0 | Opens the search screen; whether the query is filled was not visible. Not used. | 2026-10-04 |
| Prime Video | `com.amazon.amazonvideo.livingroom` | `https://app.primevideo.com/detail?gti=amzn1.dv.gti.84aa074e-d217-477f-9242-3e62c961c14b` (Wikidata P14462, Fight Club) | HiDPT projector (Hi3751V350), Android 9 | 6.24.8 | Opens the title page, even signed out. | 2026-10-04 |
| Prime Video | `com.amazon.amazonvideo.livingroom` | `https://app.primevideo.com/search?phrase=the%20boys` | HiDPT projector (Hi3751V350), Android 9 | 6.24.8 | Opens search with the query filled in and results shown. | 2026-10-04 |
| Prime Video | `com.amazon.amazonvideo.livingroom` | `detail?gti=B07QQQ52B3` and `detail?asin=B07QQQ52B3` (ASIN, Wikidata P8055) | HiDPT projector (Hi3751V350), Android 9 | 6.24.8 | Does not open the title; lands on the app's start screen. ASINs are not accepted. | 2026-10-04 |
| Paramount+ | `com.cbs.ott` | `https://www.paramountplus.com/search/?q=top%20gun`, `/movies/top-gun-maverick/` | HiDPT projector (Hi3751V350), Android 9 | 16.21.0 | Inconclusive: the app showed "technical difficulties" for both. | 2026-10-04 |

## Unconfirmed

TMDB provider IDs were checked against TMDB's own list on 2026-10-04 (`pnpm run check:provider-ids`). Package names and link formats without a row above come from documentation and community posts and have not been run on a device.

Candidates seen in community posts that are **not** in the registry because they are too uncertain:

- HBO Max title: `https://play.hbomax.com/movie/{uuid}`. The app handles `play.hbomax.com` and `play.max.com` links, but no free source has the new UUIDs.
- Netflix title, alternative to the `/title/{id}` form in the registry: `http://www.netflix.com/watch/{id}` with extra `source=30`, sent to `com.netflix.ninja/.MainActivity`
- Netflix search: `https://www.netflix.com/search?q={query}`
- Plex title: `https://watch.plex.tv/movie/{slug}` (the Wikidata key is not the slug)
