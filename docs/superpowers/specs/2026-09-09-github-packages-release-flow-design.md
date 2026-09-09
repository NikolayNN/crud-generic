# GitHub Packages publishing and CI-driven releases — Design

**Date:** 2026-09-09
**Status:** approved

## Context

The library is published through JitPack. That arrangement has drifted far enough from the
release standard in `~/.claude/CLAUDE.md` that several things are quietly broken:

- **The pom version means nothing.** `library/pom.xml` says `5.0-SNAPSHOT` while the last
  released artifact is `13.3.15-jakarta`. JitPack builds from a git tag and substitutes the
  version, so the tag is the input and the pom is ignored. The standard says the opposite:
  the pom is the single source of the version and the tag is an output of CI.
- **Tags are placed by hand.** 200+ tags exist, created manually, in three different schemes
  (`13.3.15-jakarta`, `13.3.9-javax`, `10.4`).
- **There is no CI at all.** No `.github/` directory, no automated verification before a release.
- **`mvn verify` does not run 31 of the tests.** `test-application` holds 14 test classes,
  9 of them named `*IT`. Surefire only picks up `*Test`/`Test*`/`*Tests`, and failsafe is not
  configured, so `FlexSaveCascadeIT`, `FlexUpdateIT`, `FlexDeleteIT`, `MeetingPageIT` and five
  others never execute. Run explicitly they are green (31 tests). A CI release gate built on
  `mvn verify` would therefore be green while checking almost none of the integration behaviour.
- **`versions:set` does not reach the library.** The root pom is a bare aggregator, and
  `library` declares its own `groupId`/`version` with no `<parent>`. Verified experimentally:
  `mvn versions:set -DnewVersion=14.1` at the root updated `pom.xml` only and left
  `library/pom.xml` at its old version. `/aurora-release` invokes exactly that command, so it
  would bump the aggregator and publish the old library version.
- **Dead publishing machinery.** `site-maven-plugin` pushing artifacts into an `mvn-repo` branch,
  `internal.repo` `distributionManagement` pointing at a local staging directory, and a
  `github.global.server` property — the remains of the "maven repo inside a git branch" approach.

A second problem sits on top: `library` depends on `com.github.NikolayNN:filter-specifications-lib:3.1-jakarta`,
which itself only exists on JitPack. Publishing to GitHub Packages without addressing it would
leave every consumer needing both repositories.

## Decisions

Made with the owner during brainstorming:

| Question | Decision |
|---|---|
| Release model | Full aurora standard: `develop` + `master`, `CHANGELOG.md`, `/aurora-release`, CI creates the tag |
| Coordinates | `by.nhorushko:crud-abstract-generic:14.0` — native pom coordinates, major bump, no `-jakarta` suffix |
| `filter-specifications-lib` | **Absorbed into this repository**, package preserved |
| Absorbed revision | Tag `3.1-jakarta` — exactly what is in the build today |
| JitPack | Removed entirely; `jitpack.yml` deleted |
| Publishing from `develop` | No. Releases only; `mvn install` covers local iteration |
| Reactor structure | Root pom becomes the real parent of both modules |

### Why absorb rather than migrate the dependency separately

`filter-specifications-lib` is 11 classes and 766 lines at `3.1-jakarta`, last touched in
June 2024. No project declares it in a pom — `LocatorServer` (52 files importing
`by.nhorushko.filterspecification.*`) and `bi-dvr` (5 files) both receive it transitively
through crud-generic. Moving the sources here while keeping the package name means:

- no consumer changes a single `import`;
- no duplicate-classes risk, since nothing pulls both jars;
- JitPack disappears from the published pom completely, instead of leaking a third-party
  `<repositories>` block into every consumer's dependency resolution.

Revision `3.1-jakarta` is taken rather than the newer tags because `3.2-jakarta` (polymorphic
type handling in `getPath`) and `3.3.0-jakarta` (nested collections) are behaviour changes.
Release 14.0 stays a pure relocation: if filtering misbehaves afterwards, the filter code is
provably not the cause. Those two fixes can be pulled in later as their own changelog entry.
Note that `master` in that repository is *not* a descendant of `3.3.0-jakarta` — the branches
diverged by three lines in `FilterSpecifications` — so `master` is not a candidate.

## Design

### Reactor and version

`pom.xml` becomes the parent of both modules and the only place the version lives:

```
by.nhorushko:crud-abstract-generic-parent:14.0   packaging=pom
├── properties: maven.compiler.release=25, lombok.version, spring-boot.version=3.5.16
├── dependencyManagement: import spring-boot-dependencies:3.5.16
├── pluginManagement: compiler, surefire, failsafe, source, deploy
└── distributionManagement: GitHub Packages
```

No `<repositories>` element anywhere — JitPack leaves the project entirely.

