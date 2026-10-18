# Architecture

```
 TV app ──► proxy (Cloudflare Worker) ──► TMDB      what the title is, where it is in a country
   │                │                └──► Wikidata  the title's ID on each platform
   │                └── serves registry/providers.json
   └──► installed streaming apps (opened with an intent)
```

## The three parts

**Registry (`registry/`).** Data, not code. For each provider: its TMDB provider IDs, its Android package names, and the links that open a title or a search in its app. `idSources` says which Wikidata property holds each platform's title ID. The schema is in `providers.schema.json`; CI rejects an invalid file.

**Proxy (`proxy/`).** One Worker with five read-only endpoints:

| Endpoint | Returns |
|---|---|
| `GET /search?q=&lang=` | Movies and series matching the text |
| `GET /popular?country=&lang=` | Popular titles that can be streamed in the country |
| `GET /title/{movie\|tv}/{id}?country=&lang=` | Details, the country's offers, and platform IDs |
| `GET /registry` | The registry, with an ETag |
| `GET /geo` | The country the request came from |

It keeps the TMDB key off the devices, caches at the edge (search 1 h, titles 6 h, platform IDs 7 d) and rate-limits per IP. The edge cache only works when the Worker is served from a custom domain: on a `*.workers.dev` address Cloudflare ignores cache operations, so every request goes to TMDB and Wikidata. It stores nothing about users. `country` falls back to the country Cloudflare detects.

**TV app (`android-tv/`).** One module, four packages:

- `data/` talks to the proxy and holds the registry and settings.
- `domain/` is plain Kotlin with no Android imports: the open plan, country choice, deep links, version comparison. This is where the unit tests are.
- `launch/` turns a plan into intents.
- `ui/` is Compose for TV.

Objects are wired by hand in `AppContainer`. There is no dependency injection framework, database or navigation library.

## Opening a title

`domain/OpenPlan.kt` builds an ordered list of steps for each offer, and `launch/Launcher.kt` tries them until one starts:

1. **Title link**, when the registry has one and Wikidata has the platform's ID for the title.
2. **Search link**, when the registry has one.
3. **Launch** the app.

When the provider's app is not installed there are no steps, and the screen shows a QR code to TMDB's "where to watch" page instead.

The button's label says which of these will happen ("Open in", "Find it in", "Open"), so the app never promises more than it can do.

## Updating without an app release

- **Registry.** The app loads the copy it last downloaded (or the one bundled in the APK), then fetches `/registry` in the background and keeps it if its `revision` is higher. A registry with a `schemaVersion` the app does not know is ignored, and unknown fields are skipped, so old apps keep working.
- **New titles and availability** come live from TMDB.
- **The APK itself** is updated from GitHub Releases: the app checks once per launch and, if you agree, downloads the APK into a system install session. Android verifies the signature and asks for confirmation.

## Country

A country chosen in Settings wins. Otherwise the app uses the country the proxy sees the TV in, then the last country it saw, then the device's region. The device's language is deliberately last: a TV set to English is not necessarily in the United States.

## Design

- Warm near-black background, warm white text, one amber accent used only for focus and the primary action.
- Each title's screen is tinted by the dominant colour of its poster.
- "Glass" panels are a translucent fill with a light top edge and a hairline border, drawn over an already-softened backdrop. This looks the same on every device. The backdrop itself is blurred on Android 12 and newer; older versions stretch a small image instead.
- Instrument Serif for titles and the answer, Manrope for everything else. Nothing smaller than 14sp, body text at 20sp.
- Every focusable element grows slightly and gains an amber ring. Motion is limited to focus, screen changes and the colour tint, and follows the system's "remove animations" setting.
