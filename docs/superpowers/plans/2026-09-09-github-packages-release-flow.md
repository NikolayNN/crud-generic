# GitHub Packages Release Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move `crud-generic` off JitPack onto GitHub Packages, absorb `filter-specifications-lib` into the repository, and put releases behind GitHub Actions so CI — not a human — creates the version tag.

**Architecture:** The root `pom.xml` becomes the real parent of both modules and the single source of the version (`14.0`). `library` publishes to GitHub Packages as `by.nhorushko:crud-abstract-generic`; `test-application` stays a non-published demo whose integration tests finally run under failsafe. A single `ci.yml` verifies every PR and, on push to `master`, deploys and tags.

**Tech Stack:** Java 25 (Temurin, via mise), Maven 3.9.16, Spring Boot 3.5.16 BOM, JUnit 4 + Spring Boot Test, GitHub Actions, GitHub Packages (Maven).

**Spec:** `docs/superpowers/specs/2026-09-09-github-packages-release-flow-design.md`

## Global Constraints

- **Version:** `14.0`, declared exactly once, in the root `pom.xml`. No module declares its own `<version>`.
- **Coordinates:** parent `by.nhorushko:crud-abstract-generic-parent`, library `by.nhorushko:crud-abstract-generic`.
- **Java:** `maven.compiler.release=25`. Maven and JDK come from mise — invoke as `mise exec -- mvn ...` until the wrapper exists (Task 4), then `./mvnw` is also valid.
- **Absorbed revision:** `filter-specifications-lib` tag `3.1-jakarta` only. Never `master`, never `3.2`/`3.3` — those are behaviour changes and belong to a later release.
- **Package name `by.nhorushko.filterspecification` must not change.** 57 files in LocatorServer and bi-dvr import it; renaming breaks them silently.
- **Nothing may reference JitPack** when the work is done: no `<repositories>`, no `jitpack.yml`, no `com.github.NikolayNN` dependency.
- **The agent never publishes.** No `mvn deploy` to a remote, no `git tag`, no `git push --force`. Deploys and tags are CI's job; pushes are the user's call.
- **Commit messages in English**, ending with the two attribution trailers used in this repository (see the last commit for the exact lines).
- **Test baseline:** `mise exec -- mvn -B verify` currently runs **72 tests in 23 classes** (library 64/18, test-application 8/5). After Task 2 it must run **115 tests in 33 classes**.

---

### Task 1: Absorb filter-specifications sources

Relocation only — no behaviour changes. The red/green cycle here is inverted from normal TDD: removing the dependency first makes compilation fail, and copying the sources makes it pass. The absorbed `FilterSpecificationUtilsTest` (12 tests) plus the existing `FilterFields*` tests are the safety net.

**Files:**
- Create: `library/src/main/java/by/nhorushko/filterspecification/` — 11 classes listed in Step 2
- Create: `library/src/test/java/by/nhorushko/filterspecification/FilterSpecificationUtilsTest.java`
- Modify: `library/pom.xml` — remove the `<repositories>` block (lines 28-33) and the `filter-specifications-lib` dependency (lines 82-86); add `jackson-annotations`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: package `by.nhorushko.filterspecification` inside the library jar, exporting `Converters`, `FilterCriteria`, `FilterOperation`, `FilterSpecificationAbstract`, `FilterSpecificationConstants`, `FilterSpecifications`, `FilterSpecificationUtils`, `FilterValue`, `FilterValues`, `PageRequestBuilder`, `SearchCriteria` — the same public API consumers already import.

- [ ] **Step 1: Fetch the exact revision**

Clone into `target/` so the checkout is gitignored and cannot be committed by accident.

```bash
cd D:/projects/crud-generic
rm -rf target/fsl-import
git clone --quiet https://github.com/NikolayNN/filter-specifications-lib.git target/fsl-import
git -C target/fsl-import checkout --quiet 3.1-jakarta
git -C target/fsl-import log -1 --format='%H %cs %s'
```

Expected: `... 2024-01-18 migrate from javax to jakarta`. If the date or subject differs, stop — the wrong revision is checked out.

- [ ] **Step 2: Verify the file set before copying**

```bash
find target/fsl-import/src/main -name '*.java' | sort
find target/fsl-import/src/test -name '*.java' | sort
```

Expected exactly 11 main classes — `Converters`, `FilterCriteria`, `FilterOperation`, `FilterSpecificationAbstract`, `FilterSpecificationConstants`, `FilterSpecifications`, `FilterSpecificationUtils`, `FilterValue`, `FilterValues`, `PageRequestBuilder`, `SearchCriteria` — and exactly 1 test class, `FilterSpecificationUtilsTest`. `FieldNameIterator` must **not** be present; if it is, the checkout is on `master` rather than the tag.

- [ ] **Step 3: Remove the dependency to make the build fail**

In `library/pom.xml`, delete the whole `<repositories>` element:

