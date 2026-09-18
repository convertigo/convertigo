# Release procedure

This document is the checkpoint for a Convertigo release. Open a tracking issue
from the **Release** issue template (`.github/ISSUE_TEMPLATE/release.md`),
attach it to the milestone of the version and tick the items as they are done.

Branches: `hotfix` carries the next patch or minor release (`X.Y.Z`), `develop`
the next major one. Released code is tagged from `hotfix`.

Two steps block the ones after them:

1. the `convertigo-X.Y.Z.war` asset must be attached to the GitHub release
   before the Docker official image pull request is opened, otherwise the
   Docker Library bot fails to build the image (the WAR download returns 404);
2. the official image `convertigo:X.Y.Z` must be available on Docker Hub
   before the Helm chart and the Docker docs are updated, since both reference it.

## 1. Before the release

### Tickets
- Every issue of the milestone is closed and labelled `tested`, `wontfix` or
  `invalid`; this search must return nothing:
  `is:closed is:issue milestone:X.Y.Z -label:tested -label:wontfix -label:invalid`.
- Each `tested` issue carries a comment `Tested OK with <build id>` naming the
  CI build it was verified on (`engine.GetStatus` reports the build id, for
  example `27160-hotfix-8.4.4-beta`). Issues needing a manual Studio check
  carry test notes in English.
- Issues deliberately excluded from the release (for example a reverted fix)
  are labelled `wontfix` and do not appear in the changelog.

### Changelog
- `CHANGELOG.md` has a `## X.Y.Z` section, generated with
  `./gradlew makeChangelog` then rewritten by hand: `[Category]` prefix,
  `Fixed,` for bug fixes, wording readable by a user, ascending ticket number
  inside each category, no leftover `*` marker.

### Documentation (`convertigo-doc`)
- The generated part (`reference-manual/convertigo-objects/**`, `images/beans/**`)
  is pushed to the `hotfix` branch of `convertigo-doc` by the CI build of
  `hotfix` (only `master`, `hotfix` and `develop` generate documentation).
- The hand-written part covers the user-facing changes of the milestone
  (Docker, engine properties, Administration Console, Studio, guides).
- `.circleci/doc-flow.sh audit` (run by hand in a `convertigo-doc` checkout)
  reports `master` as an ancestor of `hotfix`, with no manual difference
  between them other than the release content.

### Docker
- `docker/README.md` documents every environment variable and mount of the
  image. It is the source of both the Docker Hub description (published by the
  CI build of the `latest` image) and the Docker official docs (see below).
- `./gradlew checkDockerDocsOfficial` passes against a sibling `../docker-docs`
  checkout.

### Build
- The last CI pipeline of `hotfix` is green (engine, Studio, Docker, qualification jobs).
- `ext.convertigoVersion` in `build.gradle` equals the version to release.

## 2. Release

- Tag the release commit of `hotfix` with `X.Y.Z` and push the tag. The tag
  pipeline builds the artifacts, publishes the `convertigo/convertigo` image and
  creates a GitHub release (draft; pre-release unless the tag is a plain `X.Y.Z`).
- Review the release notes (paste the changelog section), check that
  `convertigo-X.Y.Z.war` is attached, publish the release.
- Publish the documentation: in a `convertigo-doc` checkout run
  `.circleci/doc-flow.sh release-minor` (or `release-major`). It fast-forwards
  `master` to `hotfix` and records the release point in `develop`; the `master`
  build publishes `latest` and `X.Y.x` and invalidates the CloudFront cache.

## 3. Docker official image

Requires the WAR attached to the published GitHub release.

- Run `docker/release/official-image-pr.sh X.Y.Z` from this repository with a
  sibling `../docker-official-images` checkout (fork of
  `docker-library/official-images`, `upstream` remote set). The script checks
  the WAR is downloadable, syncs the fork with upstream, updates
  `library/convertigo` (`GitCommit` of the tag, `Tags: X.Y.Z, X.Y, latest`) on a
  `convertigo-X.Y.Z` branch, pushes it and opens the pull request.
- Wait for the Docker Library review and the build of `convertigo:X.Y.Z` on
  Docker Hub (https://hub.docker.com/_/convertigo, tags tab).

## 4. After the official image is available

- Docker docs: run `docker/release/docker-docs-pr.sh` with a sibling
  `../docker-docs` checkout (fork of `docker-library/docs`, `upstream` remote
  set). The script syncs the fork, runs `./gradlew updateDockerDocsOfficial`,
  commits `convertigo/content.md` on a `convertigo-<date>` branch, pushes it and
  opens the pull request.
- Helm chart (`convertigo-helm`): set `version` and `appVersion` to `X.Y.Z` in
  `stable/convertigo/Chart.yaml`, update the README if needed, commit and push
  `master`. The GitHub Action publishes the chart to ECR Public, the S3
  repository and the AWS Marketplace ECR; check
  https://artifacthub.io/packages/helm/convertigo/convertigo afterwards.
- Check the Docker Hub description of `convertigo/convertigo` matches `docker/README.md`.
- Check https://doc.convertigo.com lists the new version in the version selector.
- Merge `hotfix` into `develop` for a minor release, bump `ext.convertigoVersion`
  on `hotfix` to the next patch version, close the milestone and the tracking issue.

## Scheduled rebuild of the released Docker images

The Docker Official Image is rebuilt by Docker whenever its base image changes;
`convertigo/convertigo` gets the same treatment from the `docker_rebuild`
workflow of `.circleci/config.yml`, run from `master` by a CircleCI scheduled
pipeline.

- Trigger (CircleCI > Project Settings > Triggers > scheduled): branch
  `master`, daily, pipeline parameter `docker_rebuild = true`. Add
  `docker_rebuild_force = true` on a second, monthly trigger to refresh the OS
  packages even when nothing else changed.
- The job reads the released version from `build.gradle` on `master`, then
  compares the digest of the Dockerfile base image and the last commit touching
  `docker/` with the `org.opencontainers.image.base.digest` and
  `org.opencontainers.image.revision` labels of `convertigo/convertigo:X.Y.Z`.
  When both match it stops in a few seconds; otherwise it rebuilds
  `docker/default` (amd64 + arm64) and `docker/aks` from `master` and pushes
  `X.Y.Z`, `latest`, `X.Y.Z-aks` and `latest-aks`. The WAR is the signed asset of
  the GitHub release, so only the system, JDK and entrypoint layers change.
- A Docker-only fix is therefore delivered by committing it on `master` (then
  merging `master` into `hotfix`): the next scheduled run republishes the images.
- Only the current release is covered; older maintenance versions are not rebuilt.
