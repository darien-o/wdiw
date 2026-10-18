import { registryEtag, registryJson, type TitleType } from "./registry";
import { popular, search, title, UpstreamError } from "./tmdb";
import { platformIds } from "./wikidata";

export interface Env {
  TMDB_TOKEN: string;
  LIMITER?: { limit(options: { key: string }): Promise<{ success: boolean }> };
}

export interface Deps {
  fetch: typeof fetch;
  /** null disables edge caching (tests, local dev without a cache). */
  cache: Cache | null;
  waitUntil(promise: Promise<unknown>): void;
}

const MINUTE = 60;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
/** How long apps may reuse a response without asking again. */
const CLIENT_TTL = 5 * MINUTE;

const CORS = { "Access-Control-Allow-Origin": "*" };

function json(body: unknown, status = 200, headers: Record<string, string> = {}): Response {
  return new Response(typeof body === "string" ? body : JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", ...CORS, ...headers },
  });
}

function error(status: number, message: string): Response {
  return json({ error: message }, status, { "Cache-Control": "no-store" });
}

/** Returns the cached value for key, or computes it and stores it for the ttl (seconds) that compute returns. */
async function cached<T>(
  deps: Deps,
  key: string,
  compute: () => Promise<{ value: T; ttl: number }>,
): Promise<T> {
  const cacheKey = `https://cache.wdiw.invalid/${key}`;
  const hit = await deps.cache?.match(cacheKey);
  if (hit) return (await hit.json()) as T;
  const { value, ttl } = await compute();
  if (deps.cache && ttl > 0) {
    const stored = new Response(JSON.stringify(value), {
      headers: { "Content-Type": "application/json", "Cache-Control": `public, max-age=${ttl}` },
    });
    deps.waitUntil(deps.cache.put(cacheKey, stored));
  }
  return value;
}

function language(url: URL): string {
  const lang = url.searchParams.get("lang") ?? "";
  return /^[a-z]{2}(-[A-Z]{2})?$/.test(lang) ? lang : "en-US";
}

function country(url: URL, request: Request): string {
  const explicit = url.searchParams.get("country")?.toUpperCase() ?? "";
  if (/^[A-Z]{2}$/.test(explicit)) return explicit;
  return detectedCountry(request) ?? "US";
}

function detectedCountry(request: Request): string | null {
  const detected = (request as { cf?: { country?: unknown } }).cf?.country;
  // Cloudflare reports "T1" for Tor and "XX" when it does not know.
  return typeof detected === "string" && /^[A-Z]{2}$/.test(detected) && detected !== "XX"
    ? detected
    : null;
}

export async function handle(request: Request, env: Env, deps: Deps): Promise<Response> {
  if (request.method !== "GET") return error(405, "Only GET is supported");

  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "") || "/";

  if (path === "/geo") {
    return json({ country: detectedCountry(request) }, 200, { "Cache-Control": "no-store" });
  }

  if (path === "/registry") {
    const headers = { ETag: registryEtag, "Cache-Control": `public, max-age=${CLIENT_TTL}` };
    if (request.headers.get("If-None-Match") === registryEtag) {
      return new Response(null, { status: 304, headers: { ...CORS, ...headers } });
    }
    return json(registryJson, 200, headers);
  }

  const titleMatch = /^\/title\/(movie|tv)\/([0-9]{1,9})$/.exec(path);
  if (path !== "/search" && path !== "/popular" && !titleMatch) return error(404, "Not found");

  if (env.LIMITER) {
    const ip = request.headers.get("CF-Connecting-IP") ?? "unknown";
    const { success } = await env.LIMITER.limit({ key: ip });
    if (!success) return error(429, "Too many requests");
  }

  const lang = language(url);
  const ok = (body: unknown) => json(body, 200, { "Cache-Control": `public, max-age=${CLIENT_TTL}` });

  try {
    if (path === "/search") {
      const query = (url.searchParams.get("q") ?? "").trim().slice(0, 100);
      if (!query) return error(400, "Missing q");
      const key = `search/${lang}/${encodeURIComponent(query.toLowerCase())}`;
      const results = await cached(deps, key, async () => ({
        value: await search(deps.fetch, env.TMDB_TOKEN, query, lang),
        ttl: HOUR,
      }));
      return ok({ results });
    }

    const region = country(url, request);

    if (path === "/popular") {
      const results = await cached(deps, `popular/${region}/${lang}`, async () => ({
        value: await popular(deps.fetch, env.TMDB_TOKEN, region, lang),
        ttl: 6 * HOUR,
      }));
      return ok({ country: region, results });
    }

    const type = titleMatch![1] as TitleType;
    const id = Number(titleMatch![2]);
    const body = await cached(deps, `title/${type}/${id}/${region}/${lang}`, async () => {
      const { wikidataId, ...details } = await title(
        deps.fetch,
        env.TMDB_TOKEN,
        type,
        id,
        region,
        lang,
      );
      // Platform IDs do not depend on country or language, so they are cached on their own and for longer.
      const ids = await cached(deps, `ids/${type}/${id}`, async () => ({
        value: await platformIds(deps.fetch, type, id, wikidataId),
        ttl: 7 * DAY,
      })).catch(() => null);
      // Without Wikidata the answer is still useful, but retry soon instead of keeping it for hours.
      return { value: { ...details, country: region, ids: ids ?? {} }, ttl: ids ? 6 * HOUR : 5 * MINUTE };
    });
    return ok(body);
  } catch (e) {
    if (e instanceof UpstreamError && e.status === 404) return error(404, "Title not found");
    console.error(e);
    return error(502, "Upstream unavailable");
  }
}

export default {
  fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    return handle(request, env, {
      fetch: (input, init) => fetch(input, init),
      cache: caches.default,
      waitUntil: (promise) => ctx.waitUntil(promise),
    });
  },
};
