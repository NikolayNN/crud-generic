# save() create-path fix under Hibernate 6.6 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix mass `OptimisticLockException` on entity creation through `save(...)` after the Spring Boot 3.5 / Hibernate ORM 6.6 upgrade, across v1, v2, and flex service families.

**Architecture:** Two orthogonal fixes. **Fix A** moves sentinel `0 → null` normalization into the ModelMapper post-converters (covers root *and* cascade children). **Fix B** replaces the `repository.save()` (merge-guess) create-path with an INSERT-aware `persistOrMerge(entity)` helper (`existsById` → `persist` / else `merge`), restoring the insert-if-absent that Hibernate 6.5's `merge` did implicitly.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring Data JPA 3.5, Hibernate ORM 6.6, jakarta.persistence, ModelMapper, JUnit 4 + Mockito (library unit tests), JUnit 5 + `@SpringBootTest` + H2 (test-application integration tests). Build with JDK 17.

**Spec:** `docs/superpowers/specs/2026-06-03-save-create-path-hibernate-66-design.md`

---

## File Structure

**Library (production) changes:**
- `library/.../mapper/AbstractMapper.java` (v1) — Fix A in `toEntityConverter()`.
- `library/.../v2/mapper/AbsMapperEntityDto.java` (v2/flex) — Fix A in `createConverterDtoToEntity()`.
- `library/.../service/ImmutableGenericService.java` (v1 base) — add `EntityManager` + `persistOrMerge`; drop `setId(null)` from `checkIdForSave(ENTITY)`.
- `library/.../service/CrudGenericService.java` (v1) — `save`/`saveAll` use `persistOrMerge`, drop `checkIdForSave`.
- `library/.../service/CrudAdditionalGenericService.java` (v1) — `save`/`saveAll` use `persistOrMerge`, drop `checkIdForSave`.
- `library/.../v2/service/AbsServiceR.java` (v2 base) — add `EntityManager` + `persistOrMerge`.
- `library/.../v2/service/AbsServiceCRUD.java` (v2) — `save`/`saveAll` use `persistOrMerge`, drop redundant `nullifyZeroId`.
- `library/.../flex/service/AbsFlexServiceCRUD.java` (flex) — add `EntityManager` + `persistOrMerge`; `save`/`saveAll` use it.

**Library test changes:**
- `library/.../v2/service/AbsServiceCRUDTest.java` — rewrite for persist/merge routing.

**test-application integration tests + fixtures (new):**
- v1 cascade fixtures: `GroupEntity`, `ItemEntity`, `GroupDto`, `ItemDto`, `GroupMapper`, `ItemMapper`, `GroupRepository`, `ItemRepository`, `GroupService`.
- v2 assigned-id fixtures: `RegionEntity`, `RegionDto`, `RegionMapper`, `RegionRepository`, `RegionServiceCRUD`.
- IT classes: `SaveV1IT` (sentinel + upsert-existing + cascade), `SaveV2IT` (sentinel + upsert-existing + assigned-id), mapper unit tests `MapperNullifyZeroIdTest`.

**Out of scope (documented):** `AbsServiceExtCRUD` (create-only, sentinel already works, covered by Fix A through its mapper); full flex Spring fixture (flex verified by code change + shared `AbsMapperEntityDto` Fix A; no flex harness exists).

---

## Phase 1 — Fix A: sentinel `0 → null` in mappers

### Task 1: v1 `AbstractMapper.toEntityConverter()` nullifies zero id

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/mapper/AbstractMapper.java:85-92`

- [ ] **Step 1: Modify the converter**

Replace the existing `toEntityConverter()` method body:

```java
    protected Converter<DTO, ENTITY> toEntityConverter() {
        return context -> {
            DTO source = context.getSource();
            ENTITY destination = context.getDestination();
            mapSpecificFields(source, destination);
            if (destination.getId() != null && destination.getId() == 0L) {
                destination.setId(null);
            }
            return context.getDestination();
        };
    }
```

(`ENTITY extends AbstractEntity`, which declares `Long getId()` / `void setId(Long)`. `== 0L` unboxes the non-null `Long`.)

- [ ] **Step 2: Compile**

Run: `mvn -q -pl library -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/mapper/AbstractMapper.java
git commit -m "fix(v1): normalize sentinel id 0 to null in AbstractMapper post-converter"
```

### Task 2: v2 `AbsMapperEntityDto.createConverterDtoToEntity()` nullifies zero id

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/v2/mapper/AbsMapperEntityDto.java:46-57`

- [ ] **Step 1: Modify the converter**

Replace the `createConverterDtoToEntity()` method body, adding `destination.nullifyZeroId()` before `return`:

