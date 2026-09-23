---
name: Release
about: Tracking checklist for a Convertigo release (see RELEASE.md)
title: 'Release X.Y.Z'
labels: task
---

Release checklist for **X.Y.Z**. The full procedure is in [RELEASE.md](https://github.com/convertigo/convertigo/blob/hotfix/RELEASE.md).

### Before the release
- [ ] All milestone issues are closed and labelled `tested`, `wontfix` or `invalid` ([search](https://github.com/convertigo/convertigo/issues?q=is%3Aclosed+is%3Aissue+milestone%3AX.Y.Z+-label%3Atested+-label%3Awontfix+-label%3Ainvalid) must be empty)
- [ ] Every `tested` issue has a `Tested OK with <build id>` comment
- [ ] `CHANGELOG.md` section `## X.Y.Z` reviewed (categories, `Fixed,`, ascending ticket order)
- [ ] `convertigo-doc` `hotfix` covers the user-facing changes; `doc-flow.sh audit` is clean
- [ ] `docker/README.md` is final; `./gradlew checkDockerDocsOfficial` passes
- [ ] Last `hotfix` pipeline green, `ext.convertigoVersion` = X.Y.Z

### Release
- [ ] `release/tag-release.sh X.Y.Z` run (release commit, master, tag), tag and master pipelines green
- [ ] `release/start-next.sh X.Y.Z+1` run and `hotfix` pushed
- [ ] GitHub release draft edited (previous text, counts, absolute changelog link), `convertigo-X.Y.Z.war` attached, release published
- [ ] Documentation published (`doc-flow.sh release-minor` / `release-major`)

### Docker official image (needs the WAR on the published release)
- [ ] `release/official-image-pr.sh X.Y.Z` run, pull request opened on docker-library/official-images
- [ ] `convertigo:X.Y.Z` available on Docker Hub

### After the official image is available
- [ ] `release/docker-docs-pr.sh X.Y.Z` run, pull request opened on docker-library/docs
- [ ] Helm chart `version` / `appVersion` = X.Y.Z pushed on `master`, chart visible on Artifact Hub
- [ ] Docker Hub description matches `docker/README.md`
- [ ] doc.convertigo.com lists the new version
- [ ] `hotfix` merged into `develop`
- [ ] Milestone closed