```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

and the dependency:

```xml
        <dependency>
            <groupId>com.github.NikolayNN</groupId>
            <artifactId>filter-specifications-lib</artifactId>
            <version>3.1-jakarta</version>
        </dependency>
```

- [ ] **Step 4: Run the build to verify it fails**

Run: `mise exec -- mvn -B -pl library compile`
Expected: FAIL — `package by.nhorushko.filterspecification does not exist`, reported for `FilterFields.java`, `AbsFlexPagingAndSortingService.java` and `PageableUtils.java`.

- [ ] **Step 5: Copy the sources in**

```bash
cd D:/projects/crud-generic
mkdir -p library/src/main/java/by/nhorushko/filterspecification
mkdir -p library/src/test/java/by/nhorushko/filterspecification
cp target/fsl-import/src/main/java/by/nhorushko/filterspecification/*.java \
   library/src/main/java/by/nhorushko/filterspecification/
cp target/fsl-import/src/test/java/by/nhorushko/filterspecification/FilterSpecificationUtilsTest.java \
   library/src/test/java/by/nhorushko/filterspecification/
```

Do not edit the copied files — not the formatting, not the imports, not the license headers. A verbatim copy is what makes this commit reviewable.

- [ ] **Step 6: Add the one dependency the absorbed code needs**

`FilterOperation` uses `com.fasterxml.jackson.annotation.JsonValue`. Add to `library/pom.xml`, in the same style as its neighbours (explicit version), next to the `commons-lang3` dependency:

```xml
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>2.21.4</version>
        </dependency>
```

Everything else the absorbed code imports — `jakarta.persistence.criteria.*`, `org.apache.commons.lang3.*`, `org.springframework.data.*`, `org.springframework.beans.factory.annotation.Autowired`, `org.springframework.stereotype.Service`, JUnit 4 — already resolves through `library`'s existing dependencies.

- [ ] **Step 7: Run the library tests**

Run: `mise exec -- mvn -B -pl library test`
Expected: PASS, `Tests run: 76` (64 existing + 12 from `FilterSpecificationUtilsTest`) across 19 classes.

- [ ] **Step 8: Confirm no JitPack reference survives in the module**

```bash
grep -rn "jitpack\|com.github.NikolayNN" library/pom.xml || echo "clean"
```

Expected: `clean`.

- [ ] **Step 9: Clean up the import checkout and commit**

```bash
cd D:/projects/crud-generic
rm -rf target/fsl-import
git add library/src/main/java/by/nhorushko/filterspecification \
        library/src/test/java/by/nhorushko/filterspecification \
        library/pom.xml
git status --short
```

Verify the staged set is exactly 12 new `.java` files plus `library/pom.xml`, then commit with subject `refactor: absorb filter-specifications-lib sources` and a body explaining that revision `3.1-jakarta` was taken verbatim, the package name is preserved so consumers keep compiling, and JitPack leaves the library module.

---

### Task 2: Root pom as the single source of version

This is the task that makes `/aurora-release` work at all. Without it `versions:set` updates the aggregator and leaves the library behind — verified experimentally during design.

**Files:**
- Modify: `pom.xml` — full rewrite, becomes parent
- Modify: `library/pom.xml` — gains `<parent>`, loses `groupId`/`version`, plugin versions move to parent
- Modify: `test-application/pom.xml` — parent changes from `spring-boot-starter-parent` to the root pom, gains failsafe

**Interfaces:**
- Consumes: the absorbed `by.nhorushko.filterspecification` package from Task 1.
- Produces: `by.nhorushko:crud-abstract-generic-parent:14.0` (packaging `pom`) as parent; `by.nhorushko:crud-abstract-generic` inheriting version `14.0`; a reactor where `mvn versions:set -DnewVersion=X` at the root updates every module.

- [ ] **Step 1: Record the baseline**

```bash
cd D:/projects/crud-generic
mise exec -- mvn -B clean verify 2>&1 | grep -E "^\[INFO\] Tests run:|BUILD"
```

Expected: two `Tests run:` summary lines totalling 72 tests, `BUILD SUCCESS`. Write the numbers down — Step 8 compares against them.

- [ ] **Step 2: Rewrite the root `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>by.nhorushko</groupId>
    <artifactId>crud-abstract-generic-parent</artifactId>
    <version>14.0</version>
    <packaging>pom</packaging>
    <name>crud-abstract-generic-parent</name>

    <modules>
        <module>library</module>
        <module>test-application</module>
    </modules>

    <properties>
        <maven.compiler.release>25</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <lombok.version>1.18.48</lombok.version>
        <spring-boot.version>3.5.16</spring-boot.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Replaces spring-boot-starter-parent for test-application: the module needs the
                 Boot BOM, but its parent must be this pom so the version stays in one place. -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.16.0</version>
                    <configuration>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                        </annotationProcessorPaths>
                        <compilerArgs>
                            <arg>-g</arg>
                            <arg>-parameters</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>3.6.0</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <version>3.6.0</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-source-plugin</artifactId>
                    <version>3.2.1</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-deploy-plugin</artifactId>
                    <version>3.1.4</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

</project>
```

- [ ] **Step 3: Point `library/pom.xml` at the parent**

Replace the identity block — currently `groupId` / `artifactId` / `name` / `version` — with:

```xml
    <parent>
        <groupId>by.nhorushko</groupId>
        <artifactId>crud-abstract-generic-parent</artifactId>
        <version>14.0</version>
    </parent>

    <artifactId>crud-abstract-generic</artifactId>
    <name>library</name>
```

Then delete the module's entire `<properties>` element. All five entries go: `maven.compiler.release`,
`project.build.sourceEncoding` and `project.reporting.outputEncoding` are inherited from the parent,
`github.global.server` belonged to the `site-maven-plugin` machinery Task 3 removes, and `lombok.version`
is inherited too — the `${lombok.version}` reference in the lombok dependency keeps resolving through
the parent.

In `<build><plugins>`, drop the `<version>` and `<configuration>` from `maven-compiler-plugin` and the `<version>` from `maven-surefire-plugin` and `maven-source-plugin` — all three are managed by the parent now. The compiler entry becomes:

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
```

and `maven-source-plugin` keeps only its `<executions>` block. Leave `site-maven-plugin`, `maven-deploy-plugin` and `distributionManagement` alone — Task 3 removes them.

- [ ] **Step 4: Move `test-application/pom.xml` onto the root parent**

Replace the `spring-boot-starter-parent` block with:

```xml
    <parent>
        <groupId>by.nhorushko</groupId>
        <artifactId>crud-abstract-generic-parent</artifactId>
        <version>14.0</version>
    </parent>

    <artifactId>crud-abstract-generic-test</artifactId>
    <name>test-application</name>
```

Delete the `groupId` and `version` lines, and the whole `<properties>` block (`java.version` and `lombok.version` are replaced by the parent's `maven.compiler.release` and `lombok.version`). Add the deploy skip and change the library dependency to inherit the reactor version:

```xml
    <properties>
        <!-- Demo application, not a published artifact. -->
        <maven.deploy.skip>true</maven.deploy.skip>
    </properties>
```

```xml
        <dependency>
            <groupId>by.nhorushko</groupId>
            <artifactId>crud-abstract-generic</artifactId>
            <version>${project.version}</version>
        </dependency>
```

- [ ] **Step 5: Turn on failsafe so the `*IT` tests actually run**

Replace the whole `<build>` section of `test-application/pom.xml` with:

```xml
    <build>
        <plugins>
            <!-- 9 *IT classes (31 tests) exist here but surefire only matches *Test/Test*/*Tests,
                 so before this they never executed and `mvn verify` was green without them. -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