```java
    protected Converter<DTO, ENTITY> createConverterDtoToEntity() {
        return context -> {
            DTO source = context.getSource();
            ENTITY destination = context.getDestination();
            mapSpecificFields(source, destination);
            if (source instanceof AbstractDto && !((AbstractDto<?>)source).isNew()) {
                ENTITY beforeEntity = entityManager.getReference(entityClass, ((AbstractDto<?>)source).getId());
                mapSpecificFields(source, beforeEntity, destination);
            }
            destination.nullifyZeroId();
            return destination;
        };
    }
```

(`ENTITY extends AbstractEntity<?>`, which provides the `nullifyZeroId()` default that nulls a numeric-zero id.)

- [ ] **Step 2: Compile**

Run: `mvn -q -pl library -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/v2/mapper/AbsMapperEntityDto.java
git commit -m "fix(v2): normalize sentinel id 0 to null in AbsMapperEntityDto post-converter"
```

---

## Phase 2 — Fix B: INSERT-aware save() in v1

### Task 3: Add `EntityManager` + `persistOrMerge` to v1 base, drop `setId(null)`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/service/ImmutableGenericService.java`

- [ ] **Step 1: Add imports**

After the existing imports (top of file), add:

```java
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
```

- [ ] **Step 2: Add the EntityManager field**

Immediately after the existing fields block (`protected final Class<ENTITY> entityClass;`, line ~28), add:

```java
    @PersistenceContext
    protected EntityManager entityManager;
```

- [ ] **Step 3: Add the `persistOrMerge` helper**

Add this method to the class (e.g. just below the constructors):

```java
    /**
     * Insert-if-absent / merge-if-present. Restores the behaviour Hibernate 6.5 merge gave
     * implicitly for detached entities whose row does not exist (broken on Hibernate 6.6).
     * Sentinel id 0 is already nulled by the mapper, so a null id always routes to persist.
     */
    protected ENTITY persistOrMerge(ENTITY entity) {
        Long id = entity.getId();
        if (id == null || !repository.existsById(id)) {
            entityManager.persist(entity);
            return entity;
        }
        return repository.save(entity);
    }
```

- [ ] **Step 4: Drop `setId(null)` from `checkIdForSave(ENTITY)`**

Change (lines ~118-121):

```java
    protected void checkIdForSave(ENTITY e) {
        checkIdForSave(e.getId());
        e.setId(null);
    }
```

to:

```java
    protected void checkIdForSave(ENTITY e) {
        checkIdForSave(e.getId());
    }
```

- [ ] **Step 5: Compile**

Run: `mvn -q -pl library -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/service/ImmutableGenericService.java
git commit -m "feat(v1): add persistOrMerge helper and PersistenceContext to ImmutableGenericService"
```

### Task 4: `CrudGenericService.save/saveAll` use `persistOrMerge`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/service/CrudGenericService.java:34-57`

- [ ] **Step 1: Replace `save` and `saveAll`**

```java
    public DTO save(DTO dto) {
        check(dto);
        ENTITY entity = mapper.toEntity(dto);
        setupEntityBeforeSave(entity);
        ENTITY savedEntity = persistOrMerge(entity);
        processEntityAfterSave(savedEntity);
        return mapper.toDto(savedEntity);
    }

    public List<DTO> saveAll(List<DTO> list) {
        List<ENTITY> entities = list.stream()
                .peek(this::check)
                .map(dto -> {
                    ENTITY e = mapper.toEntity(dto);
                    setupEntityBeforeSave(e);
                    return e;
                }).collect(Collectors.toList());
        return entities.stream()
                .map(this::persistOrMerge)
                .peek(this::processEntityAfterSave)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
```

(Removed the `checkIdForSave(entity)` / `checkIdForSave(e)` calls — v1 `save()` is now an upsert; the `update()`/`updatePartial()` guard `checkIdForUpdate` in `RudGenericService` is untouched.)

- [ ] **Step 2: Compile**

Run: `mvn -q -pl library -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/service/CrudGenericService.java
git commit -m "fix(v1): route CrudGenericService save/saveAll through persistOrMerge"
```

### Task 5: `CrudAdditionalGenericService.save/saveAll` use `persistOrMerge`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/service/CrudAdditionalGenericService.java:26-53`

- [ ] **Step 1: Replace `save` and `saveAll`**

```java
    public DTO save(Long rootId, DTO dto) {
        check(dto);
        ENTITY entity = mapper.toEntity(dto);
        setupEntityBeforeSave(rootId, entity);
        entity = persistOrMerge(entity);
        setupEntityAfterSave(entity);
        return mapper.toDto(entity);
    }

    public List<DTO> saveAll(Long rootId, Collection<DTO> list) {
        List<ENTITY> entities = list.stream()
                .peek(this::check)
                .map(dto -> {
                    ENTITY e = mapper.toEntity(dto);
                    setupEntityBeforeSave(rootId, e);
                    return e;
                })
                .collect(Collectors.toList());
        return entities.stream()
                .map(this::persistOrMerge)
                .peek(this::setupEntityAfterSave)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
```

