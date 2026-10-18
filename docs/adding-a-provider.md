# Adding or fixing a provider

Everything is in [`registry/providers.json`](../registry/providers.json). No app change is needed.

## 1. Find the facts

- **TMDB provider ID.** With a TMDB token: `cd proxy && TMDB_TOKEN=... pnpm run check:provider-ids` prints the name TMDB gives each ID in the registry. The full list is at `https://api.themoviedb.org/3/watch/providers/movie`.
- **Package name.** With the app installed on your TV: `adb shell pm list packages | grep -i <name>`. TV and phone builds often differ; list the TV one first.

## 2. Test what opens

With the app installed and signed in:

```sh
# Does a link to one title open that title?
adb shell am start -a android.intent.action.VIEW -d "https://www.netflix.com/title/70131314" --es source 30 com.netflix.ninja

# Does a search link open the search?
adb shell am start -a android.intent.action.VIEW -d "https://app.primevideo.com/search?phrase=inception" com.amazon.amazonvideo.livingroom
```

A title link needs the title's ID on that platform. If Wikidata has a property for it (search "<platform> ID" on wikidata.org), add it to `idSources` and reference it from the link with `idSource`.

## 3. Edit the registry

```json
{
  "id": "example",
  "name": "Example+",
  "tmdbIds": [123],
  "packages": ["com.example.tv", "com.example.mobile"],
  "titleLinks": [
    { "idSource": "example", "uri": "https://example.com/watch/{id}", "status": "verified" }
  ],
  "searchLink": { "uri": "https://example.com/search?q={query}", "status": "verified" }
}
```

- `{id}` is the platform's ID for the title, `{query}` is the URL-encoded title.
- Use `"status": "verified"` only for links you ran yourself, and add a row to [`registry/verified.md`](../registry/verified.md). Otherwise use `"community"`.
- Leave out `titleLinks` and `searchLink` when nothing works: the app will still launch the provider's app.
- Increase `revision` by one.

## 4. Check and send

```sh
cd proxy && pnpm run check:registry && pnpm test
```

Open a pull request. Once merged, the proxy is redeployed and every installed app picks the change up on its next start.
