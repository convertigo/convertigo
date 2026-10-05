---
name: convertigo-studio-update
description: Run the Convertigo Studio dependency update routine in a hotfix or develop checkout, including Gradle dependency review, authorized develop Eclipse/platform and Tycho updates, Eclipse manifest generation, Studio web npm updates and audit fix, validation, hotfix-to-develop merge handling, and ticket-specific commits. Use when asked to refresh Studio dependencies, package versions, audit-fix Studio web packages, or the Convertigo hotfix/develop update routine.
---

# Convertigo Studio Update

## Overview

Use this skill for the project's Convertigo Studio dependency update flow. All command examples run from the target checkout's root:

- hotfix: branch `hotfix`, currently ticket `#1170` for Convertigo `8.4.6`.
- develop: branch `develop`, currently ticket `#1039` for Convertigo `8.5.0`.

Prefer the local repository's actual branch, remotes, package scripts, and `build.gradle` contents over assumptions. Read applicable `AGENTS.md` files before editing.

## Helper Script

Use `scripts/studio_update.py` when it saves command bookkeeping. It defaults to dry-run, discovers the current checkout through Git, and accepts `--repo <path>` for a different checkout. `--lane` chooses the update policy, not another directory. Python 3.10+ and the standard library are sufficient.

Executing update phases requires the checkout to be on the selected lane's branch. The helper only suggests the known maintenance ticket when the repository version still matches it; otherwise confirm the issue before committing.

```bash
python3 .agents/skills/convertigo-studio-update/scripts/studio_update.py --lane hotfix --phase plan
python3 .agents/skills/convertigo-studio-update/scripts/studio_update.py --lane hotfix --phase preflight
python3 .agents/skills/convertigo-studio-update/scripts/studio_update.py --lane hotfix --phase gradle-report --execute
```

Run the helper's offline tests after changing the scripts:

```bash
python3 -m unittest discover -s .agents/skills/convertigo-studio-update/tests -v
```

Phases:

- `plan`: print the lane-specific command sequence.
- `preflight`: inspect branch, remotes, version values, npm scripts, and git status. This phase is read-only and runs immediately.
- `gradle-report`: run `./gradlew dependencyUpdatesPatch` on hotfix or `./gradlew dependencyUpdates` on develop, then the independent forced-version check below.
- `eclipse-config`: run `./gradlew generateEclipseConfigurationWithManifest` only after the selected Gradle dependency/version edits have been applied.
- `npm-update`: run `npm run deps:minor-update` on hotfix or `npm run deps:update` on develop.
- `npm-audit-fix`: run `npm audit fix` in `convertigo-studio-web` without `--force`.
- `npm-verify`: run `npm run format`, `npm run lint`, `npm run check:admin`, then `npm run build`.
- `status`: print git status.
- `all`: run all executable phases except `plan`.

The helper does not select or apply dependency versions. Its develop plan includes the manual Eclipse/Tycho review and conditional Studio build; `all` does not perform those steps automatically and is not a substitute for the review/edit/build sequence below.

## Safety Rules

- Start with `git status --short --branch`, `git remote -v`, and `git branch --show-current`.
- Do not discard, reset, or overwrite user changes. Work around unrelated dirty files.
- Treat Gradle and npm update reports as inputs for review, not as permission to update every version.
- In hotfix, keep Gradle and Java/JDK versions pinned unless the user explicitly approves a minor update. Do not apply "Gradle current updates" or `JDK_RELEASE` changes automatically.
- In develop, leave uncertain or risky library candidates unchanged by default. List them as "needs maintainer review" instead of guessing. Gradle wrapper updates are allowed only when the maintainer explicitly approves them for the current develop cycle; once approved, update `gradleVersion`, run the wrapper task, and review generated wrapper file changes. Preserve local wrapper settings such as `networkTimeout`, `retries`, and `retryBackOffMs` unless the maintainer explicitly asks to change them.
- The develop maintenance policy permits updating the Eclipse release train, its matching platform/core version, and stable Tycho releases. Check them each cycle using the procedure below; do not ask for this approval again. This does not authorize Eclipse/Tycho changes on hotfix, prereleases, unrelated JDK changes, or publication.
- Run `npm audit fix` without `--force` after the Studio web npm update. Never run `npm audit fix --force` unless the maintainer explicitly approves the breaking changes or downgrades it proposes.
- Do not commit, merge, or push unless the user explicitly asks for that step.
- After commands that can write files, inspect `git status` and `git diff` before continuing.
- Keep ticket references in commits: hotfix `8.4.6` uses `#1170`, develop `8.5.0` uses `#1039`. Verify the maintenance ticket again when the Convertigo version changes. Former ticket `#1216` is a closed duplicate of `#1170`; keep existing commit references intact, but use `#1170` for new updates.

## Lane Matrix

Hotfix:

- Branch: `hotfix`.
- Gradle report: `./gradlew dependencyUpdatesPatch --console=plain`.
- Version policy: update normal patch-line dependencies only; do not move Gradle or Java/JDK except a minor update explicitly accepted by the maintainer.
- npm update: in `convertigo-studio-web`, run `npm run deps:minor-update`.
- Gradle generation: after applying the selected Gradle dependency/version edits, run `./gradlew generateEclipseConfigurationWithManifest --console=plain` before npm work.
- Validation: after npm work, run npm `format`, `lint`, `check:admin`, `build`; rerun Gradle checks such as `dependencyUpdatesPatch` and `:engine:war` when publishing.
- Commit message pattern: `ref #1170 - Update Studio dependencies`.
- Push target when requested: verify remotes first, then push hotfix to both configured `origin` and `upstream` if that matches the current repository setup.

Develop:

- Branch: `develop`.
- Gradle report: `./gradlew dependencyUpdates --console=plain`.
- Version policy: develop may take broader updates, but risky libraries still need explicit review. Gradle wrapper updates may be applied only with the maintainer's explicit approval for that cycle, and must update `build.gradle`, `gradle/wrapper/gradle-wrapper.properties`, and any generated wrapper scripts from the wrapper task while preserving repo-specific wrapper settings. If unsure, skip the update and summarize why.
- Eclipse/Tycho: check and apply stable Eclipse train/platform and Tycho updates under the standing authorization below; validate a changed toolchain with a fresh local Maven Studio build.
- npm update: in `convertigo-studio-web`, run `npm run deps:update`.
- Gradle generation: after applying the selected Gradle dependency/version edits, run `./gradlew generateEclipseConfigurationWithManifest --console=plain` before npm work.
- Validation: same npm checks as hotfix; rerun `dependencyUpdates` and `:engine:war` when publishing.
- Commit message pattern: `ref #1039 - Update Studio dependencies`.
- Push target when requested: verify remotes first, then push develop to the configured GitHub remote, usually `origin`.

## Update Workflow

1. Run preflight in the target lane and read local instructions:

```bash
git status --short --branch
git remote -v
git branch --show-current
rg --files --hidden -g AGENTS.md -g '!**/.git/**'
```

2. Run the lane's Gradle dependency report. Review "later release versions", "exceed the version found", and "Gradle current updates". The root report finalizes `checkStudioJdkUpToDate` and `checkSwaggerUiUpToDate` (swagger-ui is downloaded from GitHub by `:engine:unzipSwaggerUI`, not resolved from Maven, so `swaggerUiVersion` in `build.gradle` is checked against the latest swagger-ui GitHub release); include both results in the summary. A swagger-ui patch-line update can be applied like other patch updates: bump `swaggerUiVersion`, run `./gradlew :engine:unzipSwaggerUI`, and check that `eclipse-plugin-studio/tomcat/webapps/convertigo/swagger/patch/swagger-initializer.js` still matches the upstream initializer. On hotfix, treat Gradle and JDK update messages as informational unless the user approved a minor update.

   **Do not conclude that dependencies are current from the Gradle report alone.** Transitive dependencies pinned through resolution rules can be missing or constrained in that report. Always run this read-only supplementary check, including when running Gradle manually rather than through the helper:

   ```bash
   python3 .agents/skills/convertigo-studio-update/scripts/check_forced_versions.py --repo <repo> --lane <hotfix|develop>
   ```

   It reads the actual Netty/Neethi `useVersion` values and checks Maven Central independently of Gradle resolution. Hotfix candidates stay on the same major/minor line; `.Final` is stable, not a prerelease. It also inventories other override rules (`force`, `strictly`, `enforcedPlatform`, substitutions) for manual review. This is a targeted supplement, not a complete transitive-dependency or vulnerability scanner.

   `UPDATE` is a review candidate, not permission to edit. `REVIEW` or `INCOMPLETE` (exit 2) means coverage is incomplete: inspect changed/unknown rules or retry metadata access before calling the update check complete. Explicitly report any unresolved check. Never describe a network failure as "up to date". Preserve intentional legacy/internal pins.

   After changing a forced family such as Netty, run `:engine:dependencyInsight --configuration compileFlat --dependency io.netty` and verify all resolved modules are aligned; compile the engine and review the rebuilt dependency archive. Security findings still require advisory and runtime-applicability review.

   On develop, also check the Eclipse train/platform and Tycho independently of the Gradle dependency report, as described below. Tycho is configured in Maven and is not covered by that report.

3. Edit the relevant `build.gradle` files manually. Prefer shared `ext.*Version` properties when they exist. On develop, apply selected Eclipse train/platform changes there and Tycho changes in the root `pom.xml` before generation. Do not update internal, constrained, or uncertain dependencies just because a newer artifact exists.

4. After applying the selected Gradle dependency/version edits, run:

```bash
./gradlew generateEclipseConfigurationWithManifest --console=plain
```

Inspect generated tracked files and fix Gradle/Eclipse fallout before npm work. Do not run this step before the selected dependency and toolchain version edits, otherwise generated manifests/default values can be based on stale dependencies. If Eclipse or Tycho changed on develop, perform the fresh Studio build below after generation.