- [ ] **Step 2: Compile**

Run: `mvn -q -pl library -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/service/CrudAdditionalGenericService.java
git commit -m "fix(v1): route CrudAdditionalGenericService save/saveAll through persistOrMerge"
```

---

## Phase 3 — Fix B: INSERT-aware save() in v2

### Task 6: Add `EntityManager` + `persistOrMerge` to `AbsServiceR`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/v2/service/AbsServiceR.java`

- [ ] **Step 1: Add imports**

Add to the import block:

```java
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
```

- [ ] **Step 2: Add field + helper**

After the existing fields (`protected final REPOSITORY repository;`, line ~28), add:

```java
    @PersistenceContext
    protected EntityManager entityManager;
```

And add the method (anywhere in the class body):

```java
    /**
     * Insert-if-absent / merge-if-present. Restores Hibernate 6.5 merge-of-absent-row semantics
     * (broken on 6.6). Sentinel id 0 is already nulled by the mapper, so a null id routes to persist.
     */
    protected ENTITY persistOrMerge(ENTITY entity) {
        ID id = entity.getId();
        if (id == null || !repository.existsById(id)) {
            entityManager.persist(entity);
            return entity;
        }
        return repository.save(entity);
    }
```

(`entity.getId()` returns `ID`; `repository.existsById(ID)` matches `JpaRepository<ENTITY, ID>`.)

- [ ] **Step 3: Compile**

Run: `mvn -q -pl library -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/v2/service/AbsServiceR.java
git commit -m "feat(v2): add persistOrMerge helper and PersistenceContext to AbsServiceR"
```

### Task 7: `AbsServiceCRUD.save/saveAll` use `persistOrMerge`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/v2/service/AbsServiceCRUD.java:24-40`

- [ ] **Step 1: Replace `save` and `saveAll`**

```java
    public DTO save(DTO dto) {
        ENTITY entity = persistOrMerge(mapper.toEntity(dto));
        DTO saved = mapper.toDto(entity);
        afterSaveHook(saved);
        return saved;
    }

    public List<DTO> saveAll(Collection<DTO> dto) {
        List<ENTITY> entities = mapper.toEntities(dto).stream()
                .map(this::persistOrMerge)
                .collect(java.util.stream.Collectors.toList());
        List<DTO> saved = mapper.toDtos(entities);
        saved.forEach(this::afterSaveHook);
        return saved;
    }
```

(Removed the explicit `entity.nullifyZeroId()` / `entities.forEach(AbstractEntity::nullifyZeroId)` — the mapper now owns normalization, Task 2. The unused `AbstractEntity` import may be removed if the IDE flags it; leave otherwise.)

- [ ] **Step 2: Compile**

Run: `mvn -q -pl library -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/v2/service/AbsServiceCRUD.java
git commit -m "fix(v2): route AbsServiceCRUD save/saveAll through persistOrMerge"
```

### Task 8: Rewrite `AbsServiceCRUDTest` for persist/merge routing

**Files:**
- Modify (full replace): `library/src/test/java/by/nhorushko/crudgeneric/v2/service/AbsServiceCRUDTest.java`

The test lives in package `by.nhorushko.crudgeneric.v2.service`, so the protected `entityManager` field on `AbsServiceR` is directly assignable. New id `null` → `persist` (returns same entity); id `5` with `existsById(5)==true` → `repository.save`.

- [ ] **Step 1: Write the rewritten test**