`library/pom.xml` declares `<parent>` and drops its own `groupId` and `version`. Removed:
`distributionManagement` (`internal.repo`), `site-maven-plugin`, `maven-deploy-plugin` with
`altDeploymentRepository`, the `github.global.server` property, the `repositories` block, and
the `filter-specifications-lib` dependency. Added: `jackson-annotations` (needed by `@JsonValue`
on the absorbed `FilterOperation` enum) pinned to `2.21.4`, the version Boot 3.5.16 manages —
an explicit version, consistent with how the other `library` dependencies are declared.

`test-application/pom.xml` moves from `spring-boot-starter-parent` to the root parent; the Boot
BOM reaches it through the `dependencyManagement` import above. It sets `maven.deploy.skip=true`
(a demo app is not an artifact), refers to the library as `${project.version}`, and gains a
**failsafe** execution so the 31 integration tests actually run.

### Absorbing filter-specifications

The 11 main classes and 1 test class from tag `3.1-jakarta` move to
`library/src/main/java/by/nhorushko/filterspecification/` and
`library/src/test/java/by/nhorushko/filterspecification/`, package unchanged. This is its own
commit, before the infrastructure work, so history shows a verbatim relocation separately from
everything else. Correctness is checked by the existing `FilterFields*` tests, which exercise
this API directly.

`NikolayNN/filter-specifications-lib` becomes a dead source afterwards and should be archived
on GitHub so nothing re-adds it. That is a manual action for the owner, outside this work.

### Publishing

```xml
<distributionManagement>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/NikolayNN/crud-generic</url>
  </repository>
</distributionManagement>
```

Two artifacts are published: the parent pom (consumers need it to resolve) and
`crud-abstract-generic` as jar + sources-jar + pom. `test-application` is skipped.

CI authenticates via `actions/setup-java` (`server-id: github`, username `GITHUB_ACTOR`,
password `GITHUB_TOKEN`) with `permissions: packages: write, contents: write`. No extra
secrets are needed, unlike the docker services in the standard.

Consumers need a `github` server entry in `~/.m2/settings.xml` with a PAT carrying
`read:packages`, plus a `<repositories>` block. GitHub Packages requires authentication even
for reading public packages — this is the real cost of the move and is documented explicitly
in the README rather than glossed over.

### CI

`.github/workflows/ci.yml`, `concurrency` per ref, Temurin 25, `cache: maven`:

- **`verify`** — on PRs into `develop`/`master` and pushes to `develop`: `./mvnw -B verify` only.
- **`release`** — on push to `master`: read the version via `help:evaluate`; fail if tag
  `v<version>` already exists ("version not bumped"); run `./mvnw -B deploy` (which includes
  verify); then create and push tag `v<version>` and a GitHub Release from the CHANGELOG section.

No separate "is this version already published" query. The tag is written only after a
successful deploy, so the tag check covers it, and GitHub Packages returns 409 on an attempt to
overwrite a release version, which fails the build with a clear message.

### Branches and changelog

`develop` is created from the current `master`; `master` becomes release-only, and `develop`
should be made the default branch in GitHub settings. `CHANGELOG.md` starts fresh with
`## Не выпущено` and `## 14.0`. History before 14.0 is not reconstructed — it lived in JitPack
tags and no reliable record exists; one line states this rather than inventing entries.

### Guardrails and hygiene

`.claude/settings.json` with a PreToolUse Bash hook (`.claude/hooks/guard-bash.sh`, adapted from
security-gateway-service) blocking `mvn deploy`, `git tag` and `git push --force`. Publishing is
done by CI and by a human, not by an agent.

Per section 7 of the standard: `.gitattributes`, `.mvn/maven.config` with `-ntp`, Maven wrapper
3.9.x in `only-script` mode (so `./mvnw` in the README and CI works without a jar in the repo),
and a project `CLAUDE.md`.

`jitpack.yml` is deleted. The `mvn-repo`, `javax` and dependabot branches on origin become dead
weight; removing them is proposed to the owner, not done unilaterally.

## Commit sequence

1. `refactor: absorb filter-specifications-lib sources`
2. `build: make root pom the single source of version`
3. `build: publish to GitHub Packages instead of JitPack`
4. `ci: verify on PR, release from master`
5. `chore: repo hygiene and agent guardrails`
6. `docs: README and CHANGELOG for the new release flow`

## Verification

- `mvn -B verify` — 74 tests across 23 classes today; after failsafe and the absorption it must
  reach roughly 105 across 33 classes. Exact counts recorded before and after.
- `versions:set -DnewVersion=14.1` at the root must update the root pom, `library`, and the
  library reference in `test-application`; then reverted. This is precisely the case that failed
  in the experiment above.
- Trial deploy to a local directory
  (`-DaltDeploymentRepository=local::file:./target/staging`): staging must contain the parent pom
  and `crud-abstract-generic` (jar, sources, pom), and must not contain `test-application`.
- CI itself is verified by the first real release through `/aurora-release`.

## Out of scope

After 14.0 ships, `LocatorServer` and `bi-dvr` keep compiling without source changes — the
package is preserved — but need new coordinates in their poms, a repository entry, and a PAT.
That work belongs to those repositories.

Pulling in the `3.2`/`3.3` filter fixes is a separate, later change with its own changelog entry.