5. In `convertigo-studio-web`, run the lane's npm update command:

```bash
npm run deps:minor-update   # hotfix
npm run deps:update         # develop
```

6. Still in `convertigo-studio-web`, run:

```bash
npm audit fix
```

Review whether the audit fix reduces vulnerabilities and whether it changes only expected package metadata or lockfile entries. If `npm audit fix` exits non-zero because only `--force` fixes remain, keep the non-forced changes only after validation passes and summarize the remaining vulnerabilities. Do not run `--force` without explicit approval.

7. Validate Studio web:

```bash
npm run format
npm run lint
npm run check:admin
npm run build
```

8. Review `git status` and `git diff`. Summarize exactly which Gradle properties, Maven/Tycho properties, package manifests, lockfiles, audit-fix lockfile updates, and generated files changed. Include the Eclipse/platform and Tycho checks in the develop summary even when no update is available.

9. If the user requested publication, commit and push only after checks pass. Use the ticket-specific commit message pattern and report only actions that actually succeeded.

## Develop Eclipse And Tycho

Check these versions on every develop update cycle, including when the Gradle report finds nothing:

- Verify the latest released Eclipse train and its matching platform/core version using official Eclipse release information. Do not take a future train, milestone, or snapshot merely because a p2 URL exists. Keep root `build.gradle` properties `ext.eclipseVersion` and `ext.eclipseBase` aligned; for example, the released `2026-09` train corresponds to platform `4.41`.
- Verify the latest stable Tycho release against the [official Tycho project](https://projects.eclipse.org/projects/technology.tycho) and [Maven Central plugin metadata](https://repo.maven.apache.org/maven2/org/eclipse/tycho/tycho-maven-plugin/maven-metadata.xml). Update the root `pom.xml` property `<tycho-version>`, which is shared by the Tycho plugins. Review release notes and Maven/JDK requirements before applying a major change; report incompatible requirements rather than silently changing the local JDK or other branch constraints.
- Apply all selected versions before `generateEclipseConfigurationWithManifest`. Let the existing Gradle generation update `eclipse-base/base.target` and product/manifest metadata; review its diff instead of maintaining conflicting p2 URLs manually.

After generation, if Eclipse or Tycho changed, validate with the existing Gradle tasks that invoke Maven:

```bash
mvn -version
./gradlew buildStudioClean --console=plain
./gradlew buildStudio --console=plain
```

Run the clean and build as separate sequential invocations. `buildStudio` invokes `mvn install` but skips execution while `eclipse-repository/target/products` already exists; a skipped task is not build validation. `buildStudioClean` deletes generated products, so preserve any needed local test edits there before cleaning. Do not delete user Eclipse workspaces or modify shipped JVM configuration just to launch a local test.

Report Maven reactor/product-build results separately from launch validation. Ask the user to test the rebuilt Studio when a GUI startup check is still outstanding. Do not claim success if metadata access or the fresh build failed.

## Hotfix To Develop Flow

After hotfix is green and pushed, merge the hotfix work into the develop checkout when requested. Locate it from the user's context or an explicit repository path; do not assume a directory layout.

- Confirm the hotfix commit/ref to merge with `git branch --contains <sha>` and `git branch -r --contains <sha>`.
- Merge the ref that actually contains the hotfix update.
- When conflicts occur in develop, package and dependency versions generally prefer develop's side. Resolve intentionally, then run the develop dependency update flow because develop performs its own broader updates.
- Do not use a blanket checkout to resolve conflicts. Inspect conflicting files and preserve non-version changes from the hotfix when they still apply.

## Known Special Cases

The Gradle report already filters unstable candidates and risky legacy lines. Preserve or review these constraints before changing them:

- Reject pre-release markers such as alpha, beta, rc, cr, milestone, ea, preview, snapshot, and `-b`.
- Keep `commons-collections:commons-collections` on major `3`.
- Keep `net.sf.jt400:jt400` on major `11`.
- Keep `org.apache.axis2:axis2-saaj` on `1.8.x`.
- Keep `org.apache.xmlbeans:xmlbeans` on the internal patched current version.
- Keep `org.apache.ws.commons.axiom:*` on major `1`.
- Keep Tomcat constrained by branch: hotfix currently rejects candidates above major `9`, develop above major `11`.

Review explicitly before changing Gradle wrapper, Eclipse/Tycho on hotfix, JxBrowser major, Tomcat major, Studio JDK, Svelte, or Tailwind. Develop Eclipse/Tycho updates use the standing authorization and validation above. The npm update scripts intentionally run a second `npm-check-updates` command that handles Svelte/Tailwind peer-related exceptions.

In develop, maintain a conservative "review first" bucket for libraries that look risky even when the report allows them. Put major jumps, runtime/container stack changes, Java/Jakarta namespace moves, Studio browser/runtime changes, security/crypto providers, persistence/cache drivers, and frontend framework toolchain jumps in that bucket unless the maintainer gave a specific go-ahead.
