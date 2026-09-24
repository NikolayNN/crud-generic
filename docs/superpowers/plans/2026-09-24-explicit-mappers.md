# Explicit Mappers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace ModelMapper in `crud-generic` with explicit, registry-dispatched mappers (`Mapper` / `Updater` / `MapperRegistry` / `AbsFlexMapConfig`), replace `updatePartial` with `patch(id, body)` + `changeEntity`, and ship it as version 15.0.

**Architecture:** A `MapperRegistry` is built once from every `Mapper`, `Updater` and `MapperSource` bean and looks pairs up by the exact `(fromClass, toClass)` key; the only normalisation maps JPA proxies to their nearest `@Entity` ancestor. The `AbsMapper` facade (renamed from `AbsModelMapper`) resolves the registry lazily through a supplier, which breaks the bean cycle *mapper → AbsMapper → registry → mappers*. Services write through one pipeline with two seams, `loadForUpdate(id)` and `saveUpdated(entity)`.

**Tech Stack:** Java 25 (Temurin via mise), Maven 3.9.16 (`./mvnw`), Spring Boot 3.5.16 BOM, Spring Data JPA, Hibernate 6.6, JUnit 4 + Mockito 5 (library), JUnit 5 + AssertJ + H2 (test-application).

**Spec:** `docs/superpowers/specs/2026-09-24-explicit-mappers-design.md` — read it alongside this plan; the plan argues from it.

## Global Constraints

- **Version `15.0`**, declared only in the root `pom.xml` (modules inherit it via `<parent>`). Set with `mise exec -- mvn -q versions:set -DnewVersion=15.0 -DgenerateBackupPoms=false`.
- **Packages:** `Mapper`, `Updater`, `MapperSource`, `MapperRegistry`, `Patches`, `AbsFlexMapConfig`, `AbsMapEntityToDto`, `AbsMapDtoToEntity`, `AbsMapUpdateDtoToEntity`, `AbsMapperExtRelation` live in `by.nhorushko.crudgeneric.flex.mapper`. The facade `AbsMapper` stays where `AbsModelMapper` was: `by.nhorushko.crudgeneric.flex`. `MappingNotFoundException` lives in `by.nhorushko.crudgeneric.flex.exception`. Sub-packages `mapper.core`, `mapper.composite`, `mapper.mapper` disappear.
- **No implicit mapping anywhere:** no ModelMapper, no reflection (`FieldUtils`, `java.lang.reflect`) in the flex mapping code, no lookup up a class hierarchy or through interfaces. The only normalisation: a class without `@Entity` that has an `@Entity` ancestor becomes the nearest such ancestor.
- **Error messages (exact formats):** miss — `No Mapper registered for <from FQCN> -> <to FQCN>` / `No Updater registered for ...`; duplicate — `Duplicate Mapper for <from> -> <to>: <first origin class> and <second origin class>`; null class — `IllegalStateException` naming the origin's class.
- **Untouched:** `by.nhorushko.filterspecification` (57 consumer files import it), `flex.pageable` except the mapper type, `flex.controller`, `commons-lang3` dependency.
- **Library tests are JUnit 4** (`org.junit.Test`, `org.junit.Assert.*`, `assertThrows` from JUnit 4.13). **test-application tests are JUnit 5 + AssertJ.** `*IT` classes run under failsafe, `*Test` under surefire.
- **Build:** `./mvnw -B verify` (whole reactor) or `./mvnw -B -pl library verify` (library only). JDK and Maven come from mise; there is no system `mvn`.
- **The agent never publishes:** no `mvn deploy`, no version tags, no force-push. The project hook blocks any Bash command whose JSON contains "git" next to "tag" — never write that pair, not even in a command description.
- **Commits:** exactly four on top of `develop` when done — `chore: start 15.0`, `feat(mapper): explicit Mapper/Updater registry`, `feat!: replace ModelMapper with explicit mappers`, `docs: README, CHANGELOG and CLAUDE.md for explicit mappers`. Tasks 3 and 4 make local `wip:` commits (the reactor does not compile between them); Task 5 squashes them into the third commit with `git reset --soft`. Every commit message is in English and ends with the line `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Test baseline (before Task 1):** library 76 unit tests; test-application 8 unit + 31 IT. Expected after each task is stated in its last test step.

## Review Focus

1. **A real Hibernate proxy** (lazy association, `getReference`) as a `Mapper` source or an `Updater` destination must resolve to the entity's pair — unit tests only model it with a hand-written subclass. Pinned by `HibernateProxyMappingIT` (Task 5).
2. **A consumer mapper bean behind a CGLIB proxy** (an aspect, `@Transactional`) must register under its declared pair: the proxy instance's own fields are never initialised, so `fromClass()` / `toClass()` / `map()` in the bases must stay overridable, never `final`. Pinned by `MapperBasesCglibProxyTest` (Task 3).
3. **A config whose `patches()` declares its own `UPDATE_DTO` class** (or the same body class twice) must fail startup naming the config, never silently overwrite the update updater. Pinned in `AbsFlexMapConfigTest` (Task 3).
4. **`update(dto)` called with a subclass instance of `UPDATE_DTO`** (in LocatorServer the read DTO `Carrier extends CarrierUpdate`) must fail with `MappingNotFoundException` and save nothing — never borrow the parent's updater. Pinned in `AbsFlexServiceRUDPatchTest` (Task 4).
5. **`changeEntity` whose change throws** must save nothing and skip the after-hooks. Pinned in `AbsFlexServiceRUDPatchTest` (Task 4).

---

### Task 1: Rename the cycle to 15.0

The 14.1 cycle is empty (`## Не выпущено` has no entries), so the first commit only moves the version.

**Files:**
- Modify: `pom.xml` (project `<version>`), `library/pom.xml` and `test-application/pom.xml` (`<parent><version>`)

**Interfaces:**
- Consumes: nothing.
- Produces: version `15.0` in the reactor.

- [ ] **Step 1: Set the version**

```bash
cd /d/projects/crud-generic
mise exec -- mvn -q versions:set -DnewVersion=15.0 -DgenerateBackupPoms=false
grep -n "<version>15.0</version>" pom.xml library/pom.xml test-application/pom.xml
git status --short
```

Expected: three grep hits (one per pom) and exactly those three files modified. If `versions:set` is unavailable offline, edit the three `<version>14.1</version>` lines by hand — nothing else changes.

- [ ] **Step 2: Build**

Run: `./mvnw -B verify`
Expected: `BUILD SUCCESS`; library `Tests run: 76`, test-application surefire `Tests run: 8`, failsafe `Tests run: 31`.

- [ ] **Step 3: Commit**

```bash
git add pom.xml library/pom.xml test-application/pom.xml
git commit -F - <<'EOF'
chore: start 15.0

The 14.1 cycle released nothing. Replacing ModelMapper with explicit
mappers breaks the mapping API, so the cycle becomes 15.0.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 2: Mapper / Updater registry (additive)

Everything here is new; ModelMapper stays in place and nothing uses the registry yet.

**Files:**
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/exception/MappingNotFoundException.java`
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/Mapper.java`
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/Updater.java`
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/MapperSource.java`
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/MapperRegistry.java`
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/Patches.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/MapperRegistryTest.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/MapperAndUpdaterOfTest.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/PatchesTest.java`

**Interfaces:**
- Consumes: `jakarta.persistence.Entity` (library dependency `jakarta.persistence-api`, scope `provided`, already present).
- Produces:
  - `interface Mapper<FROM, TO>`: `Class<FROM> fromClass()`, `Class<TO> toClass()`, `TO map(FROM from)`, `static <F, T> Mapper<F, T> of(Class<F> from, Class<T> to, Function<F, T> fn)`.
  - `interface Updater<FROM, TO>`: `Class<FROM> fromClass()`, `Class<TO> toClass()`, `void update(FROM from, TO into)`, `static <F, T> Updater<F, T> of(Class<F> from, Class<T> to, BiConsumer<F, T> fn)`.
  - `interface MapperSource`: `Collection<Mapper<?, ?>> mappers()`, `Collection<Updater<?, ?>> updaters()`.
  - `class MapperRegistry`: `MapperRegistry(Collection<? extends Mapper<?, ?>>, Collection<? extends Updater<?, ?>>, Collection<? extends MapperSource>)`, `<F, T> Optional<Mapper<F, T>> findMapper(Class<F>, Class<T>)`, `<F, T> Optional<Updater<F, T>> findUpdater(Class<F>, Class<T>)`, `<F, T> Mapper<F, T> getMapper(Class<F>, Class<T>)`, `<F, T> Updater<F, T> getUpdater(Class<F>, Class<T>)` (the `get*` throw `MappingNotFoundException`).
  - `final class Patches<ENTITY>`: package-private `Patches(Class<ENTITY>)`, `public <P> Patches<ENTITY> add(Class<P> patchClass, BiConsumer<P, ENTITY> apply)`, package-private `List<Updater<?, ENTITY>> updaters()`.
  - `class MappingNotFoundException extends RuntimeException`: `MappingNotFoundException(String kind, Class<?> from, Class<?> to)`.

- [ ] **Step 1: Write the failing registry test**

`library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/MapperRegistryTest.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import jakarta.persistence.Entity;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MapperRegistryTest {

    @Test
    public void findsMapperByExactPair() {
        Mapper<BaseDto, BaseEntity> mapper = Mapper.of(BaseDto.class, BaseEntity.class, dto -> new BaseEntity());

        MapperRegistry registry = new MapperRegistry(List.of(mapper), List.of(), List.of());

        assertSame(mapper, registry.getMapper(BaseDto.class, BaseEntity.class));
        assertSame(mapper, registry.findMapper(BaseDto.class, BaseEntity.class).orElseThrow());
    }

    /**
     * No walking up the hierarchy: in LocatorServer Carrier extends CarrierUpdate extends
     * CarrierCreate, and a pair for the parent must never serve the child.
     */
    @Test
    public void subclassOfRegisteredSourceIsNotFound() {
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(BaseDto.class, BaseEntity.class, dto -> new BaseEntity())), List.of(), List.of());

        assertFalse(registry.findMapper(SubDto.class, BaseEntity.class).isPresent());
        MappingNotFoundException e = assertThrows(MappingNotFoundException.class,
                () -> registry.getMapper(SubDto.class, BaseEntity.class));
        assertEquals("No Mapper registered for " + SubDto.class.getName() + " -> " + BaseEntity.class.getName(),
                e.getMessage());
    }

    @Test
    public void interfacesOfTheSourceAreNotConsidered() {
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(Marker.class, BaseEntity.class, marker -> new BaseEntity())), List.of(), List.of());

        assertFalse(registry.findMapper(MarkedDto.class, BaseEntity.class).isPresent());
    }

    @Test
    public void proxyLikeSubclassIsNormalisedToItsEntityForMapperSource() {
        Mapper<BaseEntity, OtherDto> mapper = Mapper.of(BaseEntity.class, OtherDto.class, entity -> new OtherDto());

        MapperRegistry registry = new MapperRegistry(List.of(mapper), List.of(), List.of());

        assertSame(mapper, registry.getMapper(BaseEntityProxy.class, OtherDto.class));
    }

    @Test
    public void proxyLikeSubclassIsNormalisedForUpdaterSourceAndDestination() {
        Updater<OtherDto, BaseEntity> intoEntity = Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { });
        Updater<BaseEntity, OtherEntity> fromEntity = Updater.of(BaseEntity.class, OtherEntity.class, (entity, other) -> { });

        MapperRegistry registry = new MapperRegistry(List.of(), List.of(intoEntity, fromEntity), List.of());

        assertSame(intoEntity, registry.getUpdater(OtherDto.class, BaseEntityProxy.class));
        assertSame(fromEntity, registry.getUpdater(BaseEntityProxy.class, OtherEntity.class));
    }

    @Test
    public void entitySubclassOfEntityKeepsItsOwnKey() {
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(BaseEntity.class, OtherDto.class, entity -> new OtherDto())), List.of(), List.of());

        assertFalse(registry.findMapper(SubEntity.class, OtherDto.class).isPresent());
    }

    @Test
    public void duplicatePairFailsWithBothImplementations() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(new FirstBaseMapper(), new SecondBaseMapper()), List.of(), List.of()));

        assertTrue(e.getMessage(), e.getMessage().startsWith("Duplicate Mapper for "
                + BaseDto.class.getName() + " -> " + BaseEntity.class.getName()));
        assertTrue(e.getMessage(), e.getMessage().contains(FirstBaseMapper.class.getName()));
        assertTrue(e.getMessage(), e.getMessage().contains(SecondBaseMapper.class.getName()));
    }

    @Test
    public void duplicateBetweenBeanAndSourceNamesTheSource() {
        Updater<OtherDto, BaseEntity> bean = Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { });
        StubSource source = new StubSource(List.of(),
                List.of(Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { })));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(), List.of(bean), List.of(source)));

        assertTrue(e.getMessage(), e.getMessage().startsWith("Duplicate Updater for "));
        assertTrue(e.getMessage(), e.getMessage().contains(StubSource.class.getName()));
    }

    @Test
    public void mapperWithNullClassFailsWithItsClassName() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(new NullClassMapper()), List.of(), List.of()));

        assertTrue(e.getMessage(), e.getMessage().contains(NullClassMapper.class.getName()));
    }

    @Test
    public void updaterWithNullClassFailsWithItsClassName() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(), List.of(new NullClassUpdater()), List.of()));

        assertTrue(e.getMessage(), e.getMessage().contains(NullClassUpdater.class.getName()));
    }

    @Test
    public void missingUpdaterThrowsWithDirectionAndBothClassNames() {
        MapperRegistry registry = new MapperRegistry(List.of(), List.of(), List.of());

        MappingNotFoundException e = assertThrows(MappingNotFoundException.class,
                () -> registry.getUpdater(OtherDto.class, BaseEntity.class));

        assertEquals("No Updater registered for " + OtherDto.class.getName() + " -> " + BaseEntity.class.getName(),
                e.getMessage());
    }

    @Test
    public void mapperSourceIsUnwrapped() {
        Mapper<BaseDto, BaseEntity> mapper = Mapper.of(BaseDto.class, BaseEntity.class, dto -> new BaseEntity());
        Updater<OtherDto, BaseEntity> updater = Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { });

        MapperRegistry registry = new MapperRegistry(List.of(), List.of(),
                List.of(new StubSource(List.of(mapper), List.of(updater))));

        assertSame(mapper, registry.getMapper(BaseDto.class, BaseEntity.class));
        assertSame(updater, registry.getUpdater(OtherDto.class, BaseEntity.class));
    }

    /** Two configs for one entity, one per DTO set, register different pairs and coexist. */
    @Test
    public void twoSourcesForOneEntityWithDifferentDtosCoexist() {
        StubSource first = new StubSource(
                List.of(Mapper.of(BaseDto.class, BaseEntity.class, dto -> new BaseEntity()),
                        Mapper.of(BaseEntity.class, OtherDto.class, entity -> new OtherDto())),
                List.of(Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { })));
        StubSource second = new StubSource(
                List.of(Mapper.of(SubDto.class, BaseEntity.class, dto -> new BaseEntity()),
                        Mapper.of(BaseEntity.class, MarkedDto.class, entity -> new MarkedDto())),
                List.of(Updater.of(MarkedDto.class, BaseEntity.class, (dto, entity) -> { })));

        MapperRegistry registry = new MapperRegistry(List.of(), List.of(), List.of(first, second));

        assertTrue(registry.findMapper(BaseDto.class, BaseEntity.class).isPresent());
        assertTrue(registry.findMapper(SubDto.class, BaseEntity.class).isPresent());
        assertTrue(registry.findMapper(BaseEntity.class, OtherDto.class).isPresent());
        assertTrue(registry.findMapper(BaseEntity.class, MarkedDto.class).isPresent());
        assertTrue(registry.findUpdater(OtherDto.class, BaseEntity.class).isPresent());
        assertTrue(registry.findUpdater(MarkedDto.class, BaseEntity.class).isPresent());
    }

    static class BaseDto {
    }

    static class SubDto extends BaseDto {
    }

    interface Marker {
    }

    static class MarkedDto implements Marker {
    }

    static class OtherDto {
    }

    @Entity
    static class BaseEntity {
    }

    /** Models a Hibernate proxy: an unannotated runtime subclass of an entity. */
    static class BaseEntityProxy extends BaseEntity {
    }

    @Entity
    static class SubEntity extends BaseEntity {
    }

    @Entity
    static class OtherEntity {
    }

    static class FirstBaseMapper implements Mapper<BaseDto, BaseEntity> {
        @Override
        public Class<BaseDto> fromClass() {
            return BaseDto.class;
        }

        @Override
        public Class<BaseEntity> toClass() {
            return BaseEntity.class;
        }

        @Override
        public BaseEntity map(BaseDto from) {
            return new BaseEntity();
        }
    }

    static class SecondBaseMapper extends FirstBaseMapper {
    }

    static class NullClassMapper extends FirstBaseMapper {
        @Override
        public Class<BaseEntity> toClass() {
            return null;
        }
    }

    static class NullClassUpdater implements Updater<OtherDto, BaseEntity> {
        @Override
        public Class<OtherDto> fromClass() {
            return null;
        }

        @Override
        public Class<BaseEntity> toClass() {
            return BaseEntity.class;
        }

        @Override
        public void update(OtherDto from, BaseEntity into) {
        }
    }

    static class StubSource implements MapperSource {
        private final List<Mapper<?, ?>> mappers;
        private final List<Updater<?, ?>> updaters;

        StubSource(List<Mapper<?, ?>> mappers, List<Updater<?, ?>> updaters) {
            this.mappers = mappers;
            this.updaters = updaters;
        }

        @Override
        public Collection<Mapper<?, ?>> mappers() {
            return mappers;
        }

        @Override
        public Collection<Updater<?, ?>> updaters() {
            return updaters;
        }
    }
}
```

- [ ] **Step 2: Write the failing `of` and `Patches` tests**

`library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/MapperAndUpdaterOfTest.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MapperAndUpdaterOfTest {

    @Test
    public void mapperOfDeclaresItsClassesAndDelegates() {
        Mapper<String, Integer> length = Mapper.of(String.class, Integer.class, String::length);

        assertEquals(String.class, length.fromClass());
        assertEquals(Integer.class, length.toClass());
        assertEquals(Integer.valueOf(5), length.map("hello"));
    }

    @Test
    public void updaterOfDeclaresItsClassesAndDelegates() {
        Updater<String, StringBuilder> append = Updater.of(String.class, StringBuilder.class, (s, sb) -> sb.append(s));
        StringBuilder target = new StringBuilder("a");

        append.update("b", target);

        assertEquals(String.class, append.fromClass());
        assertEquals(StringBuilder.class, append.toClass());
        assertEquals("ab", target.toString());
    }

    @Test
    public void ofRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> Mapper.<String, Integer>of(null, Integer.class, String::length));
        assertThrows(NullPointerException.class, () -> Mapper.<String, Integer>of(String.class, null, String::length));
        assertThrows(NullPointerException.class, () -> Mapper.<String, Integer>of(String.class, Integer.class, null));
        assertThrows(NullPointerException.class,
                () -> Updater.<String, StringBuilder>of(null, StringBuilder.class, (s, sb) -> sb.append(s)));
        assertThrows(NullPointerException.class,
                () -> Updater.<String, StringBuilder>of(String.class, null, (s, sb) -> sb.append(s)));
        assertThrows(NullPointerException.class,
                () -> Updater.<String, StringBuilder>of(String.class, StringBuilder.class, null));
    }
}
```

`library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/PatchesTest.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PatchesTest {

    @Test
    public void addDeclaresOneUpdaterPerBodyClassAgainstTheEntity() {
        Patches<Target> patches = new Patches<>(Target.class);

        patches.add(NamePatch.class, (patch, target) -> target.name = patch.name())
                .add(CodePatch.class, (patch, target) -> target.code = patch.code());

        List<Updater<?, Target>> updaters = patches.updaters();
        assertEquals(2, updaters.size());
        assertEquals(NamePatch.class, updaters.get(0).fromClass());
        assertEquals(CodePatch.class, updaters.get(1).fromClass());
        assertEquals(Target.class, updaters.get(0).toClass());
        assertEquals(Target.class, updaters.get(1).toClass());
    }

    @Test
    public void declaredUpdaterAppliesTheFunction() {
        Patches<Target> patches = new Patches<>(Target.class);
        patches.add(NamePatch.class, (patch, target) -> target.name = patch.name());
        Target target = new Target();

        MapperRegistry registry = new MapperRegistry(List.of(), patches.updaters(), List.of());
        registry.getUpdater(NamePatch.class, Target.class).update(new NamePatch("new"), target);

        assertEquals("new", target.name);
    }

    @Test
    public void addRejectsNullArguments() {
        Patches<Target> patches = new Patches<>(Target.class);

        assertThrows(NullPointerException.class, () -> patches.add(null, (Object patch, Target target) -> { }));
        assertThrows(NullPointerException.class, () -> patches.add(NamePatch.class, null));
    }

    record NamePatch(String name) {
    }

    record CodePatch(String code) {
    }

    static class Target {
        String name;
        String code;
    }
}
```

- [ ] **Step 3: Run the tests to see them fail**

Run: `./mvnw -B -pl library test`
Expected: `COMPILATION ERROR` — `cannot find symbol` for `Mapper`, `Updater`, `MapperRegistry`, `Patches`, `MappingNotFoundException`.

- [ ] **Step 4: Implement the exception and the interfaces**

`library/src/main/java/by/nhorushko/crudgeneric/flex/exception/MappingNotFoundException.java`:

```java
package by.nhorushko.crudgeneric.flex.exception;

