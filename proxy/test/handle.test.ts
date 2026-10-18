import { describe, expect, it, vi } from "vitest";
import { type Deps, type Env, handle } from "../src/index";
import { registry } from "../src/registry";

const env: Env = { TMDB_TOKEN: "test-token" };

const INCEPTION = {
  id: 27205,
  title: "Inception",
  original_title: "Inception",
  release_date: "2010-07-15",
  poster_path: "/poster.jpg",
  backdrop_path: "/backdrop.jpg",
  vote_average: 8.369,
  vote_count: 36000,
  overview: "A thief who steals secrets.",
  runtime: 148,
  genres: [{ name: "Action" }, { name: "Science Fiction" }],
  external_ids: { wikidata_id: "Q25188" },
  "watch/providers": {
    results: {
      CO: {
        link: "https://www.themoviedb.org/movie/27205-inception/watch?locale=CO",
        rent: [{ provider_id: 2, provider_name: "Apple TV", logo_path: "/apple.jpg", display_priority: 4 }],
        flatrate: [
          { provider_id: 1899, provider_name: "Max", logo_path: "/max.jpg", display_priority: 9 },
          { provider_id: 8, provider_name: "Netflix", logo_path: "/netflix.jpg", display_priority: 1 },
        ],
        buy: [{ provider_id: 2, provider_name: "Apple TV", logo_path: "/apple.jpg", display_priority: 4 }],
      },
    },
  },
};

const WIKIDATA = {
  results: {
    bindings: [
      { p: { value: "http://www.wikidata.org/prop/direct/P1874" }, v: { value: "70131314" } },
      { p: { value: "http://www.wikidata.org/prop/direct/P1874" }, v: { value: "99999999" } },
      { p: { value: "http://www.wikidata.org/prop/direct/P9586" }, v: { value: "umc.cmc.abc" } },
    ],
  },
};

type Route = (url: URL) => Response | undefined;

function deps(...routes: Route[]): Deps & { fetch: ReturnType<typeof vi.fn> } {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = new URL(String(input));
    for (const route of routes) {
      const res = route(url);
      if (res) return res;
    }
    throw new Error(`Unexpected fetch: ${url}`);
  });
  return { fetch: fetchMock as never, cache: null, waitUntil: () => {} };
}

const tmdbTitle: Route = (url) =>
  url.hostname === "api.themoviedb.org" && url.pathname === "/3/movie/27205"
    ? Response.json(INCEPTION)
    : undefined;
const wikidata: Route = (url) =>
  url.hostname === "query.wikidata.org" ? Response.json(WIKIDATA) : undefined;

function get(path: string, init?: RequestInit): Request {
  return new Request(`https://proxy.test${path}`, init);
}

describe("/title", () => {
  it("merges details, the country's offers and the platform IDs", async () => {
    const d = deps(tmdbTitle, wikidata);
    const res = await handle(get("/title/movie/27205?country=co&lang=es-CO"), env, d);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({
      id: 27205,
      type: "movie",
      title: "Inception",
      year: 2010,
      poster: "/poster.jpg",
      backdrop: "/backdrop.jpg",
      rating: 8.4,
      originalTitle: "Inception",
      overview: "A thief who steals secrets.",
      runtime: 148,
      genres: ["Action", "Science Fiction"],
      watchLink: "https://www.themoviedb.org/movie/27205-inception/watch?locale=CO",
      offers: [
        { providerId: 8, name: "Netflix", logo: "/netflix.jpg", kinds: ["flatrate"] },
        { providerId: 1899, name: "Max", logo: "/max.jpg", kinds: ["flatrate"] },
        { providerId: 2, name: "Apple TV", logo: "/apple.jpg", kinds: ["rent", "buy"] },
      ],
      country: "CO",
      ids: { netflix: "70131314", appletv: "umc.cmc.abc" },
    });

    const tmdbCall = new URL(String(d.fetch.mock.calls[0]![0]));
    expect(tmdbCall.searchParams.get("language")).toBe("es-CO");
    expect(d.fetch.mock.calls[0]![1].headers.Authorization).toBe("Bearer test-token");
    const sparql = new URL(String(d.fetch.mock.calls[1]![0])).searchParams.get("query")!;
    expect(sparql).toContain("BIND(wd:Q25188 AS ?item)");
    expect(sparql).toContain("wdt:P1874");
  });

  it("returns no offers for a country TMDB has no data for", async () => {
    const res = await handle(get("/title/movie/27205?country=JP"), env, deps(tmdbTitle, wikidata));
    const body = (await res.json()) as { offers: unknown[]; watchLink: unknown; country: string };
    expect(body.country).toBe("JP");
    expect(body.offers).toEqual([]);
    expect(body.watchLink).toBeNull();
  });

  it("still answers when Wikidata is down", async () => {
    const down: Route = (url) =>
      url.hostname === "query.wikidata.org" ? new Response("busy", { status: 503 }) : undefined;
    const res = await handle(get("/title/movie/27205?country=CO"), env, deps(tmdbTitle, down));
    expect(res.status).toBe(200);
    expect(((await res.json()) as { ids: unknown }).ids).toEqual({});
  });

  it("looks the item up by TMDB ID when TMDB has no Wikidata ID", async () => {
    const noWikidataId: Route = (url) =>
      url.hostname === "api.themoviedb.org"
        ? Response.json({ ...INCEPTION, external_ids: {} })
        : undefined;
    const d = deps(noWikidataId, wikidata);
    await handle(get("/title/movie/27205?country=CO"), env, d);
    const sparql = new URL(String(d.fetch.mock.calls[1]![0])).searchParams.get("query")!;
    expect(sparql).toContain('?item wdt:P4947 "27205".');
  });

  it("uses the country Cloudflare detected when none is given", async () => {
    const request = Object.assign(get("/title/movie/27205"), { cf: { country: "CO" } });
    const res = await handle(request, env, deps(tmdbTitle, wikidata));
    expect(((await res.json()) as { country: string }).country).toBe("CO");
  });

  it("maps a TMDB 404 to 404 and other failures to 502", async () => {
    const status = (code: number): Route => () => new Response("{}", { status: code });
    expect((await handle(get("/title/tv/1"), env, deps(status(404)))).status).toBe(404);
    expect((await handle(get("/title/tv/1"), env, deps(status(500)))).status).toBe(502);
  });

  it("rejects malformed paths", async () => {
    expect((await handle(get("/title/person/1"), env, deps())).status).toBe(404);
    expect((await handle(get("/title/movie/abc"), env, deps())).status).toBe(404);
  });
});

