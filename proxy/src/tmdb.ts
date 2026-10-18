import type { TitleType } from "./registry";

const API = "https://api.themoviedb.org/3";

/** Offer kinds in the order the apps show them. */
const OFFER_KINDS = ["flatrate", "free", "ads", "rent", "buy"] as const;
export type OfferKind = (typeof OFFER_KINDS)[number];

export class UpstreamError extends Error {
  constructor(readonly status: number) {
    super(`TMDB responded ${status}`);
  }
}

export interface Summary {
  id: number;
  type: TitleType;
  title: string;
  year: number | null;
  poster: string | null;
  backdrop: string | null;
  rating: number | null;
}

export interface Offer {
  providerId: number;
  name: string;
  logo: string | null;
  kinds: OfferKind[];
}

export interface TitleDetails extends Summary {
  originalTitle: string;
  overview: string;
  runtime: number | null;
  genres: string[];
  wikidataId: string | null;
  /** TMDB's own "where to watch" page for the country, or null when nothing is listed. */
  watchLink: string | null;
  offers: Offer[];
}

interface RawTitle {
  id: number;
  media_type?: string;
  title?: string;
  name?: string;
  original_title?: string;
  original_name?: string;
  release_date?: string;
  first_air_date?: string;
  poster_path?: string | null;
  backdrop_path?: string | null;
  vote_average?: number;
  vote_count?: number;
  overview?: string;
  runtime?: number | null;
  episode_run_time?: number[];
  genres?: { name: string }[];
  external_ids?: { wikidata_id?: string | null };
  "watch/providers"?: { results?: Record<string, RawCountryOffers> };
}

interface RawProvider {
  provider_id: number;
  provider_name: string;
  logo_path?: string | null;
  display_priority?: number;
}

type RawCountryOffers = { link?: string } & Partial<Record<OfferKind, RawProvider[]>>;

export async function tmdb<T>(
  fetcher: typeof fetch,
  token: string,
  path: string,
  params: Record<string, string>,
): Promise<T> {
  const url = `${API}${path}?${new URLSearchParams(params)}`;
  const res = await fetcher(url, {
    headers: { Authorization: `Bearer ${token}`, Accept: "application/json" },
  });
  if (!res.ok) throw new UpstreamError(res.status);
  return (await res.json()) as T;
}

function summary(raw: RawTitle, type: TitleType): Summary {
  const date = raw.release_date || raw.first_air_date || "";
  const year = Number.parseInt(date.slice(0, 4), 10);
  return {
    id: raw.id,
    type,
    title: raw.title ?? raw.name ?? "",
    year: Number.isNaN(year) ? null : year,
    poster: raw.poster_path ?? null,
    backdrop: raw.backdrop_path ?? null,
    rating: raw.vote_count ? Math.round((raw.vote_average ?? 0) * 10) / 10 : null,
  };
}

function offers(country: RawCountryOffers | undefined): Offer[] {
  if (!country) return [];
  const byId = new Map<number, Offer & { priority: number }>();
  for (const kind of OFFER_KINDS) {
    for (const p of country[kind] ?? []) {
      const existing = byId.get(p.provider_id);
      if (existing) {
        existing.kinds.push(kind);
      } else {
        byId.set(p.provider_id, {
          providerId: p.provider_id,
          name: p.provider_name,
          logo: p.logo_path ?? null,
          kinds: [kind],
          priority: p.display_priority ?? Number.MAX_SAFE_INTEGER,
        });
      }
    }
  }
  // Subscriptions first, then free, then paid; TMDB's own ordering breaks ties.
  return [...byId.values()]
    .sort(
      (a, b) =>
        OFFER_KINDS.indexOf(a.kinds[0]!) - OFFER_KINDS.indexOf(b.kinds[0]!) ||
        a.priority - b.priority,
    )
    .map(({ priority: _, ...offer }) => offer);
}

export async function search(
  fetcher: typeof fetch,
  token: string,
  query: string,
  lang: string,
): Promise<Summary[]> {
  const body = await tmdb<{ results: RawTitle[] }>(fetcher, token, "/search/multi", {
    query,
    language: lang,
    include_adult: "false",
  });
  return body.results
    .filter((r) => r.media_type === "movie" || r.media_type === "tv")
    .map((r) => summary(r, r.media_type as TitleType));
}

/** Popular titles that can be streamed with a subscription in the country, movies and series interleaved. */
export async function popular(
  fetcher: typeof fetch,
  token: string,
  country: string,
  lang: string,
): Promise<Summary[]> {
  const discover = (type: TitleType) =>
    tmdb<{ results: RawTitle[] }>(fetcher, token, `/discover/${type}`, {
      language: lang,
      watch_region: country,
      with_watch_monetization_types: "flatrate",
      sort_by: "popularity.desc",
      include_adult: "false",
    }).then((body) => body.results.map((r) => summary(r, type)));
  const [movies, shows] = await Promise.all([discover("movie"), discover("tv")]);
  const mixed: Summary[] = [];
  for (let i = 0; i < Math.max(movies.length, shows.length); i++) {
    if (movies[i]) mixed.push(movies[i]!);
    if (shows[i]) mixed.push(shows[i]!);
  }
  return mixed;
}

export async function title(
  fetcher: typeof fetch,
  token: string,
  type: TitleType,
  id: number,
  country: string,
  lang: string,
): Promise<TitleDetails> {
  const raw = await tmdb<RawTitle>(fetcher, token, `/${type}/${id}`, {
    language: lang,
    append_to_response: "watch/providers,external_ids",
  });
  const countryOffers = raw["watch/providers"]?.results?.[country];
  return {
    ...summary(raw, type),
    originalTitle: raw.original_title ?? raw.original_name ?? "",
    overview: raw.overview ?? "",
    runtime: raw.runtime ?? raw.episode_run_time?.[0] ?? null,
    genres: (raw.genres ?? []).map((g) => g.name),
    wikidataId: raw.external_ids?.wikidata_id || null,
    watchLink: countryOffers?.link ?? null,
    offers: offers(countryOffers),
  };
}