The `maven-compiler-plugin` entry that was here is dropped: the parent's `pluginManagement` already configures the lombok annotation processor path for every module.

- [ ] **Step 6: Verify the reactor resolves**

Run: `mise exec -- mvn -B validate`
Expected: `BUILD SUCCESS`, and the reactor lists three projects — `crud-abstract-generic-parent`, `library`, `test-application`.

- [ ] **Step 7: Confirm the version really is single-sourced**

```bash
cd D:/projects/crud-generic
mise exec -- mvn -q versions:set -DnewVersion=14.1 -DgenerateBackupPoms=false
grep -n "14\.1" pom.xml library/pom.xml test-application/pom.xml
```

Expected: `14.1` appears in all three files — the root `<version>` and the `<parent>` block of each
module. The library dependency inside `test-application` stays `${project.version}` and needs no
rewriting, which is exactly the point of Step 4. Before the restructuring this command touched the root
pom only and left `library` behind.

Revert:

```bash
mise exec -- mvn -q versions:set -DnewVersion=14.0 -DgenerateBackupPoms=false
git diff --stat
```

Expected: no diff in the poms beyond what Steps 2-5 introduced.

- [ ] **Step 8: Run the full build and compare against the baseline**

Run: `mise exec -- mvn -B clean verify 2>&1 | grep -E "^\[INFO\] Tests run:|BUILD"`
Expected: `BUILD SUCCESS` with **115 tests in 33 classes** — 76 in library (Task 1), 8 surefire + 31 failsafe in test-application. If failsafe reports `Tests run: 0`, the `*IT` classes are not being matched; check that the failsafe execution binds `integration-test` and `verify`.

- [ ] **Step 9: Commit**

```bash
git add pom.xml library/pom.xml test-application/pom.xml
git status --short
```

Commit with subject `build: make root pom the single source of version` and a body noting the reactor restructuring, version `14.0`, the Boot BOM replacing `spring-boot-starter-parent` for the demo module, and that failsafe brings 31 previously-dead integration tests into `verify`.

---

### Task 3: Publish to GitHub Packages instead of JitPack

**Files:**
- Modify: `pom.xml` — add `distributionManagement`
- Modify: `library/pom.xml` — remove `distributionManagement`, `site-maven-plugin`, `maven-deploy-plugin` override
- Delete: `jitpack.yml`

