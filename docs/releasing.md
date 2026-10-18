# Releasing

## One-time setup

1. **Signing key.** Create it once and keep it safe. Losing it means existing installs can never be updated.

   ```sh
   keytool -genkeypair -v -keystore release.keystore -alias wdiw -keyalg RSA -keysize 4096 -validity 10000
   ```

2. **Local release builds.** Put the key next to `android-tv/build.gradle.kts` and create `android-tv/keystore.properties` (both are git-ignored):

   ```properties
   storeFile=release.keystore
   storePassword=...
   keyAlias=wdiw
   keyPassword=...
   ```

3. **GitHub secrets.** Keep every secret in GitHub, inside two environments (Settings → Environments) rather than at repository level, so only the workflow that needs a secret can read it:

   | Environment | Secrets | Used by |
   |---|---|---|
   | `release` | `KEYSTORE_BASE64` (`base64 -i release.keystore`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | `release.yml` |
   | `production` | `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`, `TMDB_TOKEN` | `deploy-proxy.yml` |

   Also add the repository variable `PROXY_URL` (the deployed Worker's address). On the `release` environment, allow only `v*` tags under "Deployment branches and tags". Give the Cloudflare token only the "Edit Cloudflare Workers" permission.

4. **Proxy.** Merging to `main` deploys it and copies `TMDB_TOKEN` from GitHub into the Worker. To deploy by hand instead: `cd proxy && pnpm exec wrangler login && pnpm exec wrangler secret put TMDB_TOKEN && pnpm run deploy`. The free `*.workers.dev` address works but does not cache; attach a custom domain to the Worker to turn caching on.

## Each release

1. Raise `versionName` in `android-tv/app/build.gradle.kts`. `versionCode` is worked out from it.
2. Commit, then tag with the same version: `git tag v0.2.0 && git push --tags`.

The `Release` workflow builds the signed APK and attaches it to a GitHub release. Installed apps offer it on their next start, and [Obtainium](https://github.com/ImranR98/Obtainium) users get it from the same place.

Registry and proxy changes do not need a release: merging to `main` deploys them.