```java
package by.nhorushko.crudgeneric.v2.service;

import by.nhorushko.crudgeneric.domain.dto.Message;
import by.nhorushko.crudgeneric.domain.dto.Message.GpsCoordinate;
import by.nhorushko.crudgeneric.domain.entity.MessageEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class AbsServiceCRUDTest {

    @Mock
    private AbsMapperEntityDto<MessageEntity, Message> mockedMapper;

    @Mock
    private JpaRepository<MessageEntity, Long> mockedRepository;

    @Mock
    private EntityManager mockedEntityManager;

    private AbsServiceCRUD<Long, MessageEntity, Message, JpaRepository<MessageEntity, Long>> service;

    @Before
    public void initializeService() {
        this.service = new AbsServiceCRUD<>(this.mockedMapper, this.mockedRepository) {
        };
        this.service.entityManager = this.mockedEntityManager;
    }

    @Test
    public void newMessageShouldBePersisted() {
        final Message givenDto = new Message(null, new GpsCoordinate(5.5F, 6.6F), 10, 15, 20);
        final MessageEntity givenEntity = new MessageEntity(null, 5.5F, 6.6F, 10, 15, 20);
        when(this.mockedMapper.toEntity(any(Message.class))).thenReturn(givenEntity);

        final Message savedDto = new Message(null, new GpsCoordinate(5.5F, 6.6F), 10, 15, 20);
        when(this.mockedMapper.toDto(givenEntity)).thenReturn(savedDto);

        final Message actual = this.service.save(givenDto);

        assertSame(savedDto, actual);
        verify(this.mockedEntityManager, times(1)).persist(givenEntity);
        verify(this.mockedRepository, never()).save(any(MessageEntity.class));
    }

    @Test
    public void existingMessageShouldBeMerged() {
        final Message givenDto = new Message(5L, new GpsCoordinate(7.7F, 8.8F), 11, 16, 21);
        final MessageEntity givenEntity = new MessageEntity(5L, 7.7F, 8.8F, 11, 16, 21);
        when(this.mockedMapper.toEntity(any(Message.class))).thenReturn(givenEntity);
        when(this.mockedRepository.existsById(5L)).thenReturn(true);

        final MessageEntity mergedEntity = new MessageEntity(5L, 7.7F, 8.8F, 11, 16, 21);
        when(this.mockedRepository.save(givenEntity)).thenReturn(mergedEntity);

        final Message savedDto = new Message(5L, new GpsCoordinate(7.7F, 8.8F), 11, 16, 21);
        when(this.mockedMapper.toDto(mergedEntity)).thenReturn(savedDto);

        final Message actual = this.service.save(givenDto);

        assertSame(savedDto, actual);
        verify(this.mockedRepository, times(1)).save(givenEntity);
        verify(this.mockedEntityManager, never()).persist(any());
    }

    @Test
    public void saveAllShouldRouteEachElement() {
        final List<Message> givenDtos = List.of(
                new Message(null, new GpsCoordinate(5.5F, 6.6F), 10, 15, 20),
                new Message(5L, new GpsCoordinate(7.7F, 8.8F), 11, 16, 21)
        );
        final MessageEntity newEntity = new MessageEntity(null, 5.5F, 6.6F, 10, 15, 20);
        final MessageEntity existingEntity = new MessageEntity(5L, 7.7F, 8.8F, 11, 16, 21);
        when(this.mockedMapper.toEntities(anyCollectionOf(Message.class)))
                .thenReturn(List.of(newEntity, existingEntity));
        when(this.mockedRepository.existsById(5L)).thenReturn(true);
        when(this.mockedRepository.save(existingEntity)).thenReturn(existingEntity);

        final List<Message> savedDtos = List.of(
                new Message(255L, new GpsCoordinate(5.5F, 6.6F), 10, 15, 20),
                new Message(5L, new GpsCoordinate(7.7F, 8.8F), 11, 16, 21)
        );
        when(this.mockedMapper.toDtos(anyCollectionOf(MessageEntity.class))).thenReturn(savedDtos);

        final List<Message> actual = this.service.saveAll(givenDtos);

        assertEquals(savedDtos, actual);
        verify(this.mockedEntityManager, times(1)).persist(newEntity);
        verify(this.mockedRepository, times(1)).save(existingEntity);
    }
}
```

- [ ] **Step 2: Run the test**

Run: `mvn -q -pl library test -Dtest=AbsServiceCRUDTest`
Expected: PASS (3 tests).

- [ ] **Step 3: Commit**

```bash
git add library/src/test/java/by/nhorushko/crudgeneric/v2/service/AbsServiceCRUDTest.java
git commit -m "test(v2): rewrite AbsServiceCRUDTest for persist/merge routing"
```

---

## Phase 4 — Fix B: INSERT-aware save() in flex

### Task 9: `AbsFlexServiceCRUD` gets `persistOrMerge`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceCRUD.java`

- [ ] **Step 1: Add imports**

Add to the import block:

```java
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.stream.Collectors;
```

- [ ] **Step 2: Add field + helper**

After the `createDtoClass` field (line ~47), add:

```java
    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * Insert-if-absent / merge-if-present. Restores Hibernate 6.5 merge-of-absent-row semantics
     * (broken on 6.6). Sentinel id 0 is already nulled by the mapper (AbsMapperEntityDto).
     */
    protected ENTITY persistOrMerge(ENTITY entity) {
        ENTITY_ID id = entity.getId();
        if (id == null || !repository.existsById(id)) {
            entityManager.persist(entity);
            return entity;
        }
        return repository.save(entity);
    }
```

(`repository` and the `ENTITY`/`ENTITY_ID` generics are inherited from `AbsFlexServiceR`; `mapEntity`/`mapAllEntities`/`mapReadDto`/`mapAllReadDto` from `AbsFlexServiceRUD`.)

- [ ] **Step 3: Replace `save` and `saveAll`**