**Interfaces:**
- Consumes: the parent/child reactor from Task 2.
- Produces: `mvn deploy` targets `https://maven.pkg.github.com/NikolayNN/crud-generic` under server id `github`; the published set is the parent pom plus `crud-abstract-generic` jar + sources-jar + pom.

- [ ] **Step 1: Add the deployment target to the root pom**

Insert after `</properties>` in `pom.xml`:

```xml
    <distributionManagement>
        <!-- Server id `github` must match the <server> entry in ~/.m2/settings.xml
             (CI generates it via actions/setup-java). See README, "Публикация". -->
        <repository>
            <id>github</id>
            <name>GitHub Packages</name>
            <url>https://maven.pkg.github.com/NikolayNN/crud-generic</url>
        </repository>
    </distributionManagement>
```

No `<snapshotRepository>`: nothing is published from `develop`.

- [ ] **Step 2: Strip the legacy publishing machinery from the library**

Delete from `library/pom.xml`, in full:

```xml
    <distributionManagement>
        <repository>
            <id>internal.repo</id>
            <name>Temporary Staging Repository</name>
            <url>file://${project.build.directory}/mvn-repo</url>
        </repository>
    </distributionManagement>
```

the `maven-deploy-plugin` entry carrying `<altDeploymentRepository>`, and the entire `com.github.github:site-maven-plugin` entry that pushed artifacts into the `mvn-repo` branch. The `maven-source-plugin` execution stays — sources-jar is wanted.

- [ ] **Step 3: Delete the JitPack descriptor**

```bash
cd D:/projects/crud-generic
git rm jitpack.yml
```

- [ ] **Step 4: Verify no JitPack trace remains anywhere**

```bash
grep -rn "jitpack\|com.github.NikolayNN\|mvn-repo\|internal.repo" --include=pom.xml --include=*.yml . || echo "clean"
```

Expected: `clean`. (README still mentions JitPack at this point — Task 6 rewrites it.)

- [ ] **Step 5: Trial deploy into a local directory**

This proves what would be published without touching a remote. It is a local file deploy, not a publish.

```bash
cd D:/projects/crud-generic
rm -rf target/staging
mise exec -- mvn -B clean deploy -DaltDeploymentRepository=local::file:./target/staging
find target/staging -type f \( -name '*.jar' -o -name '*.pom' \) | sort
```

Expected exactly:
- `by/nhorushko/crud-abstract-generic-parent/14.0/crud-abstract-generic-parent-14.0.pom`
- `by/nhorushko/crud-abstract-generic/14.0/crud-abstract-generic-14.0.pom`
- `by/nhorushko/crud-abstract-generic/14.0/crud-abstract-generic-14.0.jar`
- `by/nhorushko/crud-abstract-generic/14.0/crud-abstract-generic-14.0-sources.jar`

No `crud-abstract-generic-test` artifact may appear. If one does, `maven.deploy.skip` in `test-application` is not taking effect.

- [ ] **Step 6: Confirm the published pom carries no third-party repository**

```bash
grep -c "repositories" target/staging/by/nhorushko/crud-abstract-generic-parent/14.0/crud-abstract-generic-parent-14.0.pom || echo "0 — clean"
```

Expected: `0 — clean`. A `<repositories>` block here would push JitPack into every consumer's resolution — the exact outcome absorbing the sources was meant to avoid.

- [ ] **Step 7: Commit**

```bash
rm -rf target/staging
git add pom.xml library/pom.xml
git status --short
```

Commit with subject `build: publish to GitHub Packages instead of JitPack`, noting the removal of the `mvn-repo`-branch publishing plugin and that `test-application` is excluded from deployment.

---

### Task 4: Repo hygiene and agent guardrails

Ordered before CI deliberately: the workflow invokes `./mvnw`, which only exists after this task.

**Files:**
- Create: `.gitattributes`
- Create: `.mvn/maven.config`
- Create: `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` (generated)
- Create: `.claude/settings.json`
- Create: `.claude/hooks/guard-bash.sh`
- Create: `CLAUDE.md`

**Interfaces:**
- Consumes: the reactor from Task 2.
- Produces: `./mvnw` as the build entry point used by CI and the README.

- [ ] **Step 1: Create `.gitattributes`**

```
# Нормализация концов строк: в репозитории всегда LF, в рабочей копии тоже LF
# (кроме Windows-скриптов). Перекрывает глобальный core.autocrlf=true.
* text=auto eol=lf

# Windows-скрипты должны оставаться CRLF
*.cmd text eol=crlf
*.bat text eol=crlf

# Бинарники
*.jar binary
*.png binary
*.jpg binary
*.gif binary
*.ico binary
*.pdf binary
```

- [ ] **Step 2: Create `.mvn/maven.config`**

```
-ntp
-Daether.enhancedLocalRepository.trackingFilename=_ignored.repositories
```

