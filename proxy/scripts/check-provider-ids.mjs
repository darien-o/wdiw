// Compares the TMDB provider IDs in the registry with TMDB's own provider list.
// Usage: TMDB_TOKEN=... node scripts/check-provider-ids.mjs
import { readFileSync } from "node:fs";

const token = process.env.TMDB_TOKEN;
if (!token) {
  console.error("Set TMDB_TOKEN to the TMDB API Read Access Token.");
  process.exit(1);
}

const registry = JSON.parse(
  readFileSync(new URL("../../registry/providers.json", import.meta.url), "utf8"),
);

const names = new Map();
for (const type of ["movie", "tv"]) {
  const res = await fetch(`https://api.themoviedb.org/3/watch/providers/${type}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    console.error(`TMDB responded ${res.status}`);
    process.exit(1);
  }
  for (const p of (await res.json()).results) names.set(p.provider_id, p.provider_name);
}

let unknown = 0;
for (const provider of registry.providers) {
  for (const id of provider.tmdbIds) {
    const name = names.get(id);
    if (!name) unknown++;
    console.log(`${name ? " " : "!"} ${provider.name.padEnd(20)} ${String(id).padStart(5)}  ${name ?? "NOT A TMDB PROVIDER"}`);
  }
}
console.log("\nRead the right-hand names: a wrong ID shows up as a different service's name.");
process.exit(unknown ? 1 : 0);
