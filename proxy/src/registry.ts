import data from "../../registry/providers.json";

export type TitleType = "movie" | "tv";

interface Registry {
  revision: number;
  idSources: Record<string, Partial<Record<TitleType, string>>>;
}

export const registry: Registry = data;
export const registryJson = JSON.stringify(data);
export const registryEtag = `"r${data.revision}"`;

/** Wikidata property -> idSource name, for the given title type. */
export function wikidataProperties(type: TitleType): Map<string, string> {
  const props = new Map<string, string>();
  for (const [name, source] of Object.entries(registry.idSources)) {
    const prop = source[type];
    if (prop) props.set(prop, name);
  }
  return props;
}