The second flag matters here specifically: this repository's artifacts have been cached in the shared `~/.m2` under JitPack repository ids, and without it Maven fails with "cached from a remote repository ID that is unavailable" once the coordinates change.

- [ ] **Step 3: Generate the Maven wrapper**

```bash
cd D:/projects/crud-generic
mise exec -- mvn -N wrapper:wrapper -Dmaven=3.9.16 -Dtype=only-script
cat .mvn/wrapper/maven-wrapper.properties
```

Expected: `distributionType=only-script` and a `distributionUrl` ending in `apache-maven-3.9.16-bin.zip`. No `.jar` file may appear under `.mvn/wrapper/` — if one does, the type flag did not take.

- [ ] **Step 4: Verify the wrapper builds**

Run: `./mvnw -B -q validate`
Expected: exits 0.

- [ ] **Step 5: Create `.claude/hooks/guard-bash.sh`**

Adapted from `security-gateway-service`: there is no docker here, and tags are the thing that must not be created by hand.

```bash
#!/usr/bin/env bash
# PreToolUse-хук Claude Code для инструмента Bash.
# Не даёт агенту публиковать артефакты и ставить версионные теги: публикует CI
# (.github/workflows/ci.yml), релиз запускает человек через /aurora-release.
#
# Вход: JSON от Claude Code на stdin, команда лежит в .tool_input.command.
# Выход: exit 2 + текст в stderr — вызов заблокирован, текст показывается модели;
#        exit 0 — команда разрешена.
# jq на машине может отсутствовать, поэтому проверяем по сырому JSON.

input=$(cat)

has() { printf '%s' "$input" | grep -Eq "$1"; }

blocked=""
if has '(mvn|mvnw)[^"]*[[:space:]]deploy([[:space:]]|\\?"|$)'; then
    blocked="mvn deploy"
elif has '(^|[^[:alnum:]_./-])git[[:space:]]+tag([[:space:]]|\\?"|$)'; then
    blocked="git tag"
elif has '(^|[^[:alnum:]_./-])git[[:space:]]+push[^"]*(--force|-f)([[:space:]]|\\?"|$)'; then
    blocked="git push --force"
fi

if [ -n "$blocked" ]; then
    cat >&2 <<EOF
Заблокировано хуком .claude/hooks/guard-bash.sh: «$blocked».
Артефакты в GitHub Packages публикует CI на push в master, тег v<версия> ставит тот же
workflow. Релиз запускается человеком через /aurora-release, см. README.md
(раздел «Релиз и версии»).
Если команда нужна прямо сейчас, пользователь может выполнить её сам: «! <команда>».
EOF
    exit 2
fi

exit 0
```

Note the local deploy in Task 3 Step 5 uses `-DaltDeploymentRepository`, so it is caught by this hook too. That is intentional — after this task, a local trial deploy is run by the user with `! <команда>` if it is ever needed again.

- [ ] **Step 6: Create `.claude/settings.json`**

```json
{
  "permissions": {
    "allow": [
      "Bash(mise exec -- mvn:*)",
      "Bash(mise exec -- java:*)",
      "Bash(mise ls:*)",
      "Bash(./mvnw:*)",
      "Bash(git status:*)",
      "Bash(git log:*)",
      "Bash(git diff:*)",
      "Bash(git show:*)",
      "Bash(git branch:*)",
      "Bash(git ls-files:*)"
    ],
    "deny": [
      "Bash(mvn deploy:*)",
      "Bash(git tag:*)"
    ]
  },
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "shell": "bash",
            "command": "bash \"${CLAUDE_PROJECT_DIR:-.}/.claude/hooks/guard-bash.sh\"",
            "timeout": 10
          }
        ]
      }
    ]
  }
}
```

- [ ] **Step 7: Test that the hook actually blocks**

```bash
cd D:/projects/crud-generic
echo '{"tool_input":{"command":"./mvnw -B deploy"}}' | bash .claude/hooks/guard-bash.sh; echo "exit=$?"
echo '{"tool_input":{"command":"git tag v14.0"}}'    | bash .claude/hooks/guard-bash.sh; echo "exit=$?"
echo '{"tool_input":{"command":"./mvnw -B verify"}}' | bash .claude/hooks/guard-bash.sh; echo "exit=$?"
```

Expected: `exit=2` for the first two with the Russian block message on stderr, `exit=0` and no output for the third.

- [ ] **Step 8: Create `CLAUDE.md`**