```java
    public READ_DTO save(CREATE_DTO dto) {
        beforeSaveHook(dto);
        ENTITY entity = persistOrMerge(mapEntity(dto));
        READ_DTO actual = mapReadDto(entity);
        afterSaveHook(actual);
        return actual;
    }

    public List<READ_DTO> saveAll(Collection<CREATE_DTO> dtos) {
        beforeSaveAllHook(dtos);
        List<ENTITY> entities = mapAllEntities(dtos).stream()
                .map(this::persistOrMerge)
                .collect(Collectors.toList());
        List<READ_DTO> actual = mapAllReadDto(entities);
        afterSaveAllHook(actual);
        return actual;
    }
```

- [ ] **Step 4: Compile**

Run: `mvn -q -pl library -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/flex/service/AbsFlexServiceCRUD.java
git commit -m "fix(flex): route AbsFlexServiceCRUD save/saveAll through persistOrMerge"
```

---

## Phase 5 — Integration tests (test-application, real Hibernate 6.6)

> All IT classes use `@SpringBootTest` (no `@Transactional` on the test class, so the service's own
> `@Transactional` commits and flushes — the only way `OptimisticLockException` surfaces). Each test
> uses fresh ids and cleans its repositories in `@AfterEach`. New `@Entity` classes auto-create H2
> tables (Boot embedded default `ddl-auto=create-drop`).

### Task 10: Mapper nullify unit tests (fast, no DB)

**Files:**
- Create: `test-application/src/test/java/by/nhorushko/crudgenerictest/mapper/MapperNullifyZeroIdTest.java`

Verifies Fix A end-to-end through the real registered mappers (v1 `MockAMapper`, v2 `TrackerAbsMapperEntityDto`).

- [ ] **Step 1: Write the test**

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgenerictest.domain.dto.MockADto;
import by.nhorushko.crudgenerictest.domain.dto.Tracker;
import by.nhorushko.crudgenerictest.domain.entity.MockAEntity;
import by.nhorushko.crudgenerictest.domain.entity.TrackerEntity;
import by.nhorushko.crudgenerictest.mockmapper.MockAMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MapperNullifyZeroIdTest {

    @Autowired
    private MockAMapper v1Mapper;

    @Autowired
    private TrackerAbsMapperEntityDto v2Mapper;

    @Test
    void v1MapperNullsZeroId() {
        MockAEntity entity = v1Mapper.toEntity(new MockADto(0L, "name"));
        assertThat(entity.getId()).isNull();
    }

    @Test
    void v1MapperKeepsRealId() {
        MockAEntity entity = v1Mapper.toEntity(new MockADto(7L, "name"));
        assertThat(entity.getId()).isEqualTo(7L);
    }

    @Test
    void v2MapperNullsZeroId() {
        TrackerEntity entity = v2Mapper.toEntity(new Tracker(0L, "imei", "phone"));
        assertThat(entity.getId()).isNull();
    }

    @Test
    void v2MapperKeepsRealId() {
        TrackerEntity entity = v2Mapper.toEntity(new Tracker(7L, "imei", "phone"));
        assertThat(entity.getId()).isEqualTo(7L);
    }
}
```

- [ ] **Step 2: Run**

Run: `mvn -q -pl test-application test -Dtest=MapperNullifyZeroIdTest`
Expected: PASS (4 tests). (If `assertj` is unavailable, switch to `org.junit.jupiter.api.Assertions.assertNull/assertEquals`.)

- [ ] **Step 3: Commit**

```bash
git add test-application/src/test/java/by/nhorushko/crudgenerictest/mapper/MapperNullifyZeroIdTest.java
git commit -m "test: verify mapper nullifies sentinel zero id (v1 + v2)"
```

### Task 11: v1 integration test — sentinel + upsert-existing (reuse Mock fixtures)

**Files:**
- Create: `test-application/src/test/java/by/nhorushko/crudgenerictest/service/SaveV1IT.java`

- [ ] **Step 1: Write the test**

```java
package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgenerictest.MockRepository;
import by.nhorushko.crudgenerictest.MockService;
import by.nhorushko.crudgenerictest.domain.dto.MockADto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SaveV1IT {

    @Autowired
    private MockService service;
    @Autowired
    private MockRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void sentinelZeroIdInserts() {
        MockADto saved = service.save(new MockADto(0L, "sentinel"));
        assertThat(saved.getId()).isNotNull();
        assertThat(repository.existsById(saved.getId())).isTrue();
    }

    @Test
    void saveWithExistingIdUpdatesNoDuplicate() {
        MockADto created = service.save(new MockADto(0L, "first"));
        Long id = created.getId();

        MockADto updated = service.save(new MockADto(id, "second"));

        assertThat(updated.getId()).isEqualTo(id);
        assertThat(repository.count()).isEqualTo(1L);
        assertThat(repository.findById(id).orElseThrow().getName()).isEqualTo("second");
    }
}
```

- [ ] **Step 2: Run**

Run: `mvn -q -pl test-application test -Dtest=SaveV1IT`
Expected: PASS (2 tests), no `OptimisticLockException`.

- [ ] **Step 3: Commit**

```bash
git add test-application/src/test/java/by/nhorushko/crudgenerictest/service/SaveV1IT.java
git commit -m "test(v1): integration test for sentinel insert and upsert-existing"
```

### Task 12: v1 cascade fixtures (parent + child, registered mappers)

**Files (create all):**
- `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/entity/GroupEntity.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/entity/ItemEntity.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/GroupDto.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/ItemDto.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/mockmapper/ItemMapper.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/mockmapper/GroupMapper.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/repository/GroupRepository.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/repository/ItemRepository.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/service/GroupService.java`

- [ ] **Step 1: `ItemEntity`**

```java
package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grp_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemEntity implements AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupEntity group;
}
```

- [ ] **Step 2: `GroupEntity`**

```java
package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupEntity implements AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemEntity> items = new ArrayList<>();
}
```

- [ ] **Step 3: `ItemDto`**

```java
package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto implements AbstractDto {
    private Long id;
    private String title;
}
```

- [ ] **Step 4: `GroupDto`**

```java
package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupDto implements AbstractDto {
    private Long id;
    private String name;
    private List<ItemDto> items = new ArrayList<>();
}
```

- [ ] **Step 5: `ItemMapper` (registers ItemDto↔ItemEntity TypeMap → Fix A fires for children)**

```java
package by.nhorushko.crudgenerictest.mockmapper;