/**
 * Thrown when no {@code Mapper} or {@code Updater} is registered for the exact pair of classes a
 * mapping call needs. The message names the kind, the direction and both fully qualified class
 * names, e.g. {@code No Updater registered for com.x.OrderUpdateDto -> com.x.OrderEntity}.
 */
public class MappingNotFoundException extends RuntimeException {

    public MappingNotFoundException(String kind, Class<?> from, Class<?> to) {
        super(String.format("No %s registered for %s -> %s", kind, from.getName(), to.getName()));
    }
}
```

`library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/Mapper.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import java.util.Objects;
import java.util.function.Function;

/**
 * Explicit conversion of a {@code FROM} instance into a new {@code TO} instance.
 * <p>
 * The pair of classes is declared, not inferred from generics: {@link MapperRegistry} keys every
 * mapper by {@code (fromClass(), toClass())} and finds it by the exact runtime class of the source.
 * </p>
 *
 * @param <FROM> the source type
 * @param <TO>   the produced type
 */
public interface Mapper<FROM, TO> {

    Class<FROM> fromClass();

    Class<TO> toClass();

    TO map(FROM from);

    /**
     * Wraps a function as a mapper for the given pair of classes.
     */
    static <F, T> Mapper<F, T> of(Class<F> from, Class<T> to, Function<F, T> fn) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(fn, "fn");
        return new Mapper<>() {
            @Override
            public Class<F> fromClass() {
                return from;
            }

            @Override
            public Class<T> toClass() {
                return to;
            }

            @Override
            public T map(F source) {
                return fn.apply(source);
            }
        };
    }
}
```

`library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/Updater.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Explicit in-place write of a {@code FROM} instance onto an existing {@code TO} instance, typically
 * an update DTO or a PATCH body onto a managed entity. Only what the updater writes changes.
 * <p>
 * Keyed in {@link MapperRegistry} by {@code (fromClass(), toClass())}, looked up by the exact
 * runtime classes of both instances.
 * </p>
 *
 * @param <FROM> the source type
 * @param <TO>   the type written into
 */
public interface Updater<FROM, TO> {

    Class<FROM> fromClass();

    Class<TO> toClass();

    void update(FROM from, TO into);

    /**
     * Wraps a function as an updater for the given pair of classes.
     */
    static <F, T> Updater<F, T> of(Class<F> from, Class<T> to, BiConsumer<F, T> fn) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(fn, "fn");
        return new Updater<>() {
            @Override
            public Class<F> fromClass() {
                return from;
            }

            @Override
            public Class<T> toClass() {
                return to;
            }

            @Override
            public void update(F source, T into) {
                fn.accept(source, into);
            }
        };
    }
}
```

`library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/MapperSource.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import java.util.Collection;

/**
 * A bean that contributes several mappers and updaters at once. {@link MapperRegistry} unwraps it
 * when it is built; the entity mapping config implements it.
 */
public interface MapperSource {

    Collection<Mapper<?, ?>> mappers();

    Collection<Updater<?, ?>> updaters();
}
```

- [ ] **Step 5: Implement the registry and `Patches`**

`library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/MapperRegistry.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import jakarta.persistence.Entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Every {@link Mapper} and {@link Updater} of the application, keyed by the exact pair
 * {@code (fromClass, toClass)}.
 * <p>
 * Built once from the mapper beans, the updater beans and the {@link MapperSource} beans, whose
 * contents are unwrapped. A pair registered twice, or a mapper or updater that returns {@code null}
 * from {@code fromClass()} or {@code toClass()}, fails the build with {@link IllegalStateException}
 * naming the culprit. After the build the pairs never change.
 * </p>
 * <p>
 * Lookup uses the exact key: no walking up superclasses, no interfaces. For a DTO hierarchy register
 * one pair per concrete class. The only normalisation is for JPA proxies: a class that is not
 * annotated {@link Entity @Entity} but has an {@code @Entity} ancestor is replaced by the nearest such
 * ancestor, so Hibernate proxies resolve to their entity. An {@code @Entity} subclass of an
 * {@code @Entity} class keeps its own key. The rule applies to both classes of every lookup.
 * </p>
 */
public class MapperRegistry {

    private static final String MAPPER = "Mapper";
    private static final String UPDATER = "Updater";

    private final Map<Pair, Mapper<?, ?>> mappers = new HashMap<>();
    private final Map<Pair, Updater<?, ?>> updaters = new HashMap<>();
    private final ConcurrentMap<Class<?>, Class<?>> normalised = new ConcurrentHashMap<>();