describe("/search", () => {
  const results: Route = () =>
    Response.json({
      results: [
        { id: 1, media_type: "movie", title: "Dune", release_date: "2021-09-15", vote_count: 0 },
        { id: 2, media_type: "person", name: "Someone" },
        { id: 3, media_type: "tv", name: "Dune: Prophecy", first_air_date: "" },
      ],
    });

  it("keeps movies and series only", async () => {
    const res = await handle(get("/search?q=dune"), env, deps(results));
    expect(await res.json()).toEqual({
      results: [
        { id: 1, type: "movie", title: "Dune", year: 2021, poster: null, backdrop: null, rating: null },
        { id: 3, type: "tv", title: "Dune: Prophecy", year: null, poster: null, backdrop: null, rating: null },
      ],
    });
  });

  it("requires a query", async () => {
    expect((await handle(get("/search?q=%20"), env, deps())).status).toBe(400);
  });
});

describe("/popular", () => {
  it("interleaves movies and series for the country", async () => {
    const discover: Route = (url) =>
      Response.json({
        results: url.pathname.endsWith("/movie")
          ? [{ id: 1, title: "M1" }, { id: 2, title: "M2" }]
          : [{ id: 3, name: "S1" }],
      });
    const d = deps(discover);
    const res = await handle(get("/popular?country=CO"), env, d);
    const body = (await res.json()) as { country: string; results: { title: string }[] };
    expect(body.country).toBe("CO");
    expect(body.results.map((r) => r.title)).toEqual(["M1", "S1", "M2"]);
    expect(new URL(String(d.fetch.mock.calls[0]![0])).searchParams.get("watch_region")).toBe("CO");
  });
});

describe("/registry", () => {
  it("serves the registry with an ETag and answers 304 to a matching one", async () => {
    const res = await handle(get("/registry"), env, deps());
    const etag = res.headers.get("ETag")!;
    expect(((await res.json()) as { revision: number }).revision).toBe(registry.revision);
    const again = await handle(get("/registry", { headers: { "If-None-Match": etag } }), env, deps());
    expect(again.status).toBe(304);
  });
});

describe("caching and limits", () => {
  it("serves a repeated request from the cache", async () => {
    const store = new Map<string, Response>();
    const cache = {
      match: async (key: string) => store.get(key)?.clone(),
      put: async (key: string, res: Response) => void store.set(key, res),
    } as unknown as Cache;
    const d = { ...deps(tmdbTitle, wikidata), cache };
    const first = await handle(get("/title/movie/27205?country=CO"), env, d);
    const second = await handle(get("/title/movie/27205?country=CO"), env, d);
    expect(await second.json()).toEqual(await first.json());
    expect(d.fetch).toHaveBeenCalledTimes(2);
  });

  it("answers 429 when the limiter refuses", async () => {
    const limited: Env = { ...env, LIMITER: { limit: async () => ({ success: false }) } };
    expect((await handle(get("/search?q=dune"), limited, deps())).status).toBe(429);
  });

  it("only accepts GET", async () => {
    expect((await handle(get("/search?q=dune", { method: "POST" }), env, deps())).status).toBe(405);
  });
});