import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgenerictest.domain.dto.ItemDto;
import by.nhorushko.crudgenerictest.domain.entity.ItemEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class ItemMapper extends AbstractMapper<ItemEntity, ItemDto> {
    public ItemMapper(ModelMapper modelMapper) {
        super(ItemEntity.class, ItemDto.class, modelMapper);
    }
}
```

- [ ] **Step 6: `GroupMapper` (wires child → parent back-reference)**

```java
package by.nhorushko.crudgenerictest.mockmapper;

import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgenerictest.domain.dto.GroupDto;
import by.nhorushko.crudgenerictest.domain.entity.GroupEntity;
import by.nhorushko.crudgenerictest.domain.entity.ItemEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class GroupMapper extends AbstractMapper<GroupEntity, GroupDto> {

    public GroupMapper(ModelMapper modelMapper) {
        super(GroupEntity.class, GroupDto.class, modelMapper);
    }

    @Override
    protected void mapSpecificFields(GroupDto source, GroupEntity destination) {
        if (destination.getItems() != null) {
            for (ItemEntity item : destination.getItems()) {
                item.setGroup(destination);
            }
        }
    }
}
```

- [ ] **Step 7: Repositories**

`GroupRepository.java`:

```java
package by.nhorushko.crudgenerictest.repository;

import by.nhorushko.crudgenerictest.domain.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GroupRepository extends JpaRepository<GroupEntity, Long>, JpaSpecificationExecutor<GroupEntity> {
}
```

`ItemRepository.java`:

```java
package by.nhorushko.crudgenerictest.repository;

import by.nhorushko.crudgenerictest.domain.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {
}
```

- [ ] **Step 8: `GroupService` (v1 CrudGenericService)**

```java
package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.service.CrudGenericService;
import by.nhorushko.crudgenerictest.domain.dto.GroupDto;
import by.nhorushko.crudgenerictest.domain.entity.GroupEntity;
import by.nhorushko.crudgenerictest.mockmapper.GroupMapper;
import by.nhorushko.crudgenerictest.repository.GroupRepository;
import org.springframework.stereotype.Service;

@Service
public class GroupService extends CrudGenericService<GroupDto, GroupEntity, GroupRepository, GroupMapper> {
    public GroupService(GroupRepository repository, GroupMapper mapper) {
        super(repository, mapper, GroupDto.class, GroupEntity.class);
    }
}
```

- [ ] **Step 9: Compile**

Run: `mvn -q -pl test-application -am test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 10: Commit**

```bash
git add test-application/src/main/java/by/nhorushko/crudgenerictest/domain/entity/GroupEntity.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/domain/entity/ItemEntity.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/GroupDto.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/ItemDto.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/mockmapper/ItemMapper.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/mockmapper/GroupMapper.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/repository/GroupRepository.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/repository/ItemRepository.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/service/GroupService.java
git commit -m "test: add v1 parent/child cascade fixtures"
```

### Task 13: v1 cascade integration test

**Files:**
- Create: `test-application/src/test/java/by/nhorushko/crudgenerictest/service/SaveV1CascadeIT.java`

- [ ] **Step 1: Write the test**