```markdown
# crud-generic — заметки для агентов

Библиотека CRUD-абстракций для Spring Boot. Публикуется в GitHub Packages как
`by.nhorushko:crud-abstract-generic`. Потребители: LocatorServer, bi-dvr.

## Команды

- Сборка и тесты: `./mvnw -B verify` (или `mise exec -- mvn -B verify`)
- Только библиотека: `./mvnw -B -pl library test`
- JDK и Maven — из mise (`mise.toml`), системного `mvn` на PATH нет.

## Структура

- `library/` — публикуемый артефакт `crud-abstract-generic`
- `test-application/` — демо-приложение, **не публикуется**; несёт 31 интеграционный
  тест (`*IT`), которые гоняет failsafe
- Версия живёт **только** в корневом `pom.xml`, модули наследуют её через `<parent>`

## Чего не делать

- Не публиковать: `mvn deploy` выполняет CI на push в master (хук блокирует).
- Не ставить теги руками: `v<версия>` создаёт тот же workflow. Тег — выход, а не вход.
- Не поднимать версию перед релизом: версия в pom — та, что разрабатывается.
  Бамп делает `/aurora-release` после релиза.
- Пакет `by.nhorushko.filterspecification` переехал сюда из отдельной библиотеки;
  переименовывать его нельзя — на него завязаны 57 файлов в LocatorServer и bi-dvr.

Полный стандарт релизов — раздел «Docker image & release flow» в `~/.claude/CLAUDE.md`.
```

- [ ] **Step 9: Commit**

```bash
git add .gitattributes .mvn/maven.config .mvn/wrapper/maven-wrapper.properties mvnw mvnw.cmd .claude CLAUDE.md
git status --short
```

Confirm `mvnw` is staged with its executable bit (`git ls-files -s mvnw` shows mode `100755`); if not, `git update-index --chmod=+x mvnw`. Commit with subject `chore: repo hygiene and agent guardrails`.

---

### Task 5: CI — verify on PR, release from master

**Files:**
- Create: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: `./mvnw` from Task 4, `distributionManagement` server id `github` from Task 3, version `14.0` from Task 2.
- Produces: on push to `master` — artifacts in GitHub Packages, tag `v<version>`, and a GitHub Release built from the matching `CHANGELOG.md` section.

- [ ] **Step 1: Write the workflow**

```yaml
# CI библиотеки. Схема: README «Релиз и версии» и раздел «Docker image & release flow»
# в ~/.claude/CLAUDE.md.
#   PR в develop/master     -> только тесты (verify)
#   push в develop          -> только тесты, ничего не публикуется
#   push в master (= релиз) -> guard «версия ещё не выпущена», deploy в GitHub Packages,
#                              тег v<version> и GitHub Release из раздела CHANGELOG
# Версия берётся из pom.xml (единственный источник); теги vX.Y ставит только этот workflow.
# Дополнительных секретов не нужно: публикация идёт под GITHUB_TOKEN.
name: CI

on:
  pull_request:
    branches: [develop, master]
  push:
    branches: [develop, master]
  workflow_dispatch:

# Серия push в develop схлопывается до последнего запуска; релизы на master не отменяем.
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: ${{ github.ref != 'refs/heads/master' }}

jobs:
  verify:
    name: Tests
    runs-on: ubuntu-latest
    timeout-minutes: 20
    permissions:
      contents: read
    outputs:
      version: ${{ steps.version.outputs.version }}
    steps:
      - uses: actions/checkout@v7

      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: '25'
          cache: maven

      - name: Project version
        id: version
        run: echo "version=$(./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout)" >> "$GITHUB_OUTPUT"

      - name: Release guard, tag must not exist (master only)
        if: github.ref == 'refs/heads/master' && github.event_name != 'pull_request'
        run: |
          v='${{ steps.version.outputs.version }}'
          if git ls-remote --exit-code --tags origin "refs/tags/v$v" >/dev/null; then
            echo "::error::Version $v is already released (tag v$v exists). Bump the version on develop first (chore: start ...)."
            exit 1
          fi

      - name: Verify
        run: ./mvnw -B verify

  publish:
    name: Publish and tag
    needs: verify
    if: github.event_name != 'pull_request' && github.ref == 'refs/heads/master'
    runs-on: ubuntu-latest
    timeout-minutes: 20
    permissions:
      contents: write   # тег vX.Y и GitHub Release
      packages: write   # публикация в GitHub Packages
    env:
      VERSION: ${{ needs.verify.outputs.version }}
    steps:
      - uses: actions/checkout@v7

      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: '25'
          cache: maven
          # Генерирует ~/.m2/settings.xml с <server><id>github</id>, id совпадает
          # с distributionManagement в корневом pom.xml.
          server-id: github
          server-username: MAVEN_USERNAME
          server-password: MAVEN_TOKEN

      - name: Deploy to GitHub Packages
        env:
          MAVEN_USERNAME: ${{ github.actor }}
          MAVEN_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        # Тесты уже прошли в job verify, но deploy обязан собрать jar заново.
        run: ./mvnw -B deploy -DskipTests

      - name: Tag v<version>
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git tag -a "v$VERSION" -m "Release $VERSION"
          git push origin "v$VERSION"

      - name: GitHub Release from CHANGELOG
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          # Раздел "## <version>" из CHANGELOG.md до следующего заголовка "## "
          awk -v v="$VERSION" '
            $0 == "## " v { on = 1; next }
            on && /^## /    { exit }
            on              { print }
          ' CHANGELOG.md > release-notes.md
          if [ ! -s release-notes.md ]; then
            echo "::warning::Section '## $VERSION' not found in CHANGELOG.md"
            echo "См. CHANGELOG.md" > release-notes.md
          fi
          gh release create "v$VERSION" --title "$VERSION" --notes-file release-notes.md

      - name: Summary
        run: |
          {
            echo "### by.nhorushko:crud-abstract-generic:$VERSION"
            echo "- commit: ${GITHUB_SHA::7}"
            echo "- tag: v$VERSION"
            echo "- https://github.com/${GITHUB_REPOSITORY}/packages"
          } >> "$GITHUB_STEP_SUMMARY"
```