    public MapperRegistry(Collection<? extends Mapper<?, ?>> mapperBeans,
                          Collection<? extends Updater<?, ?>> updaterBeans,
                          Collection<? extends MapperSource> sources) {
        Map<Pair, Object> mapperOrigins = new HashMap<>();
        Map<Pair, Object> updaterOrigins = new HashMap<>();
        for (Mapper<?, ?> mapper : mapperBeans) {
            register(mappers, mapperOrigins, MAPPER, mapper.fromClass(), mapper.toClass(), mapper, mapper);
        }
        for (Updater<?, ?> updater : updaterBeans) {
            register(updaters, updaterOrigins, UPDATER, updater.fromClass(), updater.toClass(), updater, updater);
        }
        for (MapperSource source : sources) {
            for (Mapper<?, ?> mapper : source.mappers()) {
                register(mappers, mapperOrigins, MAPPER, mapper.fromClass(), mapper.toClass(), mapper, source);
            }
            for (Updater<?, ?> updater : source.updaters()) {
                register(updaters, updaterOrigins, UPDATER, updater.fromClass(), updater.toClass(), updater, source);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <F, T> Optional<Mapper<F, T>> findMapper(Class<F> from, Class<T> to) {
        return Optional.ofNullable((Mapper<F, T>) mappers.get(key(from, to)));
    }

    @SuppressWarnings("unchecked")
    public <F, T> Optional<Updater<F, T>> findUpdater(Class<F> from, Class<T> to) {
        return Optional.ofNullable((Updater<F, T>) updaters.get(key(from, to)));
    }

    /**
     * @throws MappingNotFoundException if no mapper is registered for the pair
     */
    public <F, T> Mapper<F, T> getMapper(Class<F> from, Class<T> to) {
        return findMapper(from, to).orElseThrow(() -> notFound(MAPPER, from, to));
    }

    /**
     * @throws MappingNotFoundException if no updater is registered for the pair
     */
    public <F, T> Updater<F, T> getUpdater(Class<F> from, Class<T> to) {
        return findUpdater(from, to).orElseThrow(() -> notFound(UPDATER, from, to));
    }

    private static <V> void register(Map<Pair, V> target, Map<Pair, Object> origins, String kind,
                                     Class<?> from, Class<?> to, V implementation, Object origin) {
        if (from == null || to == null) {
            throw new IllegalStateException(String.format(
                    "%s from %s declares fromClass()=%s and toClass()=%s; both must be non-null",
                    kind, origin.getClass().getName(), from, to));
        }
        Pair pair = new Pair(from, to);
        Object first = origins.putIfAbsent(pair, origin);
        if (first != null) {
            throw new IllegalStateException(String.format("Duplicate %s for %s -> %s: %s and %s",
                    kind, from.getName(), to.getName(), first.getClass().getName(), origin.getClass().getName()));
        }
        target.put(pair, implementation);
    }

    private MappingNotFoundException notFound(String kind, Class<?> from, Class<?> to) {
        return new MappingNotFoundException(kind, normalise(from), normalise(to));
    }

    private Pair key(Class<?> from, Class<?> to) {
        return new Pair(normalise(from), normalise(to));
    }

    private Class<?> normalise(Class<?> type) {
        return normalised.computeIfAbsent(type, MapperRegistry::nearestEntity);
    }

    private static Class<?> nearestEntity(Class<?> type) {
        if (type.isAnnotationPresent(Entity.class)) {
            return type;
        }
        for (Class<?> ancestor = type.getSuperclass(); ancestor != null; ancestor = ancestor.getSuperclass()) {
            if (ancestor.isAnnotationPresent(Entity.class)) {
                return ancestor;
            }
        }
        return type;
    }

    private record Pair(Class<?> from, Class<?> to) {
    }
}
```

`library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/Patches.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Collects the {@link Updater}s for the PATCH bodies of one entity: one
 * {@code add(BodyClass.class, (body, entity) -> ...)} per body class. A body is a partial class
 * without id — the id comes from the path. Filled by the entity's mapping config.
 *
 * @param <ENTITY> the entity every body is written onto
 */
public final class Patches<ENTITY> {

    private final Class<ENTITY> entityClass;
    private final List<Updater<?, ENTITY>> updaters = new ArrayList<>();

    Patches(Class<ENTITY> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
    }

    public <P> Patches<ENTITY> add(Class<P> patchClass, BiConsumer<P, ENTITY> apply) {
        updaters.add(Updater.of(patchClass, entityClass, apply));
        return this;
    }

    List<Updater<?, ENTITY>> updaters() {
        return List.copyOf(updaters);
    }
}
```

- [ ] **Step 6: Run the library tests**

Run: `./mvnw -B -pl library test`
Expected: `BUILD SUCCESS`, `Tests run: 95` (76 + 13 registry + 3 `of` + 3 patches), 0 failures.

- [ ] **Step 7: Full build**

Run: `./mvnw -B verify`
Expected: `BUILD SUCCESS`; library 95, test-application 8 unit + 31 IT.

- [ ] **Step 8: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/flex/exception/MappingNotFoundException.java \
        library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/Mapper.java \
        library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/Updater.java \
        library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/MapperSource.java \
        library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/MapperRegistry.java \
        library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/Patches.java \
        library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/MapperRegistryTest.java \
        library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/MapperAndUpdaterOfTest.java \
        library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/PatchesTest.java
git commit -F - <<'EOF'
feat(mapper): explicit Mapper/Updater registry

Additive groundwork for dropping ModelMapper: Mapper and Updater with
declared class pairs, MapperSource for beans that contribute several,
Patches for PATCH-body updaters, and MapperRegistry keyed by the exact
pair, with JPA proxies normalised to their nearest @Entity ancestor.
Duplicate pairs and null classes fail the build; a miss throws
MappingNotFoundException. Nothing uses the registry yet.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 3: Facade, mapper bases and Spring wiring (library, WIP commit)

Swaps the library core: `AbsMapper` replaces `AbsModelMapper`, `AbsFlexMapConfig` and the single bases replace the ModelMapper-backed classes, the configuration shrinks to two beans, and the ModelMapper dependency goes. Services only change the mapper type here (plus `update` instead of the in-place `map`); their write paths are Task 4. `updatePartial` and `FieldCopyUtil` stay compiled until Task 4. test-application does not compile after this task — that is expected; the gate is the library build.

**Files:**
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/AbsMapper.java`
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/AbsFlexMapConfig.java`
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/AbsMapDtoToEntity.java`
- Rewrite: `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/AbsMapEntityToDto.java`
- Rewrite: `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/AbsMapUpdateDtoToEntity.java`
- Move + rewrite: `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/mapper/AbsMapperExtRelation.java` → `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/AbsMapperExtRelation.java`
- Rewrite: `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfiguration.java`
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/config/EnableAbsGenericCrud.java` (javadoc)
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceR.java`, `AbsFlexServiceRUD.java`, `AbsFlexServiceCRUD.java`, `AbsFlexServiceExtCRUD.java`, `library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/AbsFlexPagingAndSortingService.java` (mapper type)
- Modify: `library/pom.xml` (drop `org.modelmapper:modelmapper`)
- Delete (main): `flex/AbsModelMapper.java`, `flex/mapper/AbsMapCreateDtoToEntity.java`, `flex/mapper/core/AbsMapBaseDtoToEntity.java`, `flex/mapper/core/AbsMapBasic.java`, `flex/mapper/core/AbsMapDtoToEntity.java`, `flex/mapper/core/RegisterableMapper.java`, `flex/mapper/composite/AbsFlexMapConfigAbstract.java`, `flex/mapper/composite/AbsFlexMapConfigDefault.java`, `flex/config/AbsMapperEagerInitPostProcessor.java`, `flex/config/AbsTypeMapChecker.java`, `flex/config/AbsCrudCustomizer.java`
- Delete (tests): `flex/AbsModelMapperInPlaceMapTest.java`, `flex/config/AbsMapperEagerInitPostProcessorTest.java`, `flex/config/AbsCrudCustomizerTest.java`, `flex/config/AbsCrudCustomizerEagerFlagTest.java`, `flex/config/AbsTypeMapCheckerTest.java`, `flex/mapper/core/AbsMapBasicRegistrationTest.java`, `flex/mapper/composite/AbsFlexMapConfigSharedEntityTest.java`, `flex/mapper/core/AbsMapBaseDtoToEntityNullifyZeroIdTest.java`
- Test (create): `library/src/test/java/by/nhorushko/crudgeneric/flex/AbsMapperTest.java`, `library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/AbsFlexMapConfigTest.java`, `.../flex/mapper/AbsMapDtoToEntityNullifyZeroIdTest.java`, `.../flex/mapper/AbsMapperExtRelationTest.java`, `.../flex/mapper/MapperBasesCglibProxyTest.java`
- Test (rewrite): `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfigurationTest.java`
- Test (modify): `library/src/test/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceExtCRUDTest.java`, `.../service/AbsFlexServiceRUDDeleteTest.java`

All paths under `library/src/main/java/by/nhorushko/crudgeneric/` and `library/src/test/java/by/nhorushko/crudgeneric/` unless written in full.

**Interfaces:**
- Consumes (Task 2): `Mapper`, `Updater`, `MapperSource`, `MapperRegistry` (`getMapper`, `getUpdater`, `findMapper`, `findUpdater`), `Patches` (package-private `Patches(Class)`, `updaters()`), `MappingNotFoundException`.
- Produces:
  - `class AbsMapper` (package `by.nhorushko.crudgeneric.flex`): `AbsMapper(MapperRegistry, EntityManager)`, `AbsMapper(Supplier<MapperRegistry>, EntityManager)`, `<T> T map(Object source, Class<T> destinationType)`, `<T> T update(Object source, T destination)`, `<T> List<T> mapAll(Collection<?> source, Class<T> destinationType)`, `<T extends AbstractEntity<?>> T reference(AbstractDto<?> dto, Class<T> entityClass)`, `<T extends AbstractEntity<?>> T referenceById(Object id, Class<T> entityClass)`, `EntityManager getEntityManager()`.
  - `abstract class AbsFlexMapConfig<CREATE_DTO extends AbsBaseDto, UPDATE_DTO extends AbstractDto<?>, READ_DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> implements MapperSource`: constructor `(AbsMapper, Class<CREATE_DTO>, Class<UPDATE_DTO>, Class<READ_DTO>, Class<ENTITY>)`, field `protected final AbsMapper mapper`, `protected abstract ENTITY toEntity(CREATE_DTO)`, `protected abstract void updateEntity(UPDATE_DTO, ENTITY)`, `protected abstract READ_DTO toReadDto(ENTITY)`, `protected void patches(Patches<ENTITY>)`.
  - `abstract class AbsMapEntityToDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractDto<?>> implements Mapper<ENTITY, DTO>`: constructor `(AbsMapper, Class<ENTITY>, Class<DTO>)`, `protected abstract DTO create(ENTITY from)`.
  - `abstract class AbsMapDtoToEntity<DTO extends AbsBaseDto, ENTITY extends AbstractEntity<?>> implements Mapper<DTO, ENTITY>`: constructor `(AbsMapper, Class<DTO>, Class<ENTITY>)`, `protected abstract ENTITY create(DTO from)`; `map()` = `create()` + `nullifyZeroId()`.
  - `abstract class AbsMapUpdateDtoToEntity<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> implements Updater<DTO, ENTITY>`: constructor `(AbsMapper, Class<DTO>, Class<ENTITY>)`, `public abstract void update(DTO from, ENTITY into)`.
  - `abstract class AbsMapperExtRelation<DTO extends AbsCreateDto, ENTITY, EXT_ID, EXT extends AbstractEntity<?>>` (now in `flex.mapper`): constructor `(AbsMapper, Class<ENTITY>, Class<EXT>)`, `ENTITY map(EXT_ID, DTO)`, `List<ENTITY> mapAll(EXT_ID, Collection<DTO>)`, `protected abstract void setRelation(ENTITY target, EXT relation)`.
  - Spring beans `mapperRegistry` (`MapperRegistry`) and `absMapper` (`AbsMapper`) from `AbsGenericCrudConfiguration`.
  - Services: every constructor that took `AbsModelMapper` now takes `AbsMapper`; field `protected final AbsMapper mapper`.

- [ ] **Step 1: Write the failing facade test**

`library/src/test/java/by/nhorushko/crudgeneric/flex/AbsMapperTest.java`:

```java
package by.nhorushko.crudgeneric.flex;

import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgeneric.flex.mapper.Updater;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbsMapperTest {

    private final AtomicInteger registryCalls = new AtomicInteger();
    private AbsMapper mapper;

    @Before
    public void setUp() {
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(Source.class, Target.class, source -> new Target(source.name))),
                List.of(Updater.of(Source.class, Target.class, (source, target) -> target.name = source.name)),
                List.of());
        mapper = new AbsMapper(() -> {
            registryCalls.incrementAndGet();
            return registry;
        }, null);
    }

    @Test
    public void mapNullSourceReturnsNullWithoutTouchingTheRegistry() {
        assertNull(mapper.map(null, Target.class));
        assertEquals(0, registryCalls.get());
    }

    @Test
    public void mapUsesTheRegisteredMapper() {
        assertEquals("a", mapper.map(new Source("a"), Target.class).name);
    }

    @Test
    public void mapWithoutRegisteredPairThrowsMappingNotFound() {
        assertThrows(MappingNotFoundException.class, () -> mapper.map(new Target("a"), Source.class));
    }

    @Test
    public void updateNullSourceReturnsDestinationUnchanged() {
        Target target = new Target("kept");

        assertSame(target, mapper.update(null, target));
        assertEquals("kept", target.name);
    }

    @Test
    public void updateWritesThroughTheUpdaterAndReturnsTheSameInstance() {
        Target target = new Target("old");

        Target result = mapper.update(new Source("new"), target);

        assertSame(target, result);
        assertEquals("new", target.name);
    }

    @Test
    public void updateWithoutRegisteredUpdaterThrowsMappingNotFound() {
        assertThrows(MappingNotFoundException.class, () -> mapper.update(new Target("a"), new Source("b")));
    }

    @Test
    public void mapAllNullReturnsNull() {
        assertNull(mapper.mapAll(null, Target.class));
    }

    /** The result goes straight into entity collections that Hibernate manages, so it must be mutable. */
    @Test
    public void mapAllReturnsMutableListInSourceOrder() {
        List<Target> result = mapper.mapAll(List.of(new Source("a"), new Source("b")), Target.class);

        result.add(new Target("c"));

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).name);
        assertEquals("b", result.get(1).name);
    }

    @Test
    public void registryIsResolvedOnFirstUseAndOnlyOnce() {
        assertEquals(0, registryCalls.get());

        mapper.map(new Source("a"), Target.class);
        mapper.update(new Source("b"), new Target("c"));
        mapper.mapAll(List.of(new Source("d")), Target.class);

        assertEquals(1, registryCalls.get());
    }

    @Test
    public void referenceUsesTheDtoIdAndTheEntityManager() {
        EntityManager entityManager = mock(EntityManager.class);
        ItemEntity reference = new ItemEntity();
        when(entityManager.getReference(ItemEntity.class, 5L)).thenReturn(reference);
        AbsMapper withJpa = new AbsMapper(new MapperRegistry(List.of(), List.of(), List.of()), entityManager);

        assertSame(reference, withJpa.reference(new ItemDto(5L), ItemEntity.class));
        assertSame(reference, withJpa.referenceById(5L, ItemEntity.class));
        assertSame(entityManager, withJpa.getEntityManager());
    }

    static class Source {
        final String name;

        Source(String name) {
            this.name = name;
        }
    }

    static class Target {
        String name;

        Target(String name) {
            this.name = name;
        }
    }

    static class ItemEntity implements AbstractEntity<Long> {
        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }

    static class ItemDto implements AbstractDto<Long> {
        private final Long id;

        ItemDto(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }
    }
}
```

- [ ] **Step 2: Write the failing config and base tests**

`library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/AbsFlexMapConfigTest.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AbsFlexMapConfigTest {

    @Test
    public void exposesCreateAndReadMappersAndTheUpdateUpdaterWithDeclaredClasses() {
        ItemConfig config = new ItemConfig();

        assertEquals(List.of("ItemCreate->ItemEntity", "ItemEntity->ItemDto"), mapperPairs(config.mappers()));
        assertEquals(List.of("ItemUpdate->ItemEntity"), updaterPairs(config.updaters()));
    }

    @Test
    public void addsOneUpdaterPerPatchEntry() {
        PatchedItemConfig config = new PatchedItemConfig();

        assertEquals(List.of("ItemUpdate->ItemEntity", "NamePatch->ItemEntity", "CodePatch->ItemEntity"),
                updaterPairs(config.updaters()));
    }

    /** An overridable method called from the constructor would run before the subclass's fields exist. */
    @Test
    public void patchesIsNotCalledFromTheConstructor() {
        PatchedItemConfig config = new PatchedItemConfig();
        assertEquals(0, config.patchesCalls);

        config.updaters();

        assertEquals(1, config.patchesCalls);
    }

    @Test
    public void createMapperNormalisesZeroIdAndKeepsRealId() {
        MapperRegistry registry = new MapperRegistry(List.of(), List.of(), List.of(new ItemConfig()));
        Mapper<ItemCreate, ItemEntity> create = registry.getMapper(ItemCreate.class, ItemEntity.class);

        assertNull(create.map(new ItemCreate(0L, "new")).getId());
        assertEquals(Long.valueOf(7L), create.map(new ItemCreate(7L, "assigned")).getId());
    }

    @Test
    public void adaptersDelegateToTheAbstractMethodsAndPatches() {
        MapperRegistry registry = new MapperRegistry(List.of(), List.of(), List.of(new PatchedItemConfig()));
        ItemEntity entity = new ItemEntity(1L, "old");

        registry.getUpdater(ItemUpdate.class, ItemEntity.class).update(new ItemUpdate(1L, "updated"), entity);
        assertEquals("updated", entity.name);
        registry.getUpdater(NamePatch.class, ItemEntity.class).update(new NamePatch("patched"), entity);
        assertEquals("patched", entity.name);
        registry.getUpdater(CodePatch.class, ItemEntity.class).update(new CodePatch("c-1"), entity);
        assertEquals("c-1", entity.code);
        assertEquals("patched", registry.getMapper(ItemEntity.class, ItemDto.class).map(entity).name);
    }

    @Test
    public void rejectsNullClassInTheConstructor() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new ItemConfig(null));

        assertEquals("createDtoClass", e.getMessage());
    }

    /** A patch declared for the config's own UPDATE_DTO collides with the update pair: startup fails naming the config. */
    @Test
    public void patchForTheUpdateDtoClassFailsTheRegistryBuildNamingTheConfig() {
        ItemConfig config = new ItemConfig() {
            @Override
            protected void patches(Patches<ItemEntity> p) {
                p.add(ItemUpdate.class, (dto, entity) -> entity.name = dto.name);
            }
        };

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(), List.of(), List.of(config)));

        assertTrue(e.getMessage(), e.getMessage().startsWith("Duplicate Updater for " + ItemUpdate.class.getName()));
        assertTrue(e.getMessage(), e.getMessage().contains(config.getClass().getName()));
    }

    private static List<String> mapperPairs(Collection<Mapper<?, ?>> mappers) {
        return mappers.stream()
                .map(m -> m.fromClass().getSimpleName() + "->" + m.toClass().getSimpleName())
                .toList();
    }

    private static List<String> updaterPairs(Collection<Updater<?, ?>> updaters) {
        return updaters.stream()
                .map(u -> u.fromClass().getSimpleName() + "->" + u.toClass().getSimpleName())
                .toList();
    }

    static class ItemConfig extends AbsFlexMapConfig<ItemCreate, ItemUpdate, ItemDto, ItemEntity> {
        ItemConfig() {
            this(ItemCreate.class);
        }

        ItemConfig(Class<ItemCreate> createDtoClass) {
            super(null, createDtoClass, ItemUpdate.class, ItemDto.class, ItemEntity.class);
        }

        @Override
        protected ItemEntity toEntity(ItemCreate dto) {
            return new ItemEntity(dto.id, dto.name);
        }

        @Override
        protected void updateEntity(ItemUpdate dto, ItemEntity entity) {
            entity.name = dto.name;
        }

        @Override
        protected ItemDto toReadDto(ItemEntity entity) {
            return new ItemDto(entity.getId(), entity.name);
        }
    }

    static class PatchedItemConfig extends ItemConfig {
        int patchesCalls;

        @Override
        protected void patches(Patches<ItemEntity> p) {
            patchesCalls++;
            p.add(NamePatch.class, (patch, entity) -> entity.name = patch.name())
                    .add(CodePatch.class, (patch, entity) -> entity.code = patch.code());
        }
    }

    record NamePatch(String name) {
    }

    record CodePatch(String code) {
    }

    static class ItemCreate implements AbsCreateDto {
        final Long id;
        final String name;

        ItemCreate(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class ItemUpdate implements AbsUpdateDto<Long> {
        private final Long id;
        final String name;

        ItemUpdate(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    static class ItemDto implements AbstractDto<Long> {
        private final Long id;
        final String name;

        ItemDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    static class ItemEntity implements AbstractEntity<Long> {
        private Long id;
        String name;
        String code;

        ItemEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }
}
```

`library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/AbsMapDtoToEntityNullifyZeroIdTest.java` (replaces the deleted `mapper/core/AbsMapBaseDtoToEntityNullifyZeroIdTest`):

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * AbsMapDtoToEntity normalises the sentinel id 0 to null, so nested child entities mapped through
 * it are treated as new by Hibernate.
 */
public class AbsMapDtoToEntityNullifyZeroIdTest {

    private final LineMapper lineMapper = new LineMapper();

    @Test
    public void mapNormalisesSentinelZeroIdToNull() {
        LineEntity entity = lineMapper.map(new LineDto(0L, "child"));

        assertNull(entity.getId());
        assertEquals("child", entity.getTitle());
    }

    @Test
    public void mapKeepsRealId() {
        assertEquals(Long.valueOf(7L), lineMapper.map(new LineDto(7L, "child")).getId());
    }

    @Test
    public void facadeMapGoesThroughTheNormalisingMapper() {
        AbsMapper mapper = new AbsMapper(new MapperRegistry(List.of(lineMapper), List.of(), List.of()), null);

        assertNull(mapper.map(new LineDto(0L, "child"), LineEntity.class).getId());
    }

    static class LineMapper extends AbsMapDtoToEntity<LineDto, LineEntity> {
        LineMapper() {
            super(null, LineDto.class, LineEntity.class);
        }

        @Override
        protected LineEntity create(LineDto from) {
            LineEntity entity = new LineEntity();
            entity.setId(from.getId());
            entity.setTitle(from.getTitle());
            return entity;
        }
    }

    public static class LineDto implements AbstractDto<Long> {
        private final Long id;
        private final String title;

        public LineDto(Long id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    public static class LineEntity implements AbstractEntity<Long> {
        private Long id;
        private String title;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
```

`library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/AbsMapperExtRelationTest.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbsMapperExtRelationTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final ProjectEntity project = new ProjectEntity();
    private TaskExtMapper extMapper;

    @Before
    public void setUp() {
        when(entityManager.getReference(ProjectEntity.class, 7L)).thenReturn(project);
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(TaskCreate.class, TaskEntity.class, dto -> new TaskEntity(dto.title))),
                List.of(), List.of());
        extMapper = new TaskExtMapper(new AbsMapper(registry, entityManager));
    }

    @Test
    public void mapCreatesTheEntityAndPassesTheReferenceToSetRelation() {
        TaskEntity task = extMapper.map(7L, new TaskCreate("write"));

        assertEquals("write", task.title);
        assertSame(project, task.project);
    }

    @Test
    public void mapAllLinksEveryEntityToTheSameReference() {
        List<TaskEntity> tasks = extMapper.mapAll(7L, List.of(new TaskCreate("a"), new TaskCreate("b")));

        assertEquals(2, tasks.size());
        assertSame(project, tasks.get(0).project);
        assertSame(project, tasks.get(1).project);
    }

    static class TaskExtMapper extends AbsMapperExtRelation<TaskCreate, TaskEntity, Long, ProjectEntity> {
        TaskExtMapper(AbsMapper mapper) {
            super(mapper, TaskEntity.class, ProjectEntity.class);
        }

        @Override
        protected void setRelation(TaskEntity target, ProjectEntity relation) {
            target.project = relation;
        }
    }

    static class TaskCreate implements AbsCreateDto {
        final String title;

        TaskCreate(String title) {
            this.title = title;
        }
    }

    static class TaskEntity {
        final String title;
        ProjectEntity project;

        TaskEntity(String title) {
            this.title = title;
        }
    }

    static class ProjectEntity implements AbstractEntity<Long> {
        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }
}
```

`library/src/test/java/by/nhorushko/crudgeneric/flex/mapper/MapperBasesCglibProxyTest.java` (Review Focus #2):

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * A consumer's mapper bean can end up behind a CGLIB proxy (an aspect, @Transactional). The proxy
 * instance's own fields are never initialised, so fromClass(), toClass() and map() must stay
 * overridable for the proxy to reach the target: the bases must not declare them final.
 */
public class MapperBasesCglibProxyTest {

    @Test
    public void proxiedBasesRegisterUnderTheirDeclaredPairsAndDelegate() {
        Mapper<?, ?> toDto = cglibProxy(new ItemToDto());
        Mapper<?, ?> toEntity = cglibProxy(new ItemCreateToEntity());
        Updater<?, ?> update = cglibProxy(new ItemUpdateToEntity());

        MapperRegistry registry = new MapperRegistry(List.of(toDto, toEntity), List.of(update), List.of());

        assertEquals("a", registry.getMapper(ItemEntity.class, ItemDto.class).map(new ItemEntity(1L, "a")).name);
        assertNull(registry.getMapper(ItemCreate.class, ItemEntity.class).map(new ItemCreate(0L, "b")).getId());
        ItemEntity entity = new ItemEntity(1L, "old");
        registry.getUpdater(ItemUpdate.class, ItemEntity.class).update(new ItemUpdate(1L, "new"), entity);
        assertEquals("new", entity.name);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cglibProxy(T target) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        return (T) factory.getProxy();
    }

    public static class ItemToDto extends AbsMapEntityToDto<ItemEntity, ItemDto> {
        public ItemToDto() {
            super(null, ItemEntity.class, ItemDto.class);
        }

        @Override
        protected ItemDto create(ItemEntity from) {
            return new ItemDto(from.getId(), from.name);
        }
    }

    public static class ItemCreateToEntity extends AbsMapDtoToEntity<ItemCreate, ItemEntity> {
        public ItemCreateToEntity() {
            super(null, ItemCreate.class, ItemEntity.class);
        }

        @Override
        protected ItemEntity create(ItemCreate from) {
            return new ItemEntity(from.id, from.name);
        }
    }

    public static class ItemUpdateToEntity extends AbsMapUpdateDtoToEntity<ItemUpdate, ItemEntity> {
        public ItemUpdateToEntity() {
            super(null, ItemUpdate.class, ItemEntity.class);
        }

        @Override
        public void update(ItemUpdate from, ItemEntity into) {
            into.name = from.name;
        }
    }

    public static class ItemCreate implements AbsCreateDto {
        final Long id;
        final String name;

        ItemCreate(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class ItemUpdate implements AbsUpdateDto<Long> {
        private final Long id;
        final String name;

        ItemUpdate(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    public static class ItemDto implements AbstractDto<Long> {
        private final Long id;
        final String name;

        ItemDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    public static class ItemEntity implements AbstractEntity<Long> {
        private Long id;
        String name;

        ItemEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }
}
```

- [ ] **Step 3: Rewrite the configuration test**

Replace the whole content of `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfigurationTest.java`:

```java
package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapEntityToDto;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.EntityManager;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class AbsGenericCrudConfigurationTest {

    /** Spring passes empty lists into the @Bean method when an application declares no mapper at all. */
    @Test
    public void contextWithoutAnyMapperStarts() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(JpaStub.class, AbsGenericCrudConfiguration.class)) {
            MapperRegistry registry = context.getBean(MapperRegistry.class);

            assertFalse(registry.findMapper(Item.class, ItemDto.class).isPresent());
            assertNotNull(context.getBean(AbsMapper.class));
        }
    }

    @Test
    public void declaresOnlyTheRegistryAndTheFacade() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(JpaStub.class, AbsGenericCrudConfiguration.class)) {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            List<String> declared = Arrays.stream(beanFactory.getBeanDefinitionNames())
                    .filter(name -> "absGenericCrudConfiguration".equals(
                            beanFactory.getBeanDefinition(name).getFactoryBeanName()))
                    .sorted()
                    .toList();

            // Only these two come from the configuration, so no mapping-library bean is left in it.
            // The old library's bean names are not spelled out: the spec's verification grep must stay empty.
            assertEquals(List.of("absMapper", "mapperRegistry"), declared);
            for (String removed : List.of("crudAbstractGenericMappingChecker", "absMapperEagerInitPostProcessor")) {
                assertFalse(removed, context.containsBean(removed));
            }
        }
    }

    /**
     * A mapper bean injects AbsMapper while the registry injects every mapper bean: startup must
     * not fail with BeanCurrentlyInCreationException.
     */
    @Test
    public void mapperBeansInjectingTheFacadeDoNotFormACycle() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                JpaStub.class, AbsGenericCrudConfiguration.class, ItemMappers.class)) {
            AbsMapper mapper = context.getBean(AbsMapper.class);

            assertEquals("a", mapper.map(new Item(1L, "a"), ItemDto.class).getName());
        }
    }

    @Test
    public void facadeIsCreatedBeforeTheRegistryUnderLazyInit() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(JpaStub.class, AbsGenericCrudConfiguration.class, ItemMappers.class);
            context.addBeanFactoryPostProcessor(allLazy());
            context.refresh();

            AbsMapper mapper = context.getBean(AbsMapper.class);
            assertFalse(context.getBeanFactory().containsSingleton("mapperRegistry"));

            ItemDto dto = mapper.map(new Item(2L, "b"), ItemDto.class);

            assertTrue(context.getBeanFactory().containsSingleton("mapperRegistry"));
            assertEquals("b", dto.getName());
        }
    }

    private static BeanFactoryPostProcessor allLazy() {
        return beanFactory -> {
            for (String name : beanFactory.getBeanDefinitionNames()) {
                beanFactory.getBeanDefinition(name).setLazyInit(true);
            }
        };
    }

    @Configuration
    public static class JpaStub {
        @Bean
        public EntityManager entityManager() {
            return mock(EntityManager.class);
        }
    }

    @Configuration
    public static class ItemMappers {
        @Bean
        public ItemToDto itemToDto(AbsMapper mapper) {
            return new ItemToDto(mapper);
        }
    }

    public static class ItemToDto extends AbsMapEntityToDto<Item, ItemDto> {
        public ItemToDto(AbsMapper mapper) {
            super(mapper, Item.class, ItemDto.class);
        }

        @Override
        protected ItemDto create(Item from) {
            return new ItemDto(from.getId(), from.getName());
        }
    }

    public static class Item implements AbstractEntity<Long> {
        private Long id;
        private final String name;

        public Item(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }
    }

    public static class ItemDto implements AbstractDto<Long> {
        private final Long id;
        private final String name;

        public ItemDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
```

- [ ] **Step 4: Run to see the failure**

Run: `./mvnw -B -pl library test`
Expected: `COMPILATION ERROR` — `cannot find symbol` for `AbsMapper`, `AbsFlexMapConfig`, `AbsMapDtoToEntity` and the new signatures.

- [ ] **Step 5: Delete the ModelMapper-backed classes and their tests; move the ext mapper**

```bash
cd /d/projects/crud-generic
M=library/src/main/java/by/nhorushko/crudgeneric/flex
T=library/src/test/java/by/nhorushko/crudgeneric/flex
git rm -q $M/AbsModelMapper.java \
  $M/mapper/AbsMapCreateDtoToEntity.java \
  $M/mapper/core/AbsMapBaseDtoToEntity.java $M/mapper/core/AbsMapBasic.java \
  $M/mapper/core/AbsMapDtoToEntity.java $M/mapper/core/RegisterableMapper.java \
  $M/mapper/composite/AbsFlexMapConfigAbstract.java $M/mapper/composite/AbsFlexMapConfigDefault.java \
  $M/config/AbsMapperEagerInitPostProcessor.java $M/config/AbsTypeMapChecker.java $M/config/AbsCrudCustomizer.java
git rm -q $T/AbsModelMapperInPlaceMapTest.java \
  $T/config/AbsMapperEagerInitPostProcessorTest.java $T/config/AbsCrudCustomizerTest.java \
  $T/config/AbsCrudCustomizerEagerFlagTest.java $T/config/AbsTypeMapCheckerTest.java \
  $T/mapper/core/AbsMapBasicRegistrationTest.java $T/mapper/composite/AbsFlexMapConfigSharedEntityTest.java \
  $T/mapper/core/AbsMapBaseDtoToEntityNullifyZeroIdTest.java
git mv $M/mapper/mapper/AbsMapperExtRelation.java $M/mapper/AbsMapperExtRelation.java
```

- [ ] **Step 6: Create the facade**

`library/src/main/java/by/nhorushko/crudgeneric/flex/AbsMapper.java`:

```java
package by.nhorushko.crudgeneric.flex;

import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgeneric.flex.mapper.Updater;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Facade over {@link MapperRegistry}: every call goes to the {@link Mapper} or {@link Updater}
 * registered for the exact pair of classes, and a missing pair fails with
 * {@link MappingNotFoundException}. Nothing is copied implicitly.
 * <p>
 * The registry is taken from the supplier on the first mapping call, not at construction. That
 * breaks the bean cycle <em>mapper bean → AbsMapper → MapperRegistry → every mapper bean</em>, so
 * mapper beans can inject {@code AbsMapper} in their constructors. The flip side: do not map in a
 * constructor or {@code @PostConstruct} of a bean that any mapper depends on.
 * </p>
 */
public class AbsMapper {

    private final Supplier<MapperRegistry> registrySupplier;
    private final EntityManager entityManager;
    private volatile MapperRegistry registry;

    /**
     * For tests and manual wiring, with the registry already built.
     */
    public AbsMapper(MapperRegistry registry, EntityManager entityManager) {
        this(constant(registry), entityManager);
    }

    /**
     * For Spring. The supplier is called on the first mapping call, not here. Under a race on that
     * first call it may run more than once, so it must return the same registry every time
     * ({@code ObjectProvider::getObject} of a singleton does).
     */
    public AbsMapper(Supplier<MapperRegistry> registry, EntityManager entityManager) {
        this.registrySupplier = Objects.requireNonNull(registry, "registry");
        this.entityManager = entityManager;
    }

    /**
     * Maps {@code source} to a new {@code destinationType} instance with the {@link Mapper}
     * registered for {@code (source.getClass(), destinationType)}.
     *
     * @return the mapped object, or {@code null} if {@code source} is {@code null}
     * @throws MappingNotFoundException if no mapper is registered for the pair
     */
    public <T> T map(Object source, Class<T> destinationType) {
        if (source == null) {
            return null;
        }
        Objects.requireNonNull(destinationType, "destinationType");
        return mapWith(registry().getMapper(source.getClass(), destinationType), source);
    }

    /**
     * Writes {@code source} onto the existing {@code destination} with the {@link Updater}
     * registered for {@code (source.getClass(), destination.getClass())}. Only what the updater
     * writes changes.
     *
     * @return {@code destination}, unchanged if {@code source} is {@code null}
     * @throws MappingNotFoundException if no updater is registered for the pair
     */
    public <T> T update(Object source, T destination) {
        if (source == null) {
            return destination;
        }
        Objects.requireNonNull(destination, "destination");
        updateWith(registry().getUpdater(source.getClass(), destination.getClass()), source, destination);
        return destination;
    }

    /**
     * Maps every element with {@link #map(Object, Class)}. The result is a new mutable
     * {@link ArrayList}, so it can be set into an entity collection that Hibernate manages.
     *
     * @return the mapped list, or {@code null} if {@code source} is {@code null}
     */
    public <T> List<T> mapAll(Collection<?> source, Class<T> destinationType) {
        if (source == null) {
            return null;
        }
        List<T> result = new ArrayList<>(source.size());
        for (Object element : source) {
            result.add(map(element, destinationType));
        }
        return result;
    }

    /**
     * A reference to the entity with the DTO's id, without loading it and without copying any
     * field. This is how a relation is set from a read DTO.
     */
    public <T extends AbstractEntity<?>> T reference(AbstractDto<?> dto, Class<T> entityClass) {
        return referenceById(dto.getId(), entityClass);
    }

    /**
     * A reference to the entity with the given id, without loading it.
     */
    public <T extends AbstractEntity<?>> T referenceById(Object id, Class<T> entityClass) {
        return entityManager.getReference(entityClass, id);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    private MapperRegistry registry() {
        MapperRegistry result = registry;
        if (result == null) {
            result = Objects.requireNonNull(registrySupplier.get(), "MapperRegistry supplier returned null");
            registry = result;
        }
        return result;
    }

    private static Supplier<MapperRegistry> constant(MapperRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return () -> registry;
    }

    @SuppressWarnings("unchecked")
    private static <F, T> T mapWith(Mapper<F, T> mapper, Object source) {
        return mapper.map((F) source);
    }

    @SuppressWarnings("unchecked")
    private static <F, T> void updateWith(Updater<F, T> updater, Object source, Object destination) {
        updater.update((F) source, (T) destination);
    }
}
```

- [ ] **Step 7: Create `AbsFlexMapConfig` and the single bases**

`library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/AbsFlexMapConfig.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The explicit mapping of one entity's CRUD triple: how a create DTO becomes a new entity, how an
 * update DTO is written onto the managed entity, how the entity becomes a read DTO, and how each
 * PATCH body is written onto the managed entity.
 * <p>
 * Nothing is copied implicitly — the entity holds exactly what {@link #toEntity},
 * {@link #updateEntity} and {@link #patches} write. {@code null} has no meaning of its own either:
 * {@code e.setName(dto.getName())} clears the field when the DTO carries {@code null}, and
 * {@code if (dto.getName() != null) e.setName(dto.getName())} keeps it.
 * </p>
 * <p>
 * Registered pairs: {@code CREATE_DTO -> ENTITY} and {@code ENTITY -> READ_DTO} as {@link Mapper}s,
 * {@code UPDATE_DTO -> ENTITY} plus one {@code BODY -> ENTITY} per {@link #patches} entry as
 * {@link Updater}s. The create mapper calls {@code nullifyZeroId()} on the result of
 * {@link #toEntity}: the library rule "id 0 means new".
 * </p>
 */
public abstract class AbsFlexMapConfig<CREATE_DTO extends AbsBaseDto,
                                       UPDATE_DTO extends AbstractDto<?>,
                                       READ_DTO extends AbstractDto<?>,
                                       ENTITY extends AbstractEntity<?>> implements MapperSource {

    protected final AbsMapper mapper;
    private final Class<CREATE_DTO> createDtoClass;
    private final Class<UPDATE_DTO> updateDtoClass;
    private final Class<READ_DTO> readDtoClass;
    private final Class<ENTITY> entityClass;

    public AbsFlexMapConfig(AbsMapper mapper, Class<CREATE_DTO> createDtoClass, Class<UPDATE_DTO> updateDtoClass,
                            Class<READ_DTO> readDtoClass, Class<ENTITY> entityClass) {
        this.mapper = mapper;
        this.createDtoClass = Objects.requireNonNull(createDtoClass, "createDtoClass");
        this.updateDtoClass = Objects.requireNonNull(updateDtoClass, "updateDtoClass");
        this.readDtoClass = Objects.requireNonNull(readDtoClass, "readDtoClass");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
    }

    /**
     * Builds a new entity from the create DTO: {@code new Entity()} plus every field it needs.
     */
    protected abstract ENTITY toEntity(CREATE_DTO dto);

    /**
     * Writes the update DTO onto the managed entity. Only what this method writes changes.
     */
    protected abstract void updateEntity(UPDATE_DTO dto, ENTITY entity);

    /**
     * Builds the read DTO from the entity.
     */
    protected abstract READ_DTO toReadDto(ENTITY entity);

    /**
     * Declares one {@link Updater} per PATCH body class — a partial class without id, applied to the
     * managed entity by {@code AbsFlexServiceRUD.patch(id, body)}. None by default. Called when the
     * registry collects {@link #updaters()}, never from the constructor.
     */
    protected void patches(Patches<ENTITY> p) {
    }

    @Override
    public Collection<Mapper<?, ?>> mappers() {
        return List.<Mapper<?, ?>>of(
                Mapper.of(createDtoClass, entityClass, this::createEntity),
                Mapper.of(entityClass, readDtoClass, this::toReadDto));
    }

    @Override
    public Collection<Updater<?, ?>> updaters() {
        Patches<ENTITY> patches = new Patches<>(entityClass);
        patches(patches);
        List<Updater<?, ?>> result = new ArrayList<>();
        result.add(Updater.of(updateDtoClass, entityClass, this::updateEntity));
        result.addAll(patches.updaters());
        return result;
    }

    private ENTITY createEntity(CREATE_DTO dto) {
        ENTITY entity = toEntity(dto);
        if (entity != null) {
            entity.nullifyZeroId();
        }
        return entity;
    }
}
```

`library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/AbsMapDtoToEntity.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;

/**
 * Base for an explicit DTO → new entity mapper, typically for nested children mapped from the
 * parent's {@code toEntity} with {@code mapper.mapAll(...)}. Implement {@link #create}; {@link #map}
 * then normalises the sentinel id {@code 0} to {@code null}, so a new child is inserted.
 * <p>
 * {@link #fromClass()}, {@link #toClass()} and {@link #map} are deliberately not final: a CGLIB
 * proxy of the bean must be able to delegate them to the target.
 * </p>
 */
public abstract class AbsMapDtoToEntity<DTO extends AbsBaseDto, ENTITY extends AbstractEntity<?>>
        implements Mapper<DTO, ENTITY> {

    protected final AbsMapper mapper;
    protected final Class<DTO> dtoClass;
    protected final Class<ENTITY> entityClass;

    public AbsMapDtoToEntity(AbsMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        this.mapper = mapper;
        this.dtoClass = dtoClass;
        this.entityClass = entityClass;
    }

    /**
     * Builds a new entity from the DTO.
     */
    protected abstract ENTITY create(DTO from);

    @Override
    public Class<DTO> fromClass() {
        return dtoClass;
    }

    @Override
    public Class<ENTITY> toClass() {
        return entityClass;
    }

    @Override
    public ENTITY map(DTO from) {
        ENTITY entity = create(from);
        if (entity != null) {
            entity.nullifyZeroId();
        }
        return entity;
    }
}
```

Replace the whole content of `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/AbsMapEntityToDto.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;

/**
 * Base for an explicit entity → DTO mapper: a read DTO of a read-only service, a view DTO, a DTO
 * with final fields built through its constructor. Implement {@link #create}.
 * <p>
 * {@link #fromClass()}, {@link #toClass()} and {@link #map} are deliberately not final: a CGLIB
 * proxy of the bean must be able to delegate them to the target.
 * </p>
 */
public abstract class AbsMapEntityToDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractDto<?>>
        implements Mapper<ENTITY, DTO> {

    protected final AbsMapper mapper;
    protected final Class<ENTITY> entityClass;
    protected final Class<DTO> dtoClass;

    public AbsMapEntityToDto(AbsMapper mapper, Class<ENTITY> entityClass, Class<DTO> dtoClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
    }

    /**
     * Builds the DTO from the entity.
     */
    protected abstract DTO create(ENTITY from);

    @Override
    public Class<ENTITY> fromClass() {
        return entityClass;
    }

    @Override
    public Class<DTO> toClass() {
        return dtoClass;
    }

    @Override
    public DTO map(ENTITY from) {
        return create(from);
    }
}
```

Replace the whole content of `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/AbsMapUpdateDtoToEntity.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;

/**
 * Base for an explicit update DTO → managed entity updater. Implement {@link #update}.
 * <p>
 * {@link #fromClass()} and {@link #toClass()} are deliberately not final: a CGLIB proxy of the bean
 * must be able to delegate them to the target.
 * </p>
 */
public abstract class AbsMapUpdateDtoToEntity<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>>
        implements Updater<DTO, ENTITY> {

    protected final AbsMapper mapper;
    protected final Class<DTO> dtoClass;
    protected final Class<ENTITY> entityClass;

    public AbsMapUpdateDtoToEntity(AbsMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        this.mapper = mapper;
        this.dtoClass = dtoClass;
        this.entityClass = entityClass;
    }

    @Override
    public Class<DTO> fromClass() {
        return dtoClass;
    }

    @Override
    public Class<ENTITY> toClass() {
        return entityClass;
    }

    /**
     * Writes the DTO onto the managed entity. Only what this method writes changes.
     */
    @Override
    public abstract void update(DTO from, ENTITY into);
}
```

Replace the whole content of the moved `library/src/main/java/by/nhorushko/crudgeneric/flex/mapper/AbsMapperExtRelation.java`:

```java
package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates entities from create DTOs and links each to an existing external entity identified by id.
 * <p>
 * The entity is mapped with the {@code Mapper<DTO, ENTITY>} from the registry, the external entity
 * is a reference obtained by id without loading it, and {@link #setRelation} links the two —
 * explicitly, in the subclass.
 * </p>
 *
 * @param <DTO>    the create DTO
 * @param <ENTITY> the entity created from the DTO
 * @param <EXT_ID> the id type of the external entity
 * @param <EXT>    the external entity the created one is linked to
 */
public abstract class AbsMapperExtRelation<DTO extends AbsCreateDto, ENTITY, EXT_ID, EXT extends AbstractEntity<?>> {

    private final AbsMapper mapper;
    private final Class<ENTITY> entityClass;
    private final Class<EXT> extClass;

    public AbsMapperExtRelation(AbsMapper mapper, Class<ENTITY> entityClass, Class<EXT> extClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.extClass = extClass;
    }

    /**
     * Maps the DTO to a new entity and links it to the external entity with id {@code extId}.
     */
    public ENTITY map(EXT_ID extId, DTO dto) {
        ENTITY entity = mapper.map(dto, entityClass);
        EXT relation = mapper.referenceById(extId, extClass);
        setRelation(entity, relation);
        return entity;
    }

    /**
     * Maps every DTO and links each entity to the same external entity.
     */
    public List<ENTITY> mapAll(EXT_ID extId, Collection<DTO> dtos) {
        return dtos.stream()
                .map(dto -> map(extId, dto))
                .collect(Collectors.toList());
    }

    /**
     * Links the new entity to the external one, e.g. {@code target.setProject(relation)}.
     */
    protected abstract void setRelation(ENTITY target, EXT relation);
}
```

- [ ] **Step 8: Rewrite the Spring configuration**

Replace the whole content of `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfiguration.java`:

```java
package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgeneric.flex.mapper.MapperSource;
import by.nhorushko.crudgeneric.flex.mapper.Updater;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring wiring of the explicit mapping stack: a {@link MapperRegistry} built from every
 * {@link Mapper}, {@link Updater} and {@link MapperSource} bean, and the {@link AbsMapper} facade
 * over it.
 * <p>
 * {@code AbsMapper} gets the registry lazily through an {@link ObjectProvider}, so mapper beans can
 * inject {@code AbsMapper} without a cycle with the registry, which injects them.
 * </p>
 */
@Configuration
public class AbsGenericCrudConfiguration {

    /**
     * Collects every mapper bean. Spring creates them all to satisfy the injection, with or without
     * lazy init, and passes an empty list when the application has none of a kind.
     */
    @Bean
    public MapperRegistry mapperRegistry(List<Mapper<?, ?>> mappers,
                                         List<Updater<?, ?>> updaters,
                                         List<MapperSource> sources) {
        return new MapperRegistry(mappers, updaters, sources);
    }

    @Bean
    public AbsMapper absMapper(ObjectProvider<MapperRegistry> registry, EntityManager entityManager) {
        return new AbsMapper(registry::getObject, entityManager);
    }
}
```

In `library/src/main/java/by/nhorushko/crudgeneric/flex/config/EnableAbsGenericCrud.java`, replace the class javadoc (everything between `/**` and ` */` above `@Retention`) with:

```java
/**
 * Enables the Generic CRUD Framework in a Spring Boot application.
 * <p>
 * Placed on the application's main class or any configuration class, it imports
 * {@link AbsGenericCrudConfiguration}, which registers the
 * {@link by.nhorushko.crudgeneric.flex.mapper.MapperRegistry} built from every mapper bean and the
 * {@link by.nhorushko.crudgeneric.flex.AbsMapper} facade the services map through.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableAbsGenericCrud
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 *
 * @see AbsGenericCrudConfiguration
 */
```

- [ ] **Step 9: Switch the services to `AbsMapper` and drop the dependency**

```bash
cd /d/projects/crud-generic/library/src/main/java/by/nhorushko/crudgeneric/flex
sed -i 's/AbsModelMapper/AbsMapper/g' service/AbsFlexServiceR.java service/AbsFlexServiceRUD.java \
  service/AbsFlexServiceCRUD.java service/AbsFlexServiceExtCRUD.java pageable/AbsFlexPagingAndSortingService.java
sed -i 's/import by\.nhorushko\.crudgeneric\.flex\.mapper\.mapper\.AbsMapperExtRelation;/import by.nhorushko.crudgeneric.flex.mapper.AbsMapperExtRelation;/' \
  service/AbsFlexServiceExtCRUD.java
sed -i 's/^        mapper\.map(dto, entity);$/        mapper.update(dto, entity);/' service/AbsFlexServiceRUD.java
grep -n "mapper.update(dto, entity)" service/AbsFlexServiceRUD.java
cd /d/projects/crud-generic
grep -rn "AbsModelMapper\|mapper\.mapper\.\|mapper\.core\.\|mapper\.composite\." library/src/main/java || echo "clean"
```

Expected: one hit for `mapper.update(dto, entity)` and `clean`.

In `library/pom.xml`, delete this block:

```xml
        <dependency>
            <groupId>org.modelmapper</groupId>
            <artifactId>modelmapper</artifactId>
            <version>3.2.6</version>
        </dependency>

```

- [ ] **Step 10: Point the two existing service tests at a real `AbsMapper`**

`library/src/test/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceExtCRUDTest.java`:

1. Replace the imports
   `import by.nhorushko.crudgeneric.flex.AbsModelMapper;` and
   `import by.nhorushko.crudgeneric.flex.mapper.mapper.AbsMapperExtRelation;` with

   ```java
   import by.nhorushko.crudgeneric.flex.AbsMapper;
   import by.nhorushko.crudgeneric.flex.mapper.AbsMapperExtRelation;
   import by.nhorushko.crudgeneric.flex.mapper.Mapper;
   import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
   ```

2. Replace

   ```java
       @Mock
       private AbsModelMapper mapper;
   ```

   with

   ```java
       private final AbsMapper mapper = new AbsMapper(new MapperRegistry(
               List.of(Mapper.of(ItemEntity.class, ItemDto.class, entity -> new ItemDto(entity.getId(), entity.getName()))),
               List.of(), List.of()), null);
   ```

3. Delete these three stub lines (one in `saveTreatsZeroIdAsNewAndNullifiesItBeforeSave`, one in each of the two `saveAll...` tests):

   ```java
           when(mapper.map(any(ItemEntity.class), eq(ItemDto.class))).thenReturn(new ItemDto(1L, "hooked"));
   ```
   ```java
           when(mapper.mapAll(anyCollection(), eq(ItemDto.class))).thenReturn(singletonList(new ItemDto(1L, "hooked")));
   ```

`library/src/test/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceRUDDeleteTest.java`:

1. Replace `import by.nhorushko.crudgeneric.flex.AbsModelMapper;` with

   ```java
   import by.nhorushko.crudgeneric.flex.AbsMapper;
   import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
   ```

2. Replace

   ```java
       @Mock
       private AbsModelMapper mapper;
   ```

   with

   ```java
       private final AbsMapper mapper = new AbsMapper(new MapperRegistry(List.of(), List.of(), List.of()), null);
   ```

- [ ] **Step 11: Run the library build**

Run: `./mvnw -B -pl library verify`
Expected: `BUILD SUCCESS`, `Tests run: 98` — 95 − 21 deleted − 3 old configuration tests + 10 `AbsMapperTest` + 7 `AbsFlexMapConfigTest` + 3 nullify + 2 ext relation + 1 CGLIB + 4 configuration. 0 failures.

```bash
grep -rli modelmapper library/src library/pom.xml || echo "clean"
```

Expected: only `library/src/main/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceRUD.java` (its javadoc still mentions the old `skipNullEnabled` setting; Task 4 rewrites that file). Anything else is a miss — fix it.

- [ ] **Step 12: WIP commit**

test-application does not compile yet; this commit is squashed in Task 5.

```bash
cd /d/projects/crud-generic
git add -A library
git commit -F - <<'EOF'
wip: facade, mapper bases and Spring wiring

Squashed into "feat!: replace ModelMapper with explicit mappers".

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 4: Write paths — `patch`, `changeEntity`, seams (library, WIP commit)

**Files:**
- Rewrite: `library/src/main/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceRUD.java`
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceCRUD.java`
- Rewrite: `library/src/main/java/by/nhorushko/crudgeneric/flex/service/AbsUpdateChangesHookable.java`
- Delete: `library/src/main/java/by/nhorushko/crudgeneric/flex/util/FieldCopyUtil.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceRUDPatchTest.java`

**Interfaces:**
- Consumes (Task 3): `AbsMapper.update(Object, T)`, `AbsMapper.map`, `AbsMapper.mapAll`; `MapperRegistry`, `Mapper.of`, `Updater.of` in tests.
- Produces, in `AbsFlexServiceRUD<ENTITY_ID, ENTITY, READ_DTO, UPDATE_DTO, REPOSITORY>`:
  - `public READ_DTO update(UPDATE_DTO dto)` (signature unchanged)
  - `public READ_DTO patch(ENTITY_ID id, Object body)`
  - `protected READ_DTO changeEntity(ENTITY_ID id, Consumer<ENTITY> change)`
  - `protected ENTITY loadForUpdate(ENTITY_ID id)`, `protected ENTITY saveUpdated(ENTITY entity)`
  - `protected void beforeUpdateHook(UPDATE_DTO dto)`, `protected void beforePatchHook(ENTITY_ID id, Object body)`, `protected void afterUpdateHook(READ_DTO dto)`
  - `protected Optional<READ_DTO> tryBeforeUpdateHook(ENTITY_ID id, Object body)`
  - removed: `updatePartial`, `copyPartial`, `IGNORE_PARTIAL_UPDATE_PROPERTIES`, `mapEntity(Object)`, `mapAllEntities(Collection<?>)`
- In `AbsFlexServiceCRUD`: `protected ENTITY mapEntity(CREATE_DTO dto)`, `protected List<ENTITY> mapAllEntities(Collection<CREATE_DTO> dtos)`.
- `AbsUpdateChangesHookable<ENTITY_ID, READ_DTO>`: `void beforeUpdateHook(READ_DTO previous, Object current)`, `void afterUpdateHook(READ_DTO previous, READ_DTO current)`.

- [ ] **Step 1: Write the failing test**

`library/src/test/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceRUDPatchTest.java`:

```java
package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgeneric.flex.mapper.Updater;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The three write paths of AbsFlexServiceRUD — update, patch, changeEntity — share one pipeline:
 * loadForUpdate, an explicit change of the managed entity, saveUpdated, then the after-hooks.
 */
public class AbsFlexServiceRUDPatchTest {

    private final List<String> events = new ArrayList<>();
    private JpaRepository<ItemEntity, Long> repository;
    private ItemEntity stored;
    private RecordingService service;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        repository = mock(JpaRepository.class);
        stored = new ItemEntity(1L, "old");
        when(repository.findById(1L)).thenReturn(Optional.of(stored));
        when(repository.findById(404L)).thenReturn(Optional.empty());
        when(repository.save(any(ItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(ItemEntity.class, ItemDto.class, entity -> new ItemDto(entity.getId(), entity.name))),
                List.of(Updater.of(ItemUpdate.class, ItemEntity.class, (dto, entity) -> entity.name = dto.name),
                        Updater.of(NamePatch.class, ItemEntity.class, (patch, entity) -> entity.name = patch.name())),
                List.of());
        service = new RecordingService(new AbsMapper(registry, null), repository, events);
    }

    @Test
    public void updateRunsThePipelineThroughTheUpdateDtoUpdater() {
        ItemDto result = service.update(new ItemUpdate(1L, "new"));

        assertEquals("new", result.name);
        assertEquals("new", stored.name);
        assertEquals(List.of("beforeUpdate:new", "beforeChanges:old", "load:1", "save:new",
                "afterUpdate:new", "afterChanges:old->new"), events);
    }

    @Test
    public void patchRunsBeforePatchHookHookableHooksAndAfterUpdateHook() {
        ItemDto result = service.patch(1L, new NamePatch("new"));

        assertEquals("new", result.name);
        assertEquals("new", stored.name);
        assertEquals(List.of("beforePatch:1:NamePatch", "beforeChanges:old", "load:1", "save:new",
                "afterUpdate:new", "afterChanges:old->new"), events);
    }

    /** update(null, entity) would return the entity unchanged, and patch would then store it silently. */
    @Test
    public void patchWithNullBodyOrIdThrowsWithoutSaving() {
        assertThrows(NullPointerException.class, () -> service.patch(1L, null));
        assertThrows(NullPointerException.class, () -> service.patch(null, new NamePatch("new")));

        assertTrue(events.isEmpty());
        verify(repository, never()).save(any());
        assertEquals("old", stored.name);
    }

    /** A controller passing a raw JSON map as the body gets a loud failure, not a partial write. */
    @Test
    public void patchBodyWithoutUpdaterThrowsMappingNotFoundWithoutSaving() {
        Map<String, Object> body = new LinkedHashMap<>(Map.of("name", "new"));

        MappingNotFoundException e = assertThrows(MappingNotFoundException.class, () -> service.patch(1L, body));

        assertTrue(e.getMessage(), e.getMessage().contains(LinkedHashMap.class.getName()));
        verify(repository, never()).save(any());
        assertEquals("old", stored.name);
    }

    /** Exact key at the service level: a subclass of UPDATE_DTO never borrows the parent's updater. */
    @Test
    public void updateWithSubclassOfUpdateDtoIsNotMatchedByTheParentUpdater() {
        assertThrows(MappingNotFoundException.class, () -> service.update(new ItemUpdateSub(1L, "new")));

        verify(repository, never()).save(any());
        assertEquals("old", stored.name);
    }

    @Test
    public void changeEntityAppliesChangeSavesAndRunsOnlyAfterHooks() {
        ItemDto result = service.rename(1L, "renamed");

        assertEquals("renamed", result.name);
        assertEquals("renamed", stored.name);
        assertEquals(List.of("load:1", "save:renamed", "afterUpdate:renamed", "afterChanges:old->renamed"), events);
    }

    @Test
    public void changeEntityWhoseChangeThrowsSavesNothingAndSkipsAfterHooks() {
        assertThrows(IllegalStateException.class, () -> service.changeEntity(1L, entity -> {
            throw new IllegalStateException("rejected");
        }));

        assertEquals(List.of("load:1"), events);
        verify(repository, never()).save(any());
    }

    @Test
    public void loadForUpdateWithoutEntityThrowsAppNotFound() {
        assertThrows(AppNotFoundException.class, () -> service.rename(404L, "x"));

        assertEquals(List.of("load:404"), events);
        verify(repository, never()).save(any());
    }

    static class RecordingService
            extends AbsFlexServiceRUD<Long, ItemEntity, ItemDto, ItemUpdate, JpaRepository<ItemEntity, Long>>
            implements AbsUpdateChangesHookable<Long, ItemDto> {

        private final List<String> events;

        RecordingService(AbsMapper mapper, JpaRepository<ItemEntity, Long> repository, List<String> events) {
            super(mapper, repository, ItemEntity.class, ItemDto.class, ItemUpdate.class);
            this.events = events;
        }

        ItemDto rename(Long id, String name) {
            return changeEntity(id, entity -> entity.name = name);
        }

        @Override
        protected void beforeUpdateHook(ItemUpdate dto) {
            events.add("beforeUpdate:" + dto.name);
        }

        @Override
        protected void beforePatchHook(Long id, Object body) {
            events.add("beforePatch:" + id + ":" + body.getClass().getSimpleName());
        }

        @Override
        public void beforeUpdateHook(ItemDto previous, Object current) {
            events.add("beforeChanges:" + previous.name);
        }

        @Override
        protected ItemEntity loadForUpdate(Long id) {
            events.add("load:" + id);
            return super.loadForUpdate(id);
        }

        @Override
        protected ItemEntity saveUpdated(ItemEntity entity) {
            events.add("save:" + entity.name);
            return super.saveUpdated(entity);
        }

        @Override
        protected void afterUpdateHook(ItemDto dto) {
            events.add("afterUpdate:" + dto.name);
        }

        @Override
        public void afterUpdateHook(ItemDto previous, ItemDto current) {
            events.add("afterChanges:" + previous.name + "->" + current.name);
        }
    }

    static class ItemEntity implements AbstractEntity<Long> {
        private Long id;
        String name;

        ItemEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }

    static class ItemDto implements AbstractDto<Long> {
        private final Long id;
        final String name;

        ItemDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    static class ItemUpdate implements AbsUpdateDto<Long> {
        private final Long id;
        final String name;

        ItemUpdate(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    static class ItemUpdateSub extends ItemUpdate {
        ItemUpdateSub(Long id, String name) {
            super(id, name);
        }
    }

    record NamePatch(String name) {
    }
}
```

- [ ] **Step 2: Run to see the failure**

Run: `./mvnw -B -pl library test`
Expected: `COMPILATION ERROR` — no `patch`, `changeEntity`, `loadForUpdate`, `saveUpdated`, `beforePatchHook`; `beforeUpdateHook(ItemUpdate)` and `beforeUpdateHook(ItemDto, Object)` do not override anything.

- [ ] **Step 3: Rewrite `AbsUpdateChangesHookable`**

Replace the whole content of `library/src/main/java/by/nhorushko/crudgeneric/flex/service/AbsUpdateChangesHookable.java`:

```java
package by.nhorushko.crudgeneric.flex.service;

/**
 * Hooks that see the state of an entity before and after a write. A service that implements this
 * interface gets them on every write path of {@link AbsFlexServiceRUD}; {@code beforeUpdateHook}
 * only on the paths that carry a request body ({@code update} and {@code patch}).
 *
 * @param <ENTITY_ID> the entity id type
 * @param <READ_DTO>  the read DTO the states are represented as
 */
public interface AbsUpdateChangesHookable<ENTITY_ID, READ_DTO> {

    /**
     * Called before the body is applied.
     *
     * @param previous the stored state before the write
     * @param current  the request body about to be applied: the update DTO for {@code update}, the
     *                 PATCH body for {@code patch}
     */
    void beforeUpdateHook(READ_DTO previous, Object current);

    /**
     * Called after the write is stored.
     *
     * @param previous the state before the write
     * @param current  the stored state after the write
     */
    void afterUpdateHook(READ_DTO previous, READ_DTO current);
}
```

- [ ] **Step 4: Rewrite `AbsFlexServiceRUD`**

Replace the whole content of `library/src/main/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceRUD.java`:

```java
package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import by.nhorushko.crudgeneric.flex.model.IdEntity;
import lombok.Getter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static java.lang.String.format;

/**
 * Abstract service class providing read, update, and delete operations for entities.
 * <p>
 * Extends {@link AbsFlexServiceR} with three write paths — {@link #update}, {@link #patch} and
 * {@link #changeEntity} — and {@link #delete}. Every write path loads the managed entity through
 * {@link #loadForUpdate}, changes it in place explicitly, and stores it through {@link #saveUpdated};
 * override those two seams rather than the write methods. A check that must guard every write path
 * belongs in {@code loadForUpdate}: {@link #beforeUpdateHook(AbsUpdateDto)} runs for {@code update}
 * only.
 * </p>
 *
 * @param <ENTITY_ID>  the type of the entity's identifier
 * @param <ENTITY>     the entity type that extends {@link AbstractEntity}
 * @param <READ_DTO>   the DTO type used for read operations, extending {@link AbstractDto}
 * @param <UPDATE_DTO> the DTO type used for update operations, extending {@link AbsUpdateDto}
 * @param <REPOSITORY> the repository type for the entity, extending {@link JpaRepository}
 */
public abstract class AbsFlexServiceRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbsUpdateDto<ENTITY_ID>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>
        extends AbsFlexServiceR<ENTITY_ID, ENTITY, READ_DTO, REPOSITORY> {

    @Getter
    protected final Class<UPDATE_DTO> updateDtoClass;

    public AbsFlexServiceRUD(AbsMapper mapper, REPOSITORY repository,
                             Class<ENTITY> entityClass, Class<READ_DTO> readDtoClass, Class<UPDATE_DTO> updateDtoClass) {
        super(mapper, repository, entityClass, readDtoClass);
        this.updateDtoClass = updateDtoClass;
    }

    /**
     * Updates the entity identified by the DTO's id.
     * <p>
     * The managed entity is loaded with {@link #loadForUpdate}, the {@code Updater} registered for
     * {@code UPDATE_DTO -> ENTITY} writes the DTO onto it, and {@link #saveUpdated} stores it. Only
     * what the updater writes changes; a {@code null} in the DTO clears a field only if the updater
     * writes that field as is.
     * </p>
     *
     * @param dto the DTO with the entity's id and the new values
     * @return the stored entity as a READ_DTO
     * @throws IllegalArgumentException if the DTO has no id ({@code null} or {@code 0})
     * @throws AppNotFoundException     if no entity with that id exists
     * @throws MappingNotFoundException if no updater is registered for the DTO's exact class
     */
    public READ_DTO update(UPDATE_DTO dto) {
        checkId(dto);
        beforeUpdateHook(dto);
        return runUpdate(dto.getId(), dto);
    }

    /**
     * Applies a PATCH body to the entity with the given id.
     * <p>
     * The body is a partial class without id — the id comes from the path. The {@code Updater}
     * registered for {@code body.getClass() -> ENTITY}, declared in the entity mapping config's
     * {@code patches()}, writes it onto the managed entity. Runs {@link #beforePatchHook}, the
     * {@link AbsUpdateChangesHookable} hooks and {@link #afterUpdateHook}, but not
     * {@link #beforeUpdateHook(AbsUpdateDto)}.
     * </p>
     *
     * @param id   the id of the entity to change
     * @param body the PATCH body
     * @return the stored entity as a READ_DTO
     * @throws NullPointerException     if {@code id} or {@code body} is {@code null}
     * @throws AppNotFoundException     if no entity with that id exists
     * @throws MappingNotFoundException if no updater is registered for the body's exact class
     */
    public READ_DTO patch(ENTITY_ID id, Object body) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(body, "body");
        beforePatchHook(id, body);
        return runUpdate(id, body);
    }

    /**
     * Applies a change written in code to the entity with the given id: loads it with
     * {@link #loadForUpdate}, runs {@code change} on it, stores it with {@link #saveUpdated}.
     * <p>
     * There is no request body, so the hooks that take one — {@link #beforeUpdateHook(AbsUpdateDto)},
     * {@link #beforePatchHook} and {@link AbsUpdateChangesHookable#beforeUpdateHook} — do not run.
     * {@link #afterUpdateHook} and {@link AbsUpdateChangesHookable#afterUpdateHook} do; the previous
     * state for the latter is taken before {@code change} runs.
     * </p>
     *
     * @param id     the id of the entity to change
     * @param change the change, applied to the managed entity
     * @return the stored entity as a READ_DTO
     * @throws AppNotFoundException if no entity with that id exists
     */
    protected READ_DTO changeEntity(ENTITY_ID id, Consumer<ENTITY> change) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(change, "change");
        ENTITY entity = loadForUpdate(id);
        Optional<READ_DTO> previous = this instanceof AbsUpdateChangesHookable
                ? Optional.of(mapReadDto(entity))
                : Optional.empty();
        change.accept(entity);
        return saveAndRunAfterHooks(entity, previous);
    }

    /**
     * Loads the managed entity that every write path changes. Override for a fetch join, or for a
     * check that must guard {@code update}, {@code patch} and {@code changeEntity} alike.
     *
     * @throws AppNotFoundException if no entity with that id exists
     */
    protected ENTITY loadForUpdate(ENTITY_ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppNotFoundException(format("Entity id: %s was not found", id)));
    }

    /**
     * Stores the changed entity on every write path. Override with {@code saveAndFlush} when the
     * response must carry values the database computes.
     */
    protected ENTITY saveUpdated(ENTITY entity) {
        return repository.save(entity);
    }

    private READ_DTO runUpdate(ENTITY_ID id, Object body) {
        Optional<READ_DTO> previous = tryBeforeUpdateHook(id, body);
        ENTITY entity = loadForUpdate(id);
        mapper.update(body, entity);
        return saveAndRunAfterHooks(entity, previous);
    }

    @SuppressWarnings("unchecked")
    private READ_DTO saveAndRunAfterHooks(ENTITY entity, Optional<READ_DTO> previous) {
        READ_DTO current = mapReadDto(saveUpdated(entity));
        afterUpdateHook(current);
        previous.ifPresent(p -> ((AbsUpdateChangesHookable<ENTITY_ID, READ_DTO>) this).afterUpdateHook(p, current));
        return current;
    }

    @SuppressWarnings("unchecked")
    protected Optional<READ_DTO> tryBeforeUpdateHook(ENTITY_ID id, Object body) {
        if (this instanceof AbsUpdateChangesHookable) {
            READ_DTO previous = getById(id);
            ((AbsUpdateChangesHookable<ENTITY_ID, READ_DTO>) this).beforeUpdateHook(previous, body);
            return Optional.of(previous);
        }
        return Optional.empty();
    }

    /**
     * Hook called by {@link #update} before anything is loaded. Not called by {@code patch} or
     * {@code changeEntity}: a check for every write path belongs in {@link #loadForUpdate}.
     *
     * @param dto the update DTO about to be applied
     */
    protected void beforeUpdateHook(UPDATE_DTO dto) {
    }

    /**
     * Hook called by {@link #patch} before anything is loaded.
     *
     * @param id   the id of the entity about to be changed
     * @param body the PATCH body about to be applied
     */
    protected void beforePatchHook(ENTITY_ID id, Object body) {
    }

    /**
     * Hook called after every write path, with the stored state.
     *
     * @param dto the stored entity as a READ_DTO
     */
    protected void afterUpdateHook(READ_DTO dto) {
    }

    private void checkId(IdEntity<ENTITY_ID> entity) {
        if (entity.isNew()) {
            throw new IllegalArgumentException(
                    format("Updated entity: %s should have id: (not null OR 0), but was id: %s", entity.getClass(), entity.getId()));
        }
    }

    /**
     * Deletes an entity by its ID.
     * <p>
     * This method removes the entity with the specified ID from the repository, effectively deleting it from the system.
     * The operation is idempotent: when no entity with the given id exists, the call is a silent no-op.
     * Hooks are provided for executing logic before and after the deletion; they run only when the entity exists.
     * </p>
     *
     * @param id the ID of the entity to delete
     */
    public void delete(ENTITY_ID id) {
        if (!repository.existsById(id)) {
            return;
        }
        beforeDeleteHook(id);
        repository.deleteById(id);
        afterDeleteHook(id);
    }

    /**
     * Hook method called before an existing entity is deleted.
     * <p>
     * Override this method in subclasses to implement custom logic to be executed before deleting an entity.
     * </p>
     *
     * @param id the ID of the entity about to be deleted
     */
    protected void beforeDeleteHook(ENTITY_ID id) {
    }

    /**
     * Hook method called after an entity is deleted.
     * <p>
     * Override this method in subclasses to implement custom logic to be executed after deleting an entity.
     * </p>
     *
     * @param id the ID of the deleted entity
     */
    protected void afterDeleteHook(ENTITY_ID id) {
    }
}
```

- [ ] **Step 5: Typed `mapEntity` in `AbsFlexServiceCRUD`, delete `FieldCopyUtil`**

In `library/src/main/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceCRUD.java`:

1. In the `persistOrMerge` javadoc replace

   ```java
        * every save path funnels through — so application mappers that override {@code toEntity(...)}
        * and bypass the base converter's normalisation still route a new entity to persist.
   ```

   with

   ```java
        * every save path funnels through — so a create mapper that skips {@code nullifyZeroId()}
        * (a plain {@code Mapper.of} bean, say) still routes a new entity to persist.
   ```

2. Insert after the `afterSaveAllHook` method, before the class's closing `}`:

   ```java

       /**
        * Maps a create DTO to a new entity with the {@code Mapper<CREATE_DTO, ENTITY>} from the registry.
        *
        * @param dto the create DTO
        * @return the new, not yet persisted entity
        */
       protected ENTITY mapEntity(CREATE_DTO dto) {
           return mapper.map(dto, entityClass);
       }

       /**
        * Maps create DTOs to new entities with {@link #mapEntity}'s mapper.
        *
        * @param dtos the create DTOs
        * @return a new mutable list of new, not yet persisted entities
        */
       protected List<ENTITY> mapAllEntities(Collection<CREATE_DTO> dtos) {
           return mapper.mapAll(dtos, entityClass);
       }
   ```

Then:

```bash
cd /d/projects/crud-generic
git rm -q library/src/main/java/by/nhorushko/crudgeneric/flex/util/FieldCopyUtil.java
grep -rn "FieldUtils\|java.lang.reflect" library/src/main/java/by/nhorushko/crudgeneric/flex || echo "clean"
grep -rli "modelmapper\|updatePartial\|FieldCopyUtil" library/src library/pom.xml || echo "clean"
```

Expected: `clean` twice.

- [ ] **Step 6: Run the library build**

Run: `./mvnw -B -pl library verify`
Expected: `BUILD SUCCESS`, `Tests run: 106` (98 + 8), 0 failures.

- [ ] **Step 7: WIP commit**

```bash
git add -A library
git commit -F - <<'EOF'
wip: patch, changeEntity and write seams

Squashed into "feat!: replace ModelMapper with explicit mappers".

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 5: Migrate test-application and squash into the breaking commit

**Files:**
- Delete: `test-application/src/main/java/by/nhorushko/crudgenerictest/config/ModelMapperConfig.java`, `.../mapper/OrderLineMapConfig.java`
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/mapper/OrderLineMapper.java`, `.../domain/dto/OrderNamePatch.java`
- Rewrite: `.../mapper/OrderMapConfig.java`, `.../mapper/RegionMapConfig.java`, `.../mapper/TaskMapConfig.java`, `.../mapper/TaskExtMapper.java`
- Modify: `.../mapper/MeetingMapper.java`, `.../mapper/OrderViewMapper.java`, `.../service/MeetingPageableService.java`, `.../service/RegionServiceCRUD.java`, `.../service/TaskServiceExtCRUD.java` (type only), `.../service/OrderServiceCRUD.java` (type + `rename`), `.../domain/dto/OrderView.java`, `.../domain/entity/TaskEntity.java` (javadoc)
- Delete (tests): `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/` (3 files), `.../util/FieldCopyUtilTest.java`
- Rewrite (tests): `.../service/FlexUpdateIT.java`, `.../service/FlexSaveOverridingMapperIT.java`, `.../mapper/FlexTwoConfigsForSameEntityIT.java`
- Create (tests): `.../service/FlexPatchIT.java`, `.../mapper/MapperRegistryLazyInitIT.java`, `.../mapper/HibernateProxyMappingIT.java`

Main paths are under `test-application/src/main/java/by/nhorushko/crudgenerictest/`, test paths under `test-application/src/test/java/by/nhorushko/crudgenerictest/`.

**Interfaces:**
- Consumes (Tasks 2–4): `AbsMapper`, `AbsFlexMapConfig`, `Patches`, `AbsMapDtoToEntity`, `AbsMapEntityToDto`, `AbsMapperExtRelation.setRelation`, `Mapper.of`, `MapperRegistry.findMapper` / `findUpdater`, `MappingNotFoundException`, `AbsFlexServiceRUD.patch` / `changeEntity` / `loadForUpdate` / `saveUpdated` / `beforePatchHook`.
- Produces: `record OrderNamePatch(String name)`; `OrderServiceCRUD.rename(Long id, String name)`; component `OrderLineMapper` (`Mapper<OrderLineDto, OrderLineEntity>`).

- [ ] **Step 1: Write the new and changed integration tests first**

Replace the whole content of `test-application/src/test/java/by/nhorushko/crudgenerictest/service/FlexUpdateIT.java`:

```java
package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.exception.AppNotFoundException;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderNamePatch;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.domain.entity.OrderLineEntity;
import by.nhorushko.crudgenerictest.repository.OrderLineRepository;
import by.nhorushko.crudgenerictest.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression tests for the flex update path: an UPDATE_DTO deliberately carries
 * only a subset of the entity's fields ({@code secretCode} and {@code lines} are
 * absent), and updating through it must not wipe the fields it does not carry.
 */
@SpringBootTest
class FlexUpdateIT {

    @Autowired
    private OrderServiceCRUD service;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderLineRepository lineRepository;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void updateAppliesDtoFields() {
        OrderEntity order = persistedOrder("old", "s3cret");

        OrderDto updated = service.update(new OrderUpdateDto(order.getId(), "new"));

        assertThat(updated.getName()).isEqualTo("new");
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getName()).isEqualTo("new");
    }

    @Test
    void updatePreservesEntityFieldAbsentFromUpdateDto() {
        OrderEntity order = persistedOrder("old", "s3cret");

        service.update(new OrderUpdateDto(order.getId(), "new"));

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getSecretCode())
                .isEqualTo("s3cret");
    }

    @Test
    void updatePreservesChildrenWhenUpdateDtoOmitsThem() {
        OrderEntity order = persistedOrder("old", "s3cret", "line-1", "line-2");
        assertThat(lineRepository.count()).isEqualTo(2L);

        service.update(new OrderUpdateDto(order.getId(), "new"));

        assertThat(lineRepository.count()).isEqualTo(2L);
    }

    @Test
    void patchPreservesFieldsAbsentFromPatchBody() {
        OrderEntity order = persistedOrder("old", "s3cret", "line-1");

        service.patch(order.getId(), new OrderNamePatch("new"));

        OrderEntity actual = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(actual.getName()).isEqualTo("new");
        assertThat(actual.getSecretCode()).isEqualTo("s3cret");
        assertThat(lineRepository.count()).isEqualTo(1L);
    }

    /**
     * The library gives null no meaning of its own: OrderMapConfig.updateEntity writes the name as
     * is, so a null in the DTO clears it. This pins the demo's choice, not a recommendation.
     */
    @Test
    void updateWithNullNameClearsItBecauseUpdateEntityWritesItAsIs() {
        OrderEntity order = persistedOrder("old", "s3cret");

        service.update(new OrderUpdateDto(order.getId(), null));

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getName()).isNull();
    }

    @Test
    void updateMissingIdThrowsAppNotFound() {
        assertThatThrownBy(() -> service.update(new OrderUpdateDto(999_999L, "x")))
                .isInstanceOf(AppNotFoundException.class);
        assertThat(orderRepository.count()).isZero();
    }

    private OrderEntity persistedOrder(String name, String secretCode, String... lineTitles) {
        OrderEntity order = new OrderEntity();
        order.setName(name);
        order.setSecretCode(secretCode);
        for (String title : lineTitles) {
            OrderLineEntity line = new OrderLineEntity();
            line.setTitle(title);
            order.getLines().add(line);
        }
        return orderRepository.save(order);
    }
}
```

Create `test-application/src/test/java/by/nhorushko/crudgenerictest/service/FlexPatchIT.java`:

```java
package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderNamePatch;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The write paths of AbsFlexServiceRUD on the real Spring/Hibernate stack: patch(id, body) goes
 * through the Updater declared in OrderMapConfig.patches(), rename goes through changeEntity, a body
 * without an Updater fails loudly, and the overridden loadForUpdate / saveUpdated seams sit on all
 * three paths. A dedicated H2 url forks the cached context so the extra service bean stays local.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:patchdb;NON_KEYWORDS=USER")
class FlexPatchIT {

    @Autowired
    private RecordingOrderService service;
    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
        service.events().clear();
    }

    @Test
    void patchGoesThroughRegisteredUpdaterAndUpdateHooks() {
        OrderEntity order = persistedOrder("old");

        OrderDto patched = service.patch(order.getId(), new OrderNamePatch("new"));

        assertThat(patched.getName()).isEqualTo("new");
        OrderEntity actual = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(actual.getName()).isEqualTo("new");
        assertThat(actual.getSecretCode()).isEqualTo("s3cret");
        assertThat(service.events()).containsExactly("beforePatch", "load", "save", "afterUpdate:new");
    }

    @Test
    void renameGoesThroughChangeEntityWithoutBeforeHooks() {
        OrderEntity order = persistedOrder("old");

        OrderDto renamed = service.rename(order.getId(), "renamed");

        assertThat(renamed.getName()).isEqualTo("renamed");
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getName()).isEqualTo("renamed");
        assertThat(service.events()).containsExactly("load", "save", "afterUpdate:renamed");
    }

    @Test
    void patchBodyWithoutUpdaterFailsWithMappingNotFound() {
        OrderEntity order = persistedOrder("old");

        assertThatThrownBy(() -> service.patch(order.getId(), new UnregisteredPatch("x")))
                .isInstanceOf(MappingNotFoundException.class)
                .hasMessageContaining(UnregisteredPatch.class.getName());
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getName()).isEqualTo("old");
        assertThat(service.events()).doesNotContain("save");
    }

    @Test
    void overriddenSeamsRunOnAllThreeWritePaths() {
        OrderEntity order = persistedOrder("old");

        service.update(new OrderUpdateDto(order.getId(), "via-update"));
        service.patch(order.getId(), new OrderNamePatch("via-patch"));
        service.rename(order.getId(), "via-change");

        assertThat(service.events()).filteredOn("load"::equals).hasSize(3);
        assertThat(service.events()).filteredOn("save"::equals).hasSize(3);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getName()).isEqualTo("via-change");
    }

    private OrderEntity persistedOrder(String name) {
        OrderEntity order = new OrderEntity();
        order.setName(name);
        order.setSecretCode("s3cret");
        return orderRepository.save(order);
    }

    @TestConfiguration
    static class Config {
        @Bean
        RecordingOrderService recordingOrderService(AbsMapper mapper, OrderRepository repository) {
            return new RecordingOrderService(mapper, repository);
        }
    }

    /**
     * The bean is a CGLIB proxy (class-level @Transactional), and a proxy's own fields are never
     * initialised: read the events through the public events() method, never the field.
     */
    static class RecordingOrderService extends OrderServiceCRUD {
        private final List<String> events = new ArrayList<>();

        RecordingOrderService(AbsMapper mapper, OrderRepository repository) {
            super(mapper, repository);
        }

        public List<String> events() {
            return events;
        }

        @Override
        protected void beforeUpdateHook(OrderUpdateDto dto) {
            events.add("beforeUpdate");
        }

        @Override
        protected void beforePatchHook(Long id, Object body) {
            events.add("beforePatch");
        }

        @Override
        protected OrderEntity loadForUpdate(Long id) {
            events.add("load");
            return super.loadForUpdate(id);
        }

        @Override
        protected OrderEntity saveUpdated(OrderEntity entity) {
            events.add("save");
            return super.saveUpdated(entity);
        }

        @Override
        protected void afterUpdateHook(OrderDto dto) {
            events.add("afterUpdate:" + dto.getName());
        }
    }

    record UnregisteredPatch(String name) {
    }
}
```

Create `test-application/src/test/java/by/nhorushko/crudgenerictest/mapper/MapperRegistryLazyInitIT.java`:

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgenerictest.domain.dto.MeetingDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderLineDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderNamePatch;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderView;
import by.nhorushko.crudgenerictest.domain.dto.RegionCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionUpdateDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.MeetingEntity;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.domain.entity.OrderLineEntity;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import by.nhorushko.crudgenerictest.domain.entity.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Under global lazy init nothing is created at startup, yet the registry must still hold every
 * mapper bean of the demo once it is built: it pulls them all in through constructor injection.
 * Replaces the eager-init tests of 14.x.
 */
@SpringBootTest(properties = {
        "spring.main.lazy-initialization=true",
        "spring.datasource.url=jdbc:h2:mem:lazytestdb;NON_KEYWORDS=USER"
})
class MapperRegistryLazyInitIT {

    @Autowired
    private MapperRegistry registry;
    @Autowired
    private AbsMapper mapper;

    @Test
    void registryHoldsEveryDemoMapperUnderLazyInit() {
        assertThat(registry.findMapper(OrderCreateDto.class, OrderEntity.class)).isPresent();
        assertThat(registry.findMapper(OrderEntity.class, OrderDto.class)).isPresent();
        assertThat(registry.findUpdater(OrderUpdateDto.class, OrderEntity.class)).isPresent();
        assertThat(registry.findUpdater(OrderNamePatch.class, OrderEntity.class)).isPresent();
        assertThat(registry.findMapper(OrderLineDto.class, OrderLineEntity.class)).isPresent();
        assertThat(registry.findMapper(OrderEntity.class, OrderView.class)).isPresent();
        assertThat(registry.findMapper(RegionCreateDto.class, RegionEntity.class)).isPresent();
        assertThat(registry.findMapper(RegionEntity.class, RegionDto.class)).isPresent();
        assertThat(registry.findUpdater(RegionUpdateDto.class, RegionEntity.class)).isPresent();
        assertThat(registry.findMapper(TaskCreateDto.class, TaskEntity.class)).isPresent();
        assertThat(registry.findMapper(TaskEntity.class, TaskDto.class)).isPresent();
        assertThat(registry.findUpdater(TaskUpdateDto.class, TaskEntity.class)).isPresent();
        assertThat(registry.findMapper(MeetingEntity.class, MeetingDto.class)).isPresent();
    }

    @Test
    void mapToImmutableViewWorksUnderLazyInit() {
        OrderEntity entity = new OrderEntity();
        entity.setId(42L);
        entity.setName("alice");

        assertThat(mapper.map(entity, OrderView.class)).isEqualTo(new OrderView(42L, "alice"));
    }
}
```

Create `test-application/src/test/java/by/nhorushko/crudgenerictest/mapper/HibernateProxyMappingIT.java` (Review Focus #1):

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Hibernate proxy's class is an unannotated runtime subclass of the entity. The registry
 * normalises it to the nearest @Entity ancestor, so a lazy reference maps with the entity's pair
 * both as a Mapper source and as an Updater destination. Each test rolls back.
 */
@SpringBootTest
@Transactional
class HibernateProxyMappingIT {

    @Autowired
    private AbsMapper mapper;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void proxyAsMapperSourceUsesTheEntityPair() {
        RegionEntity proxy = storedRegionProxy(7L, "north");

        assertThat(mapper.map(proxy, RegionDto.class)).isEqualTo(new RegionDto(7L, "north"));
    }

    @Test
    void proxyAsUpdaterDestinationUsesTheEntityPair() {
        RegionEntity proxy = storedRegionProxy(8L, "south");

        mapper.update(new RegionUpdateDto(8L, "renamed"), proxy);

        assertThat(proxy.getName()).isEqualTo("renamed");
    }

    private RegionEntity storedRegionProxy(Long id, String name) {
        entityManager.persist(new RegionEntity(id, name));
        entityManager.flush();
        entityManager.clear();
        RegionEntity proxy = entityManager.getReference(RegionEntity.class, id);
        assertThat(proxy.getClass()).isNotEqualTo(RegionEntity.class);
        return proxy;
    }
}
```

Replace the whole content of `test-application/src/test/java/by/nhorushko/crudgenerictest/service/FlexSaveOverridingMapperIT.java`:

```java
package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.repository.OrderRepository;
import lombok.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A create mapper declared as a plain {@code Mapper.of} bean — not built on AbsMapDtoToEntity or
 * AbsFlexMapConfig — copies the sentinel id 0 verbatim into the entity, skipping their
 * nullifyZeroId. persistOrMerge normalises the sentinel itself: the save must INSERT, not
 * merge/fail. Flex twin of the deleted v2 SaveOverridingMapperIT.
 */
@SpringBootTest
class FlexSaveOverridingMapperIT {

    @Autowired
    private OverridingOrderService service;
    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void sentinelZeroIdInsertsEvenWhenMapperBypassesBaseNormalisation() {
        OrderDto saved = service.save(new ZeroIdOrderCreate(0L, "ov-order"));

        assertThat(saved.getId()).isNotNull();
        assertThat(orderRepository.existsById(saved.getId())).isTrue();
    }

    @TestConfiguration
    static class Config {
        /** A distinct source type, so no collision with OrderMapConfig's OrderCreateDto -> OrderEntity. */
        @Bean
        Mapper<ZeroIdOrderCreate, OrderEntity> zeroIdOrderCreateMapper() {
            return Mapper.of(ZeroIdOrderCreate.class, OrderEntity.class, dto -> {
                OrderEntity entity = new OrderEntity();
                entity.setId(dto.getId()); // 0L sentinel copied verbatim — NOT normalised
                entity.setName(dto.getName());
                return entity;
            });
        }

        @Bean
        OverridingOrderService overridingOrderService(AbsMapper mapper, OrderRepository repository) {
            return new OverridingOrderService(mapper, repository);
        }
    }

    static class OverridingOrderService
            extends AbsFlexServiceCRUD<Long, OrderEntity, OrderDto, OrderUpdateDto, ZeroIdOrderCreate, OrderRepository> {
        OverridingOrderService(AbsMapper mapper, OrderRepository repository) {
            super(mapper, repository, OrderEntity.class, OrderDto.class, OrderUpdateDto.class, ZeroIdOrderCreate.class);
        }
    }

    @Value
    static class ZeroIdOrderCreate implements AbsCreateDto {
        Long id;
        String name;
    }
}
```

Replace the whole content of `test-application/src/test/java/by/nhorushko/crudgenerictest/mapper/FlexTwoConfigsForSameEntityIT.java`:

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsFlexMapConfig;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring startup with TWO {@code AbsFlexMapConfig} beans for the same entity ({@code OrderEntity}):
 * the production {@code OrderMapConfig} plus a second config with its own DTO triple. They register
 * different pairs, so the registry builds without a duplicate and both DTO sets keep working. A
 * dedicated H2 url forks the cached test context so the extra config does not leak into the other
 * integration tests.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:twoconfigsdb;NON_KEYWORDS=USER")
class FlexTwoConfigsForSameEntityIT {

    @TestConfiguration
    static class SecondOrderConfig {
        @Bean
        AbsFlexMapConfig<OrderSummaryCreateDto, OrderSummaryUpdateDto, OrderSummaryDto, OrderEntity> orderSummaryMapConfig(AbsMapper mapper) {
            return new AbsFlexMapConfig<>(mapper,
                    OrderSummaryCreateDto.class, OrderSummaryUpdateDto.class, OrderSummaryDto.class, OrderEntity.class) {
                @Override
                protected OrderEntity toEntity(OrderSummaryCreateDto dto) {
                    OrderEntity entity = new OrderEntity();
                    entity.setName(dto.getName());
                    return entity;
                }

                @Override
                protected void updateEntity(OrderSummaryUpdateDto dto, OrderEntity entity) {
                    entity.setName(dto.getName());
                }

                @Override
                protected OrderSummaryDto toReadDto(OrderEntity entity) {
                    return new OrderSummaryDto(entity.getId(), entity.getName());
                }
            };
        }
    }

    @Autowired
    private AbsMapper mapper;

    @Test
    void bothConfigsMapEntityToTheirReadDto() {
        OrderEntity entity = new OrderEntity();
        entity.setId(7L);
        entity.setName("order-7");

        assertThat(mapper.map(entity, OrderDto.class)).isEqualTo(new OrderDto(7L, "order-7"));
        assertThat(mapper.map(entity, OrderSummaryDto.class)).isEqualTo(new OrderSummaryDto(7L, "order-7"));
    }

    @Test
    void secondConfigCreateDtoMapsToEntity() {
        OrderEntity entity = mapper.map(new OrderSummaryCreateDto("summary-order"), OrderEntity.class);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getName()).isEqualTo("summary-order");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OrderSummaryDto implements AbstractDto<Long> {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OrderSummaryUpdateDto implements AbsUpdateDto<Long> {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OrderSummaryCreateDto implements AbsCreateDto {
        private String name;
    }
}
```

Delete the obsolete tests:

```bash
cd /d/projects/crud-generic
git rm -q -r test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit
git rm -q test-application/src/test/java/by/nhorushko/crudgenerictest/util/FieldCopyUtilTest.java
```

- [ ] **Step 2: Run to see the failure**

Run: `./mvnw -B verify`
Expected: library `BUILD SUCCESS` part, then test-application `COMPILATION ERROR` — `AbsModelMapper`, `AbsFlexMapConfigDefault`, `org.modelmapper` and `OrderNamePatch` not found.

- [ ] **Step 3: Patch body and mapping configs**

Create `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/OrderNamePatch.java`:

```java
package by.nhorushko.crudgenerictest.domain.dto;

/**
 * PATCH body that renames an order. No id: the id comes from the path.
 */
public record OrderNamePatch(String name) {
}
```

Replace the whole content of `test-application/src/main/java/by/nhorushko/crudgenerictest/mapper/OrderMapConfig.java`:

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsFlexMapConfig;
import by.nhorushko.crudgeneric.flex.mapper.Patches;
import by.nhorushko.crudgenerictest.domain.dto.OrderCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderNamePatch;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.domain.entity.OrderLineEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderMapConfig extends AbsFlexMapConfig<OrderCreateDto, OrderUpdateDto, OrderDto, OrderEntity> {

    public OrderMapConfig(AbsMapper mapper) {
        super(mapper, OrderCreateDto.class, OrderUpdateDto.class, OrderDto.class, OrderEntity.class);
    }

    @Override
    protected OrderEntity toEntity(OrderCreateDto dto) {
        OrderEntity entity = new OrderEntity();
        entity.setName(dto.getName());
        entity.setLines(mapper.mapAll(dto.getLines(), OrderLineEntity.class)); // nested children: explicit
        return entity;
    }

    /** Writes the name as is: a null in the DTO clears it (see FlexUpdateIT). */
    @Override
    protected void updateEntity(OrderUpdateDto dto, OrderEntity entity) {
        entity.setName(dto.getName());
    }

    @Override
    protected OrderDto toReadDto(OrderEntity entity) {
        return new OrderDto(entity.getId(), entity.getName());
    }

    @Override
    protected void patches(Patches<OrderEntity> p) {
        p.add(OrderNamePatch.class, (patch, entity) -> entity.setName(patch.name()));
    }
}
```

Replace the whole content of `test-application/src/main/java/by/nhorushko/crudgenerictest/mapper/RegionMapConfig.java`:

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsFlexMapConfig;
import by.nhorushko.crudgenerictest.domain.dto.RegionCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import org.springframework.stereotype.Component;

@Component
public class RegionMapConfig extends AbsFlexMapConfig<RegionCreateDto, RegionUpdateDto, RegionDto, RegionEntity> {

    public RegionMapConfig(AbsMapper mapper) {
        super(mapper, RegionCreateDto.class, RegionUpdateDto.class, RegionDto.class, RegionEntity.class);
    }

    /** Region ids are assigned by the client and save() is an upsert, so the id must be copied. */
    @Override
    protected RegionEntity toEntity(RegionCreateDto dto) {
        RegionEntity entity = new RegionEntity();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        return entity;
    }

    @Override
    protected void updateEntity(RegionUpdateDto dto, RegionEntity entity) {
        entity.setName(dto.getName());
    }

    @Override
    protected RegionDto toReadDto(RegionEntity entity) {
        return new RegionDto(entity.getId(), entity.getName());
    }
}
```

Replace the whole content of `test-application/src/main/java/by/nhorushko/crudgenerictest/mapper/TaskMapConfig.java`:

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsFlexMapConfig;
import by.nhorushko.crudgenerictest.domain.dto.TaskCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskMapConfig extends AbsFlexMapConfig<TaskCreateDto, TaskUpdateDto, TaskDto, TaskEntity> {

    public TaskMapConfig(AbsMapper mapper) {
        super(mapper, TaskCreateDto.class, TaskUpdateDto.class, TaskDto.class, TaskEntity.class);
    }

    /**
     * Copies the id: the sentinel 0 must become a new row, and a real id must reach the ext create
     * path so that it can reject it.
     */
    @Override
    protected TaskEntity toEntity(TaskCreateDto dto) {
        TaskEntity entity = new TaskEntity();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        return entity;
    }

    @Override
    protected void updateEntity(TaskUpdateDto dto, TaskEntity entity) {
        entity.setTitle(dto.getTitle());
    }

    @Override
    protected TaskDto toReadDto(TaskEntity entity) {
        return new TaskDto(entity.getId(), entity.getTitle());
    }
}
```

Replace the whole content of `test-application/src/main/java/by/nhorushko/crudgenerictest/mapper/TaskExtMapper.java`:

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapperExtRelation;
import by.nhorushko.crudgenerictest.domain.dto.TaskCreateDto;
import by.nhorushko.crudgenerictest.domain.entity.ProjectEntity;
import by.nhorushko.crudgenerictest.domain.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskExtMapper extends AbsMapperExtRelation<TaskCreateDto, TaskEntity, Long, ProjectEntity> {

    public TaskExtMapper(AbsMapper mapper) {
        super(mapper, TaskEntity.class, ProjectEntity.class);
    }

    @Override
    protected void setRelation(TaskEntity task, ProjectEntity project) {
        task.setProject(project);
    }
}
```

Replace `OrderLineMapConfig` with a single create-direction mapper:

```bash
git rm -q test-application/src/main/java/by/nhorushko/crudgenerictest/mapper/OrderLineMapConfig.java
git rm -q test-application/src/main/java/by/nhorushko/crudgenerictest/config/ModelMapperConfig.java
```

Create `test-application/src/main/java/by/nhorushko/crudgenerictest/mapper/OrderLineMapper.java`:

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapDtoToEntity;
import by.nhorushko.crudgenerictest.domain.dto.OrderLineDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderLineEntity;
import org.springframework.stereotype.Component;

/**
 * Nested order lines: {@code OrderMapConfig.toEntity} maps them through this mapper, which
 * normalises the sentinel id 0 of a new line to null. Create direction only — nothing maps lines
 * back to DTOs.
 */
@Component
public class OrderLineMapper extends AbsMapDtoToEntity<OrderLineDto, OrderLineEntity> {

    public OrderLineMapper(AbsMapper mapper) {
        super(mapper, OrderLineDto.class, OrderLineEntity.class);
    }

    @Override
    protected OrderLineEntity create(OrderLineDto from) {
        OrderLineEntity line = new OrderLineEntity();
        line.setId(from.getId());
        line.setTitle(from.getTitle());
        return line;
    }
}
```

- [ ] **Step 4: Remaining main sources**

```bash
cd /d/projects/crud-generic/test-application/src/main/java/by/nhorushko/crudgenerictest
sed -i 's/AbsModelMapper/AbsMapper/g' mapper/MeetingMapper.java mapper/OrderViewMapper.java \
  service/MeetingPageableService.java service/OrderServiceCRUD.java service/RegionServiceCRUD.java \
  service/TaskServiceExtCRUD.java
```

In `service/OrderServiceCRUD.java`, insert after the constructor, before the class's closing `}`:

```java

    /**
     * Renames the order through {@code changeEntity}: a change written in code, no request body, so
     * only the after-update hooks run.
     */
    public OrderDto rename(Long id, String name) {
        return changeEntity(id, order -> order.setName(name));
    }
```

In `domain/dto/OrderView.java` replace the class javadoc with:

```java
/**
 * Immutable (@Value — no no-arg constructor): built only by {@code OrderViewMapper}.
 * {@code MapperRegistryLazyInitIT} maps into it under global lazy init.
 */
```

In `domain/entity/TaskEntity.java` replace the class javadoc with:

```java
/**
 * Child side of the flex ext fixture: {@code TaskExtMapper.setRelation} links a new task to
 * its {@link ProjectEntity}.
 */
```

- [ ] **Step 5: Run the full build**

Run: `./mvnw -B verify`
Expected: `BUILD SUCCESS`; library `Tests run: 106`; test-application surefire `Tests run: 3` (only `AbstractEntityNullifyZeroIdTest`), failsafe `Tests run: 40` (31 + 1 `FlexUpdateIT` + 4 `FlexPatchIT` + 2 `MapperRegistryLazyInitIT` + 2 `HibernateProxyMappingIT`), 0 failures. If `FlexAssignedIdSaveIT` or `FlexExtSaveIT` is red, fix the config's `toEntity` (the id copy), never the test.

- [ ] **Step 6: Verification greps from the spec**

```bash
cd /d/projects/crud-generic
grep -ri modelmapper library/src test-application/src library/pom.xml || echo "clean 1"
grep -rn "FieldUtils\|java.lang.reflect" library/src/main/java/by/nhorushko/crudgeneric/flex || echo "clean 2"
grep -rn "updatePartial\|FieldCopyUtil\|RegisterableMapper\|AbsModelMapper\|AbsTypeMapChecker\|AbsMappingChecker\|AbsCrudCustomizer" library/src test-application/src || echo "clean 3"
```

Expected: `clean 1`, `clean 2`, `clean 3`. Any hit is a leftover — fix it and rerun Step 5.

- [ ] **Step 7: Squash Tasks 3–5 into the breaking commit**

```bash
cd /d/projects/crud-generic
git add -A library test-application
REGISTRY_COMMIT=$(git log --format=%H -1 --grep='^feat(mapper): explicit Mapper/Updater registry$')
echo "$REGISTRY_COMMIT"
git reset --soft "$REGISTRY_COMMIT"
git commit -F - <<'EOF'
feat!: replace ModelMapper with explicit mappers

BREAKING CHANGE: ModelMapper is gone. Every DTO <-> entity conversion is
consumer code that MapperRegistry finds by the exact class pair.

- AbsModelMapper -> AbsMapper; the in-place map becomes update(source,
  target), getModelMapper() is gone
- AbsFlexMapConfig (toEntity / updateEntity / toReadDto / patches)
  replaces AbsFlexMapConfigDefault/Abstract; AbsMapEntityToDto,
  AbsMapDtoToEntity and AbsMapUpdateDtoToEntity have one abstract method
- updatePartial -> patch(id, body) with an explicit Updater per body,
  plus changeEntity(id, change); loadForUpdate / saveUpdated seams on
  every write path; beforeUpdateHook typed UPDATE_DTO
- AbsMapperExtRelation moves to flex.mapper, setRelation is abstract
- Removed: startup TypeMap checker, AbsCrudCustomizer, eager-init post
  processor, FieldCopyUtil, the org.modelmapper dependency
- test-application migrated; new FlexPatchIT, MapperRegistryLazyInitIT
  and HibernateProxyMappingIT

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
git log --oneline -4
git status --short
```

Expected: `REGISTRY_COMMIT` is a non-empty sha; the log shows `feat!: replace ModelMapper with explicit mappers`, `feat(mapper): explicit Mapper/Updater registry`, `chore: start 15.0`, `docs(spec): ...`; the working tree is clean.

---

### Task 6: README, CHANGELOG and CLAUDE.md

**Files:**
- Modify: `README.md`, `CHANGELOG.md`, `CLAUDE.md`

**Interfaces:**
- Consumes: the API as shipped in Tasks 2–5.
- Produces: documentation only.

- [ ] **Step 1: README — intro, features, prerequisites, step 1**

In `README.md`:

1. Replace

   ```
   The Generic CRUD Framework simplifies the development of Spring Boot applications by providing a structured approach to mapping Data Transfer Objects (DTOs) to entities and implementing CRUD operations. Leveraging the power of ModelMapper and abstract classes, it streamlines the creation of services and controllers with minimal boilerplate code.
   ```

   with

   ```
   The Generic CRUD Framework simplifies the development of Spring Boot applications by providing a structured approach to mapping Data Transfer Objects (DTOs) to entities and implementing CRUD operations. Mapping is explicit: every conversion is code you write, found by a registry through the exact pair of classes — nothing is copied by matching field names.
   ```

2. Replace

   ```
   * Simplified DTO to Entity mappings and vice versa.
   * Abstract configurations for easy mapping between DTOs and entities.
   ```

   with

   ```
   * Explicit DTO to entity mappings and vice versa: one config per entity, no implicit field copying.
   * A mapper registry that fails loudly on a missing or duplicate pair of classes.
   ```

3. Delete the line `* ModelMapper` under `### Prerequisites`.

4. Replace

   ```
   Before diving into the specifics of entity and DTO creation, enable the framework in your Spring Boot application by using the @EnableAbsGenericCrud annotation. This step is crucial as it sets up the necessary configurations for ModelMapper and other components required by the framework.
   ```

   with

   ```
   Before diving into the specifics of entity and DTO creation, enable the framework in your Spring Boot application by using the @EnableAbsGenericCrud annotation. It registers the mapper registry and the `AbsMapper` facade the services map through.
   ```

- [ ] **Step 2: README — steps 4 and 5**

Replace everything from the line `### Step 4: Implement Mapping Configurations` up to (not including) `### Step 6: Develop Controllers` with:

````markdown
### Step 4: Implement Mapping Configurations
Extend `AbsFlexMapConfig` — one config per entity. Every field is written explicitly; nothing is copied by matching names. Declare one `Updater` per PATCH body class in `patches()`.

```java
@Component
public class MyMappingConfig extends AbsFlexMapConfig<MyCreateDto, MyUpdateDto, MyReadDto, MyEntity> {

    public MyMappingConfig(AbsMapper mapper) {
        super(mapper, MyCreateDto.class, MyUpdateDto.class, MyReadDto.class, MyEntity.class);
    }

    @Override
    protected MyEntity toEntity(MyCreateDto dto) {
        MyEntity entity = new MyEntity();
        entity.setName(dto.getName());
        entity.setItems(mapper.mapAll(dto.getItems(), MyItemEntity.class)); // nested children: explicit
        return entity;
    }

    @Override
    protected void updateEntity(MyUpdateDto dto, MyEntity entity) {
        entity.setName(dto.getName());
    }

    @Override
    protected MyReadDto toReadDto(MyEntity entity) {
        return new MyReadDto(entity.getId(), entity.getName());
    }

    @Override
    protected void patches(Patches<MyEntity> p) {
        p.add(MyNamePatch.class, (patch, entity) -> entity.setName(patch.name()));
    }
}
```

For a single direction use `AbsMapEntityToDto` (view DTOs, read-only services), `AbsMapDtoToEntity` (nested children) or `AbsMapUpdateDtoToEntity`; for a one-off pair declare a bean `Mapper.of(From.class, To.class, fn)`. A missing pair fails with `MappingNotFoundException` on the first call; a pair registered twice fails at startup.

### Step 5: Create Services
Extend AbsFlexServiceCRUD or AbsFlexServiceExtCRUD for CRUD services. Implement abstract methods and use the provided functionalities.

```java
@Service
public class MyEntityService extends AbsFlexServiceCRUD<Long, MyEntity, MyReadDto, MyUpdateDto, MyCreateDto, MyRepository> {
    // Constructor and methods
}
```

`update(dto)`, `patch(id, body)` and the protected `changeEntity(id, change)` all load the entity through `loadForUpdate(id)` and store it through `saveUpdated(entity)` — override those two seams instead of `update`.

````

- [ ] **Step 3: README — pageable example and the migration section**

1. In the `RtRoutePageableService` example replace `RtRouteRepository repository, AbsModelMapper mapper` with `RtRouteRepository repository, AbsMapper mapper`.

2. Insert this section right before the line `## Migration to 5.0 (flex-only)`:

````markdown
## Миграция на 15.0 (явные мапперы)

15.0 удаляет ModelMapper. Каждое преобразование DTO ↔ entity — код потребителя, который
`MapperRegistry` находит по точной паре классов. Ничего не копируется по совпадению имён
полей, и `null` сам по себе ничего не значит.

**Порядок для потребителя на `13.3.15-jakarta`:** сначала хвост v1/v2 переводится на flex на
той же 13.3.15 (таблица «Migration to 5.0 (flex-only)» ниже), затем 13.3.15 → 15.0 одним шагом
по таблице этого раздела. **Через 14.0 не ходить:** там `updatePartial` неявно кладёт read DTO
на managed-сущность, и через этот путь пошли бы все PATCH. Окно без компиляции сокращается
подготовкой на 13.3.15: переопределить `setRelation` во всех наследниках `AbsMapperExtRelation`
(метод там уже protected) и сделать явными поля, которые сегодня копируются по имени.

Таблица составлена по API 13.3.15, на котором сидят потребители, а не по 14.0.

### Было (13.3.15) → стало (15.0)

| 13.3.15 | 15.0 |
|---|---|
| `AbsModelMapper` | `AbsMapper`, тот же пакет `by.nhorushko.crudgeneric.flex`. `map(x, Y.class)`, `mapAll`, `reference`, `referenceById`, `getEntityManager()` не меняются — меняется тип параметра конструкторов |
| `AbsModelMapper.getModelMapper()` | удалён; каждое прямое обращение к ModelMapper становится `Mapper` / `Updater` |
| in-place `map(source, destination)` | `update(source, destination)` через зарегистрированный `Updater` |
| `AbsFlexMapConfigDefault`, `AbsFlexMapConfigAbstract` | `AbsFlexMapConfig`: `toEntity`, `updateEntity`, `toReadDto` и необязательный `patches(p)` |
| фабрика `mapperCreateDtoToEntity(...)` + `mapSpecificFieldsCreateDtoToEntity(mapper, dto, entity)` поверх неявного копирования | `toEntity(dto)`: `new Entity()` + те же строки + поля, которые раньше копировались по имени, включая `id`, если он нужен (assigned id, sentinel `0`) |
| фабрика `mapperUpdateDtoToEntity(...)` + `mapSpecificFieldsUpdateDtoToEntity` | `updateEntity(dto, entity)` на managed-сущности |
| `AbsMapUpdateDtoToPresetEntity` из `mapperUpdateDtoToEntity(...)` | `updateEntity(dto, entity)`; фабрика исчезает |
| `handleAfterMapSpecificFields(source, received)`: `entityManager.find` + перенос полей | загрузка — `loadForUpdate(id)` сервиса (по умолчанию `findById`; переопределяется ради fetch join или фильтра), перенос — тело `updateEntity` |
| `mergeActualAndReceived(actual, received)` | то же тело в `updateEntity`, только источник — DTO, а не `received`; отсоединённой копии больше нет |
| `mapperReadDtoToEntity(...)`, `AbsMapDtoToPresetEntity` для read DTO, `mapSpecificFieldsReadDtoToEntity` | удалены; тела PATCH — `patches()`, связь из read DTO — `mapper.reference(dto, Entity.class)` |
| `mapperEntityToReadDto(...)` + `createReadDtoFromEntity(mapper, entity)` | `toReadDto(entity)`; маппер доступен полем `mapper` |
| `mapperEntityToEntity(...)` (self-map, движок `mergeActualAndReceived` по умолчанию) | удалён без замены |
| `AbsMapBasic`, `AbsMapBaseDtoToEntity`, `AbsMapCreateDtoToEntity`, `mapper.core.AbsMapDtoToEntity` + `mapSpecificFields` / `handleAfterMapSpecificFields` | `AbsMapDtoToEntity` (`flex.mapper`): `create(dto)`; `map()` = `create()` + `nullifyZeroId()` |
| `AbsMapEntityToDto` | то же имя и тот же `create(entity)`; внутренности ModelMapper удалены |
| `AbsMapUpdateDtoToEntity` + `mapSpecificFields` | то же имя, абстрактный `update(dto, entity)` |
| `customizeTypeMap(TypeMap)`, `RegisterableMapper` | бин `Mapper.of(From.class, To.class, fn)` или своя реализация `Mapper` / `Updater` |
| `mapper.mapper.AbsMapperExtRelation`, `setRelation` по умолчанию ищет поле рефлексией | `mapper.AbsMapperExtRelation`, `setRelation(target, relation)` абстрактный |
| `updatePartial(id, partial)`, `IGNORE_PARTIAL_UPDATE_PROPERTIES`, `FieldCopyUtil` | `patch(id, body)` + `Updater` на каждый partial-класс в `patches()` конфига; изменения в коде — `changeEntity(id, change)` |
| переопределённый `update(dto)` с `mapEntity(dto)` ради `saveAndFlush` или своей загрузки | швы `saveUpdated(entity)` / `loadForUpdate(id)` |
| `mapEntity(Object)`, `mapAllEntities(Collection<?>)` в `AbsFlexServiceRUD` | `mapEntity(CREATE_DTO)`, `mapAllEntities(Collection<CREATE_DTO>)` в `AbsFlexServiceCRUD` |
| `beforeUpdateHook(AbstractDto<ID>)`, звался и из `updatePartial` | `beforeUpdateHook(UPDATE_DTO)` — только из `update`; у `patch` свой `beforePatchHook(id, body)` |
| `AbsUpdateChangesHookable.beforeUpdateHook(previous, AbstractDto<ID> current)` | `current` — `Object`: update DTO или тело патча |
| `AbsTypeMapChecker`, `AbsCrudCustomizer` (`typeMapCheckerEnabled`, `eagerTypeMapRegistration`), `AbsMapperEagerInitPostProcessor` | удалены, см. «Стартового чекера нет» |
| бин `absGenericCrudModelMapper`, свой `@Primary ModelMapper` | не нужны; зависимость `org.modelmapper:modelmapper` из библиотеки убрана — если приложение само пользуется ModelMapper, объявите её у себя |

### Preset-маппер → `updateEntity`

```java
// 13.3.15: маппер сам находит строку и переносит поля с отсоединённой копии
@Override
protected AbsMapBasic<MyUpdateDto, MyEntity> mapperUpdateDtoToEntity(
        AbsModelMapper mapper, Class<MyUpdateDto> updateDtoClass, Class<MyEntity> entityClass) {
    return new AbsMapUpdateDtoToPresetEntity<>(mapper, updateDtoClass, entityClass) {
        @Override
        protected MyEntity mergeActualAndReceived(MyEntity actual, MyEntity received) {
            actual.setName(received.getName());
            actual.setDescription(received.getDescription());
            return actual;
        }
    };
}

// 15.0: строку загружает сервис (loadForUpdate), конфиг пишет только поля запроса
@Override
protected void updateEntity(MyUpdateDto dto, MyEntity entity) {
    entity.setName(dto.getName());
    entity.setDescription(dto.getDescription());
}
```

### PATCH

```java
// 13.3.15
service.updatePartial(id, new PartialName(name));

// 15.0: тот же вызов под новым именем, partial-класс остаётся…
service.patch(id, new PartialName(name));

// …плюс Updater на каждый partial-класс в конфиге сущности
@Override
protected void patches(Patches<MyEntity> p) {
    p.add(PartialName.class, (body, entity) -> entity.setName(body.getName()));
}

// Изменение, которое пишет код, а не тело запроса
public MyReadDto archive(Long id) {
    return changeEntity(id, entity -> entity.setArchivedAt(Instant.now()));
}
```

Тело без зарегистрированного `Updater` — `MappingNotFoundException`, ничего не сохраняется.

### Швы записи

```java
// 13.3.15: update переопределён целиком ради saveAndFlush
@Override
public MyReadDto update(MyUpdateDto dto) {
    return mapReadDto(repository.saveAndFlush(mapEntity(dto)));
}

// 15.0: только шов, и он работает для update, patch и changeEntity
@Override
protected MyEntity saveUpdated(MyEntity entity) {
    return repository.saveAndFlush(entity); // значения, посчитанные базой, попадают в ответ
}
```

### Правила

- **Null.** Библиотека null не трактует. `toEntity`, `updateEntity` и `patches()` пишут ровно
  то, что написано: `e.setName(dto.getName())` очищает поле, `if (dto.getName() != null)
  e.setName(dto.getName())` сохраняет. До 15.0 null не очищал поле ни на одном пути записи
  (`skipNull` у `update`, пропуск при маппинге в сущность у `updatePartial`), поэтому при
  переносе каждого update DTO и partial-класса решение «не трогать» или «очистить»
  принимается на каждое поле. Фасад особо обрабатывает только null-источник:
  `map(null, X.class)` → `null`, `update(null, target)` → `target` без изменений.
- **Точный ключ.** Реестр ищет пару только по точным классам: ни суперклассы, ни интерфейсы
  не рассматриваются. Для иерархии DTO нужна пара на каждый конкретный класс: в LocatorServer —
  `StepAddress` / `StepWarehouse` / `StepTransit` (пара на абстрактный `Step` не нужна:
  экземпляра с таким runtime-классом не бывает); в цепочках
  `Carrier extends CarrierUpdate extends CarrierCreate` (так же Skill и Warehouse) каждый класс
  получает свою пару — маппер `CarrierCreate` не подхватит `CarrierUpdate`. Единственное
  исключение — JPA-прокси: класс без `@Entity` с `@Entity`-предком приводится к ближайшему
  такому предку. Ленивая ссылка на полиморфную сущность — прокси объявленного базового класса;
  чтобы маппить её по конкретному наследнику, сначала `Hibernate.unproxy(...)`.
- **Связь из read DTO — через `reference`.** `map(driverDto, DriverEntity.class)` ради связи
  заменяется на `mapper.reference(driverDto, DriverEntity.class)`: `getReference` по id без
  копирования полей. Пары read DTO → entity в 15.0 нет.
- **Не маппить в конструкторах и `@PostConstruct`.** Первый `map()` собирает реестр, а сборка
  создаёт все бины-мапперы. Если это случится при создании бина, от которого зависит хотя бы
  один маппер, Spring бросит `BeanCurrentlyInCreationException`. Прогревы кэшей — в
  `ApplicationReadyEvent` или `SmartLifecycle`.
- **Проверки на все пути записи — в `loadForUpdate`.** `beforeUpdateHook(UPDATE_DTO)`
  вызывается только из `update`; `patch` зовёт `beforePatchHook(id, body)`, `changeEntity` —
  ни тот, ни другой. В 13.3.15 `updatePartial` проходил через `beforeUpdateHook`. Запрет,
  который должен закрывать все пути (например, правка глобального навыка в
  `LogisticSkillService`), переносится в переопределённый `loadForUpdate`.
- **Стартового чекера нет.** `AbsTypeMapChecker`, `AbsCrudCustomizer` и
  `AbsMapperEagerInitPostProcessor` удалены: пара без маппера всплывает
  `MappingNotFoundException` при первом вызове с направлением и обоими классами, дубль пары
  ломает старт. Тесты, которые выключали чекер бином `AbsCrudCustomizer`, просто теряют эту
  конфигурацию; мокать flex-сервисы можно без оговорок. Нужна проверка состава пар на старте —
  свой `SmartLifecycle` поверх публичных `MapperRegistry.findMapper` / `findUpdater`.
- **AOP на `updatePartial`.** Pointcut'ы на `updatePartial` после переезда молча перестанут
  срабатывать (в LocatorServer — `TrackerCertificateExpireCacheAspect`): перенацельте их на
  `patch`.

````

- [ ] **Step 4: CHANGELOG**

In `CHANGELOG.md`, replace

```
## Не выпущено

## 14.0
```

with

```
## Не выпущено

### Маппинг: только явный, ModelMapper удалён
- Зависимость `org.modelmapper:modelmapper` удалена. Каждое преобразование DTO ↔ entity —
  явный код потребителя, который `MapperRegistry` находит по точной паре классов. Ничего не
  копируется по совпадению имён полей.
- Новые типы в `by.nhorushko.crudgeneric.flex.mapper`: `Mapper` / `Updater` (с фабриками
  `Mapper.of` / `Updater.of`), `MapperSource`, `MapperRegistry`, `Patches` и `AbsFlexMapConfig`
  с тремя абстрактными методами `toEntity` / `updateEntity` / `toReadDto` и необязательным
  `patches()`. Одиночные базы `AbsMapEntityToDto`, `AbsMapDtoToEntity`,
  `AbsMapUpdateDtoToEntity` — по одному абстрактному методу.
- Фасад `AbsModelMapper` переименован в `AbsMapper`: `map`, `mapAll`, `reference`,
  `referenceById` не меняются, in-place форма называется `update(source, target)`,
  `getModelMapper()` удалён.
- Пары ищутся только по точному ключу, без подъёма по суперклассам и интерфейсам; JPA-прокси
  приводятся к ближайшему `@Entity`-предку. Промах — `MappingNotFoundException` при вызове,
  дубль пары — ошибка старта.
- `AbsMapperExtRelation` переехал в `flex.mapper`; `setRelation` стал абстрактным, рефлексивный
  поиск поля удалён.
- Удалены `AbsFlexMapConfigDefault`, `AbsFlexMapConfigAbstract`, `AbsMapBasic`,
  `AbsMapBaseDtoToEntity`, `AbsMapCreateDtoToEntity`, `mapper.core.AbsMapDtoToEntity`,
  `RegisterableMapper`, стартовый чекер `AbsTypeMapChecker`, `AbsCrudCustomizer`,
  `AbsMapperEagerInitPostProcessor` и `FieldCopyUtil`.

### Сервисы
- `updatePartial(id, partial)` заменён на `patch(id, body)`: форма вызова та же, но каждое
  тело получает явный `Updater`, объявленный в `patches()` конфига. Для изменений в коде —
  `changeEntity(id, change)`.
- Все пути записи идут через швы `loadForUpdate(id)` и `saveUpdated(entity)`: переопределять
  `update` ради `saveAndFlush` или fetch join больше не нужно.
- `beforeUpdateHook` типизирован `UPDATE_DTO` и вызывается только из `update`; у `patch` свой
  `beforePatchHook(id, body)`. `AbsUpdateChangesHookable.beforeUpdateHook` принимает `current`
  как `Object`.
- `mapEntity` / `mapAllEntities` переехали из `AbsFlexServiceRUD` в `AbsFlexServiceCRUD` и
  типизированы `CREATE_DTO`.
- Библиотека больше не задаёт семантику null: `null` в DTO очищает поле, только если
  `updateEntity` или `patches()` так написаны. Раньше null не очищал поле ни на одном пути.

### Миграция
- Потребители на `13.3.15-jakarta` переходят на 15.0 одним шагом, минуя 14.0. Таблица «было →
  стало» по API 13.3.15 — в README, раздел «Миграция на 15.0 (явные мапперы)».

## 14.0
```

- [ ] **Step 5: CLAUDE.md**

In `CLAUDE.md`:

1. Replace

   ```
   - `test-application/` — демо-приложение, **не публикуется**; несёт 31 интеграционный
     тест (`*IT`), которые гоняет failsafe
   ```

   with

   ```
   - `test-application/` — демо-приложение, **не публикуется**; несёт 40 интеграционных
     тестов (`*IT`), которые гоняет failsafe
   ```

2. Under `## Чего не делать`, after the line `- Не публиковать: ...`, add:

   ```
   - Не возвращать неявный маппинг: маппинг только явный, через `MapperRegistry`; никакого
     ModelMapper и рефлексии в маппинге.
   ```

- [ ] **Step 6: Check and commit**

```bash
cd /d/projects/crud-generic
grep -n "AbsModelMapper\|AbsFlexMapConfigDefault" README.md
```

Expected: hits only inside the new migration section (the «было» column and the Preset example) and in the historical «Migration to 5.0» table. Nothing in «Usage Guide» or the pageable example.

Run: `./mvnw -B verify`
Expected: `BUILD SUCCESS` with the Task 5 numbers (106 / 3 / 40).

```bash
git add README.md CHANGELOG.md CLAUDE.md
git commit -F - <<'EOF'
docs: README, CHANGELOG and CLAUDE.md for explicit mappers

README shows the explicit AbsFlexMapConfig and gains a migration section
for consumers on 13.3.15: an old-to-new table by the 13.3.15 API
(Preset mappers and their hooks, config factories, updatePartial,
getModelMapper, AbsCrudCustomizer), PATCH and write-seam examples, and
the rules on null, exact keys, relations, constructors and startup
checks.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
git log --oneline -5
```

Expected: the four new commits on top of `docs(spec): facade update(source, target), ...`.