```java
package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgenerictest.domain.dto.GroupDto;
import by.nhorushko.crudgenerictest.domain.dto.ItemDto;
import by.nhorushko.crudgenerictest.repository.GroupRepository;
import by.nhorushko.crudgenerictest.repository.ItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SaveV1CascadeIT {

    @Autowired
    private GroupService service;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private ItemRepository itemRepository;

    @AfterEach
    void cleanUp() {
        groupRepository.deleteAll();
    }

    @Test
    void newChildWithSentinelIdIsInsertedOnExistingParent() {
        // create an existing parent (no children)
        GroupDto created = service.save(new GroupDto(0L, "group", new ArrayList<>()));
        Long groupId = created.getId();
        assertThat(itemRepository.count()).isZero();

        // save the existing parent with a NEW child carrying sentinel id = 0
        GroupDto existing = service.getById(groupId);
        existing.getItems().add(new ItemDto(0L, "child"));

        GroupDto result = service.save(existing); // must NOT throw OptimisticLockException

        assertThat(result.getId()).isEqualTo(groupId);
        assertThat(itemRepository.count()).isEqualTo(1L);
        List<ItemDto> savedItems = service.getById(groupId).getItems();
        assertThat(savedItems).hasSize(1);
        assertThat(savedItems.get(0).getId()).isNotNull();
        assertThat(savedItems.get(0).getTitle()).isEqualTo("child");
    }
}
```

- [ ] **Step 2: Run**

Run: `mvn -q -pl test-application test -Dtest=SaveV1CascadeIT`
Expected: PASS (1 test), no `OptimisticLockException`.

- [ ] **Step 3: Commit**

```bash
git add test-application/src/test/java/by/nhorushko/crudgenerictest/service/SaveV1CascadeIT.java
git commit -m "test(v1): cascade insert of sentinel-id child on existing parent"
```

### Task 14: v2 assigned-id fixtures

**Files (create all):**
- `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/entity/RegionEntity.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/RegionDto.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/mapper/RegionMapper.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/repository/RegionRepository.java`
- `test-application/src/main/java/by/nhorushko/crudgenerictest/service/RegionServiceCRUD.java`

- [ ] **Step 1: `RegionEntity` (assigned id — no `@GeneratedValue`)**

```java
package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "region")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionEntity implements AbstractEntity<Long> {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;
}
```

- [ ] **Step 2: `RegionDto`**

```java
package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import lombok.Value;

@Value
public class RegionDto implements AbstractDto<Long> {
    Long id;
    String name;
}
```

- [ ] **Step 3: `RegionMapper`**

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import jakarta.persistence.EntityManager;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public final class RegionMapper extends AbsMapperEntityDto<RegionEntity, RegionDto> {
    public RegionMapper(ModelMapper modelMapper, EntityManager entityManager) {
        super(modelMapper, entityManager, RegionEntity.class, RegionDto.class);
    }

    @Override
    protected RegionDto create(RegionEntity from) {
        return new RegionDto(from.getId(), from.getName());
    }
}
```

- [ ] **Step 4: `RegionRepository`**

```java
package by.nhorushko.crudgenerictest.repository;

import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<RegionEntity, Long> {
}
```

- [ ] **Step 5: `RegionServiceCRUD`**

```java
package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.v2.service.AbsServiceCRUD;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import by.nhorushko.crudgenerictest.mapper.RegionMapper;
import by.nhorushko.crudgenerictest.repository.RegionRepository;
import org.springframework.stereotype.Service;

@Service
public class RegionServiceCRUD extends AbsServiceCRUD<Long, RegionEntity, RegionDto, RegionRepository> {
    public RegionServiceCRUD(RegionMapper mapper, RegionRepository repository) {
        super(mapper, repository);
    }
}
```

- [ ] **Step 6: Compile**

Run: `mvn -q -pl test-application -am test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add test-application/src/main/java/by/nhorushko/crudgenerictest/domain/entity/RegionEntity.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/RegionDto.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/mapper/RegionMapper.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/repository/RegionRepository.java \
        test-application/src/main/java/by/nhorushko/crudgenerictest/service/RegionServiceCRUD.java
