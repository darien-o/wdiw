import { type TitleType, wikidataProperties } from "./registry";

const ENDPOINT = "https://query.wikidata.org/sparql";
const USER_AGENT = "wdiw-proxy/1 (https://github.com/darien-o/wdiw)";
const TMDB_ID_PROPERTY: Record<TitleType, string> = { movie: "P4947", tv: "P4983" };

interface SparqlResponse {
  results: { bindings: { p: { value: string }; v: { value: string } }[] };
}

export function buildQuery(type: TitleType, tmdbId: number, wikidataId: string | null): string {
  const values = [...wikidataProperties(type).keys()].map((p) => `wdt:${p}`).join(" ");
  const item = wikidataId
    ? `BIND(wd:${wikidataId} AS ?item)`
    : `?item wdt:${TMDB_ID_PROPERTY[type]} "${tmdbId}".`;
  return `SELECT ?p ?v WHERE { ${item} VALUES ?p { ${values} } ?item ?p ?v. }`;
}

/**
 * Platform IDs for a title, keyed by idSource name (e.g. { netflix: "70131314" }).
 * Throws when Wikidata cannot be reached, so the caller can skip caching.
 */
export async function platformIds(
  fetcher: typeof fetch,
  type: TitleType,
  tmdbId: number,
  wikidataId: string | null,
): Promise<Record<string, string>> {
  const props = wikidataProperties(type);
  if (props.size === 0) return {};
  const safeWikidataId = wikidataId && /^Q[0-9]+$/.test(wikidataId) ? wikidataId : null;
  const url = `${ENDPOINT}?query=${encodeURIComponent(buildQuery(type, tmdbId, safeWikidataId))}`;
  const res = await fetcher(url, {
    headers: { Accept: "application/sparql-results+json", "User-Agent": USER_AGENT },
  });
  if (!res.ok) throw new Error(`Wikidata responded ${res.status}`);
  const body = (await res.json()) as SparqlResponse;
  const ids: Record<string, string> = {};
  for (const { p, v } of body.results.bindings) {
    const name = props.get(p.value.slice(p.value.lastIndexOf("/") + 1));
    if (name && !(name in ids)) ids[name] = v.value;
  }
  return ids;
}