- [ ] **Step 2: Check the workflow parses**

```bash
cd D:/projects/crud-generic
python -c "import yaml,sys; yaml.safe_load(open('.github/workflows/ci.yml',encoding='utf-8')); print('yaml ok')"
```

Expected: `yaml ok`. If python is unavailable, `gh workflow view` after pushing serves the same purpose — but prefer catching it locally.

- [ ] **Step 3: Verify the release-notes extractor against a real CHANGELOG**

`CHANGELOG.md` does not exist yet (Task 6 creates it), so test the awk program against a fixture:

```bash
cd D:/projects/crud-generic
printf '# Changelog\n\n## Не выпущено\n\n## 14.0\n\n- first line\n- second line\n\n## 13.0\n\n- old\n' > target/changelog-fixture.md
awk -v v="14.0" '
  $0 == "## " v { on = 1; next }
  on && /^## /    { exit }
  on              { print }
' target/changelog-fixture.md
rm target/changelog-fixture.md
```

Expected: the two `- first line` / `- second line` entries and surrounding blank lines, and nothing from the `13.0` section.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/ci.yml
git status --short
```

Commit with subject `ci: verify on PR, release from master`, noting that the tag is an output of the workflow and that no repository secrets are required.

---

### Task 6: README and CHANGELOG

**Files:**
- Create: `CHANGELOG.md`
- Modify: `README.md` — replace the "Installation via Maven" section, add "Релиз и версии" and "CI (GitHub Actions)"

**Interfaces:**
- Consumes: everything above.
- Produces: the `## Не выпущено` / `## 14.0` structure that `/aurora-release` and the CI release-notes step both depend on.

- [ ] **Step 1: Create `CHANGELOG.md`**

```markdown
# Changelog

История версий библиотеки. Версии до 14.0 выпускались через JitPack и в этом файле
не восстанавливаются — их список остался в тегах репозитория.

## Не выпущено

## 14.0

### Публикация
- Библиотека переехала с JitPack в GitHub Packages. Новые координаты:
  `by.nhorushko:crud-abstract-generic` вместо `com.github.NikolayNN:crud-generic`.
  Потребителю нужен `<repository>` на `https://maven.pkg.github.com/NikolayNN/crud-generic`
  и PAT с правом `read:packages` в `~/.m2/settings.xml` — GitHub Packages требует
  аутентификации даже для публичных пакетов. Инструкция в README.
- Исходники `filter-specifications-lib` (ревизия `3.1-jakarta`) перенесены в этот
  репозиторий с сохранением пакета `by.nhorushko.filterspecification`. Импорты у
  потребителей не меняются, зависимость от JitPack исчезла полностью.
- Удалены `jitpack.yml`, публикация артефактов в ветку `mvn-repo` через
  `site-maven-plugin` и `distributionManagement` на локальную папку.

### Сборка
- Корневой `pom.xml` стал родителем обоих модулей и единственным местом, где живёт
  версия. Раньше `versions:set` в корне не доходил до `library`, и релиз опубликовал бы
  старую версию.
- `test-application` перешёл с `spring-boot-starter-parent` на импорт
  `spring-boot-dependencies` и помечен `maven.deploy.skip` — это демо, а не артефакт.
- Подключён `maven-failsafe-plugin`: 31 интеграционный тест в 9 классах `*IT` раньше
  не выполнялся вообще, потому что surefire их не видит, а failsafe не был настроен.
  `mvn verify` теперь гоняет 115 тестов вместо 72.
- Добавлены `.gitattributes`, `.mvn/maven.config` и Maven wrapper 3.9.16 (`only-script`).

### CI
- `.github/workflows/ci.yml`: PR и push в develop — только тесты; push в master —
  публикация в GitHub Packages, тег `v<версия>` и GitHub Release из раздела этого файла.
  Теги больше не ставятся руками. Дополнительных секретов не требуется.
- `.claude/settings.json` и хук `guard-bash.sh` запрещают агенту `mvn deploy`,
  `git tag` и `git push --force`.
