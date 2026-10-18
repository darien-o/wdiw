# Contributing

## Ways to help

- **Test a provider on your TV.** Most valuable. See [`docs/adding-a-provider.md`](docs/adding-a-provider.md).
- **Report a wrong result.** Open an issue with the title, your country, and what the app showed.
- **Translate.** Copy `android-tv/app/src/main/res/values/strings.xml` into `values-<language>/` and translate it.
- **Code.** Read [`docs/architecture.md`](docs/architecture.md) first.

## Ground rules

- No scraping and no unofficial APIs. Availability comes from TMDB, platform IDs from Wikidata.
- Nothing that streams, downloads or bypasses a provider's own app.
- Keep the APK small and the start fast. New dependencies need a reason; CI fails above 6 MB.
- The app stores nothing about the viewer and has no analytics. Keep it that way.

## Before opening a pull request

```sh
# proxy and registry
cd proxy && pnpm run check:registry && pnpm run typecheck && pnpm test

# TV app
cd android-tv && ./gradlew testDebugUnitTest assembleDebug
```

Logic that decides something (which link to try, which country, which version is newer) lives in `android-tv/app/src/main/kotlin/.../domain/` as plain Kotlin and has unit tests. Add tests there when you change it.

User-visible text goes in `strings.xml`, in English and Spanish.
