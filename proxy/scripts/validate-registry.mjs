// Checks registry/providers.json against its schema, plus the rules a JSON Schema cannot express.
import { readFileSync } from "node:fs";
import Ajv from "ajv/dist/2020.js";

const read = (name) => JSON.parse(readFileSync(new URL(`../../registry/${name}`, import.meta.url), "utf8"));
const schema = read("providers.schema.json");
const registry = read("providers.json");

const errors = [];
const validate = new Ajv({ allErrors: true }).compile(schema);
if (!validate(registry)) {
  for (const e of validate.errors) errors.push(`${e.instancePath || "/"} ${e.message}`);
}

const seenIds = new Set();
const seenTmdbIds = new Map();
for (const provider of registry.providers ?? []) {
  if (seenIds.has(provider.id)) errors.push(`duplicate provider id "${provider.id}"`);
  seenIds.add(provider.id);

  for (const tmdbId of provider.tmdbIds ?? []) {
    const owner = seenTmdbIds.get(tmdbId);
    if (owner) errors.push(`TMDB id ${tmdbId} is claimed by both "${owner}" and "${provider.id}"`);
    seenTmdbIds.set(tmdbId, provider.id);
  }

  for (const link of provider.titleLinks ?? []) {
    const source = registry.idSources?.[link.idSource];
    if (!source) {
      errors.push(`${provider.id}: unknown idSource "${link.idSource}"`);
    } else {
      for (const type of link.types ?? ["movie", "tv"]) {
        if (!source[type] && link.types) {
          errors.push(`${provider.id}: idSource "${link.idSource}" has no ${type} property`);
        }
      }
    }
    if (!link.uri.includes("{id}")) errors.push(`${provider.id}: title link has no {id}`);
  }
  if (provider.searchLink && !provider.searchLink.uri.includes("{query}")) {
    errors.push(`${provider.id}: search link has no {query}`);
  }
}

if (errors.length) {
  console.error(`registry/providers.json is invalid:\n- ${errors.join("\n- ")}`);
  process.exit(1);
}
console.log(`registry/providers.json is valid (revision ${registry.revision}, ${registry.providers.length} providers)`);