```

- [ ] **Step 2: Replace the installation section of `README.md`**

The current section reads "### Installation via Maven" followed by the JitPack repository snippet, the `com.github.NikolayNN:crud-generic` dependency, and the line "Find the latest versions at: https://jitpack.io/#NikolayNN/crud-generic". Replace all of it with:

````markdown
### Installation via Maven

The library is published to GitHub Packages. **GitHub Packages requires authentication even
for reading public packages**, so a token is needed once per machine and per CI runner.

1. Create a personal access token with the `read:packages` scope.
2. Add it to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github-crud-generic</id>
      <username>YOUR_GITHUB_LOGIN</username>
      <password>YOUR_TOKEN_WITH_read:packages</password>
    </server>
  </servers>
</settings>
```

3. Add the repository and the dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github-crud-generic</id>
        <url>https://maven.pkg.github.com/NikolayNN/crud-generic</url>
    </repository>
</repositories>

<dependency>
    <groupId>by.nhorushko</groupId>
    <artifactId>crud-abstract-generic</artifactId>
    <version>14.0</version>
</dependency>
```

The `<id>` in `settings.xml` and in `<repositories>` must match.

Released versions are listed at https://github.com/NikolayNN/crud-generic/packages

**Migrating from JitPack (versions up to 13.3.15-jakarta):** the coordinates changed from
`com.github.NikolayNN:crud-generic` to `by.nhorushko:crud-abstract-generic`, and the
`jitpack.io` repository is no longer needed. Java imports do not change — including
`by.nhorushko.filterspecification.*`, whose sources now live in this library.
````

- [ ] **Step 3: Append the release and CI sections to `README.md`**

```markdown
## Релиз и версии

Версия в корневом `pom.xml` — это версия, **которая сейчас разрабатывается**. Релиз её не
меняет: коммит `chore: release X.Y` закрывает раздел changelog, master fast-forward'ится на
него, и сразу после этого develop переводится на следующую версию (`chore: start X.Y+1`).

Релиз запускается командой `/aurora-release` из ветки `develop` при чистом дереве. Дальше
всё делает CI: тесты, публикация `by.nhorushko:crud-abstract-generic:X.Y` в GitHub Packages,
тег `vX.Y` и GitHub Release из раздела `## X.Y` в `CHANGELOG.md`.

Теги руками не ставятся: тег — это результат релиза, а не его вход. Версию перед релизом
поднимать не нужно.

## CI (GitHub Actions)

`.github/workflows/ci.yml`:

| Событие | Что происходит |
|---|---|
| PR в `develop`/`master` | `./mvnw -B verify` |
| push в `develop` | `./mvnw -B verify`, ничего не публикуется |
| push в `master` | проверка «тег `vX.Y` ещё не существует», verify, deploy в GitHub Packages, тег `vX.Y`, GitHub Release |

Секретов заводить не нужно — публикация идёт под `GITHUB_TOKEN`.
```

- [ ] **Step 4: Check the README has no stale JitPack references**

```bash
cd D:/projects/crud-generic
grep -n "jitpack\|com.github.NikolayNN" README.md || echo "clean"
```

Expected: `clean` apart from the one intentional mention inside the "Migrating from JitPack" note. Review each hit and remove anything that still instructs a reader to use JitPack.

- [ ] **Step 5: Check the prerequisites section is still accurate**

The README's Prerequisites section states "JDK 25+" and "Spring Boot 3.5+". The root pom sets
`maven.compiler.release=25` and `spring-boot.version=3.5.16`, so both are correct and stay as they are.
If a previous task changed either property, update the README to match the pom.

- [ ] **Step 6: Commit**

```bash
git add README.md CHANGELOG.md
git status --short
```

Commit with subject `docs: README and CHANGELOG for the new release flow`.

---

### Task 7: Hand off for the first release

The remaining actions push to a remote and publish. **Do not perform them autonomously** — present the state and let the user decide.

- [ ] **Step 1: Run the full build one last time**

Run: `./mvnw -B clean verify 2>&1 | grep -E "^\[INFO\] Tests run:|BUILD"`
Expected: `BUILD SUCCESS`, 115 tests.

- [ ] **Step 2: Show the user what is ready**

Report: the six commits on `develop`, the measured test delta (72 → 115), the trial-deploy artifact list from Task 3, and the three things that need a human:

1. `git push -u origin develop` — first push of the branch; CI runs `verify` and nothing else.
2. Make `develop` the default branch in GitHub settings, so PRs target it.
3. `/aurora-release 14.0` — the actual release. It closes the changelog section, fast-forwards `master`, bumps `develop` to `14.1`, and pushes; CI then publishes and tags.

- [ ] **Step 3: After the release, report the follow-ups**

- Archive `NikolayNN/filter-specifications-lib` on GitHub so nothing re-adds the dependency.
- Delete the now-dead `mvn-repo`, `javax` and dependabot branches on origin (user's call).
- Update `LocatorServer` and `bi-dvr` to the new coordinates — separate work in those repositories. Their Java imports do not change; their poms and `settings.xml` do.
- Consider pulling in the `3.2`/`3.3-jakarta` filter fixes (polymorphic `getPath`, nested collections) as a later release with its own changelog entry.