git commit -m "test: add v2 assigned-id fixtures (RegionEntity)"
```

### Task 15: v2 integration test — sentinel + assigned-id insert/update

**Files:**
- Create: `test-application/src/test/java/by/nhorushko/crudgenerictest/service/SaveV2IT.java`

- [ ] **Step 1: Write the test**

```java
package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.dto.Tracker;
import by.nhorushko.crudgenerictest.repository.RegionRepository;
import by.nhorushko.crudgenerictest.repository.TrackerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SaveV2IT {

    @Autowired
    private TrackerServiceCRUD trackerService;
    @Autowired
    private TrackerRepository trackerRepository;
    @Autowired
    private RegionServiceCRUD regionService;
    @Autowired
    private RegionRepository regionRepository;

    @AfterEach
    void cleanUp() {
        regionRepository.deleteAll();
        // tracker rows 1..5 are seeded by data.sql; remove only ids we create (>= 100)
        trackerRepository.findAll().stream()
                .filter(t -> t.getId() != null && t.getId() >= 100L)
                .forEach(trackerRepository::delete);
    }

    @Test
    void sentinelZeroIdInsertsForIdentityEntity() {
        Tracker saved = trackerService.save(new Tracker(0L, "imei-x", "phone-x"));
        assertThat(saved.getId()).isNotNull();
        assertThat(trackerRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void assignedIdAbsentInserts() {
        RegionDto saved = regionService.save(new RegionDto(42L, "north"));
        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(regionRepository.existsById(42L)).isTrue();
    }

    @Test
    void assignedIdPresentUpdatesNoDuplicate() {
        regionService.save(new RegionDto(42L, "north"));
        RegionDto updated = regionService.save(new RegionDto(42L, "south"));

        assertThat(updated.getName()).isEqualTo("south");
        assertThat(regionRepository.count()).isEqualTo(1L);
        assertThat(regionRepository.findById(42L).orElseThrow().getName()).isEqualTo("south");
    }
}
```

- [ ] **Step 2: Run**

Run: `mvn -q -pl test-application test -Dtest=SaveV2IT`
Expected: PASS (3 tests). `assignedIdAbsentInserts` is the regression that fails on 6.6 without Fix B (`OptimisticLockException: ...Region#42`).

- [ ] **Step 3: Commit**

```bash
git add test-application/src/test/java/by/nhorushko/crudgenerictest/service/SaveV2IT.java
git commit -m "test(v2): integration tests for sentinel insert and assigned-id insert/update"
```

---

## Phase 6 — Full verification

### Task 16: Run the complete suite and fix fallout

- [ ] **Step 1: Build + full test run (unit + IT)**

Run: `mvn -q clean test -Dtest=*,*IT`
Expected: BUILD SUCCESS, all tests green.

- [ ] **Step 2: Triage failures**

Likely fallout to inspect if red:
- `CrudExpandGenericServiceTest` — `CrudExpandGenericService` overrides `checkIdForSave` to no-op and inherits the new `CrudAdditionalGenericService.save` (now `persistOrMerge`). If it asserted the old throw/`save` behavior, update expectations to match upsert routing.
- Any test asserting `repository.save` is invoked on a *create* path — update to expect `entityManager.persist` for new (null/sentinel) ids.
- Do **not** weaken assertions to force green; fix the test to reflect the intended new contract (save = upsert).

- [ ] **Step 3: Commit any fixes**

```bash
git add -A
git commit -m "test: align existing tests with save() upsert routing"
```

### Task 17: Update memory note (project knowledge)

- [ ] **Step 1: Record the fix** in `C:\Users\Nikolay\.claude\projects\C--Users-Nikolay-IdeaProjects-crud-generic\memory\` as a `project`-type memory: "save() is now an upsert across v1/v2/flex via `persistOrMerge`; sentinel `0→null` is the mapper's responsibility (post-converter); v1 `CrudGenericService.save` no longer guards against non-null ids." Add the one-line pointer to `MEMORY.md`.

---

## Self-Review

**Spec coverage:**
- Fix A (mapper `0→null`, root + cascade): Tasks 1, 2 (code), 10 (unit), 13 (cascade IT). ✅
- Fix B (`persistOrMerge`) v1: Tasks 3, 4, 5; v2: Tasks 6, 7; flex: Task 9. ✅
- v1 → upsert (guard removed): Tasks 4, 5; verified Task 11 (`saveWithExistingIdUpdatesNoDuplicate`). ✅
- Sentinel 0 owned by mapper, redundant service nullify removed: Tasks 2, 7. ✅
- Scenario 1 (sentinel root): Tasks 11, 15. Scenario 1 (cascade child): Task 13. ✅
- Scenario 2 (assigned/`@MapsId` absent→insert, present→update): Task 15. ✅
- Regression (existing id → single UPDATE): Tasks 11, 15. ✅
- Per family: v1 (11, 13), v2 (15), flex (9 code + unit routing via shared logic; full flex IT explicitly deferred — noted in File Structure). ⚠️ documented gap.
- `AbsServiceExtCRUD` left unchanged (create-only, covered by Fix A) — documented. ✅

**Placeholder scan:** No TBD/TODO; every code step has complete code; commands have expected output. (Task 16 Step 2 lists *conditional* fixes — acceptable, it is triage guidance, not a code placeholder.)

**Type consistency:** `persistOrMerge(ENTITY)` returns `ENTITY` in all three families; v1 uses `Long id`, v2 uses `ID id`, flex uses `ENTITY_ID id` — each matches its base class's id generic and `existsById` signature. `nullifyZeroId()` exists only on v2 `AbstractEntity` (used in Task 2); v1 uses the inline `setId(null)` (Task 1) since v1 `AbstractEntity` has no such method. Consistent.
