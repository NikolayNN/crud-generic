# Declarative FilterFields Registry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `AbsFilterSpecification` + the per-service `buildSpecification` switch with a single declarative `FilterFields` registry (one line per filterable field: name, path, type, allowed operations) in crud-generic 5.0 `flex.pageable`.

**Architecture:** A new immutable `FilterFields<ENTITY>` registry (built via a fluent `Builder` with built-in converters per type) becomes the single source of truth for filter validation, Specification building and sort mapping. `AbsFlexPagingAndSortingService` loses the `SPECS` type parameter and its abstract `buildSpecification`; it lazily builds the registry from one abstract `filterFields(builder)` method. All client-side filter errors raise the new `FilterValidationException` (apps map it to HTTP 400).

**Tech Stack:** Java 17, Spring Data JPA, filter-specifications-lib 3.1-jakarta (unchanged), library tests = JUnit 4.13.2 + Mockito, test-application = Spring Boot 3.2.1 + JUnit 5 + MockMvc + H2.

**Spec:** `docs/superpowers/specs/2026-07-14-pageable-filter-fields-design.md`

## Global Constraints

- Build with **JDK 17**. On this machine `JAVA_HOME` defaults to JDK 11, which Maven uses — so **every** `mvn` command must be prefixed to override it. Git Bash: `export JAVA_HOME="C:\Program Files\Java\jdk-17"; mvn ...`. PowerShell: `$env:JAVA_HOME="C:\Program Files\Java\jdk-17"; mvn ...`. Without this the Java-17 source fails to compile under javac 11.
- Library version stays `5.0-SNAPSHOT`; test-application depends on it from the local repo — after library changes run `mvn -f library/pom.xml install` before building test-application.
- Wire format for filter values is **unchanged**: `like#foo`, `eq#5`, `btn#a,b`, `en#null` (`FilterOperation` codes: eq, neq, gt, gte, lt, lte, in, nin, btn, like, en, nn).
- Sort syntax accepted after this change: `asc#field`, `desc#field`, bare `field` (= ascending). Legacy `+field`/`-field` → `FilterValidationException`.
- The old mechanism is fully deleted — no deprecation shims, no parallel API.
- Library test framework is JUnit 4 (`org.junit.Test`); test-application uses JUnit 5. Do not mix.
- All commands below are for PowerShell from the repo root `C:\Users\Nikolay\IdeaProjects\crud-generic`.

---

### Task 1: `FilterValidationException` + `FilterFields` builder

**Files:**
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/exception/FilterValidationException.java`
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/FilterFields.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/pageable/FilterFieldsBuilderTest.java`

**Interfaces:**
- Consumes: `by.nhorushko.filterspecification.Converters`, `FilterOperation`, `PageFilterRequest.Filter`, `org.springframework.data.jpa.domain.Specification` (all existing).
- Produces: `FilterFields.<E>builder()`, `FilterFields.<E>builder(Converters)`, `Builder` methods `string`, `ofLong`, `ofInteger`, `ofDouble`, `ofFloat`, `ofBoolean`, `instant`, `ofLocalDate`, `ofLocalDateTime` (each `(String name, FilterOperation... ops)` and `(String name, String path, FilterOperation... ops)`), `ofEnum(String, [String,] Class<E extends Enum<E>>, FilterOperation...)`, `field(String, String, Class<?>, FilterOperation...)`, `custom(String, Function<PageFilterRequest.Filter, Specification<ENTITY>>)`, `build()` → `FilterFields<ENTITY>`. Tasks 2–4 rely on these exact signatures and on the package-private `Entry` fields `name/path/operations/converter/customFactory`.

- [ ] **Step 1: Write the failing test**

```java
package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.filterspecification.Converters;
import org.junit.Test;

import java.math.BigDecimal;

import static by.nhorushko.filterspecification.FilterOperation.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class FilterFieldsBuilderTest {

    /** ENTITY type token for readability; never instantiated. */
    private static class Person {
    }

    private enum Status {ACTIVE, ARCHIVED}

    private static Converters convertersWithBigDecimal() {
        return new Converters() {
            @Override
            public void addConverters() {
                map.put(BigDecimal.class, BigDecimal::new);
            }
        };
    }

    @Test
    public void buildsRegistryWithEveryTypedMethod() {
        FilterFields<Person> fields = FilterFields.<Person>builder()
                .string("name", CONTAINS)
                .string("description", "details.description", CONTAINS)
                .ofLong("userId", "user.id", EQUAL)
                .ofInteger("count", GREATER_THAN)
                .ofDouble("weight", LESS_THAN)
                .ofFloat("ratio", LESS_THAN)
                .ofBoolean("active", EQUAL)
                .instant("createdAt", BETWEEN)
                .ofLocalDate("day", EQUAL)
                .ofLocalDateTime("startedAt", GREATER_THAN)
                .ofEnum("status", Status.class, EQUAL, IN)
                .ofEnum("subStatus", "sub.status", Status.class, EQUAL)
                .custom("special", filter -> (root, query, cb) -> cb.conjunction())
                .build();
        assertNotNull(fields);
    }

    @Test
    public void fieldEntryUsesRegisteredConverter() {
        FilterFields<Person> fields = FilterFields.<Person>builder(convertersWithBigDecimal())
                .field("score", "rating.score", BigDecimal.class, GREATER_THAN, LESS_THAN)
                .build();
        assertNotNull(fields);
    }

    @Test
    public void typedEntryWithoutOperationsIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> FilterFields.<Person>builder().string("name"));
    }

    @Test
    public void duplicateNameIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> FilterFields.<Person>builder()
                        .string("name", CONTAINS)
                        .string("name", EQUAL));
    }

    @Test
    public void fieldEntryWithoutConvertersIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> FilterFields.<Person>builder()
                        .field("score", "rating.score", BigDecimal.class, GREATER_THAN));
    }

    @Test
    public void fieldEntryWithoutRegisteredConverterIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> FilterFields.<Person>builder(convertersWithBigDecimal())
                        .field("headers", "req.headers", StringBuilder.class, EQUAL));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f library/pom.xml test "-Dtest=FilterFieldsBuilderTest"`
Expected: COMPILATION ERROR — `FilterFields` does not exist.

- [ ] **Step 3: Write the implementation**

`library/src/main/java/by/nhorushko/crudgeneric/flex/exception/FilterValidationException.java`:

```java
package by.nhorushko.crudgeneric.flex.exception;

/**
 * Client-side error in a page request: unknown filter field, disallowed
 * filter operation, a value that fails conversion, or a malformed sort
 * expression. Applications should map it to HTTP 400.
 */
public class FilterValidationException extends RuntimeException {

    public FilterValidationException(String message) {
        super(message);
    }

    public FilterValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

`library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/FilterFields.java` (builder part; `toSpecification` and `sort` are added in Tasks 2–3):

```java
package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.filterspecification.Converters;
import by.nhorushko.filterspecification.FilterOperation;
import by.nhorushko.filterspecification.FilterSpecifications;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Immutable registry of filterable fields — the single place a filterable
 * field of a paged endpoint is declared. Each entry records the external
 * filter name, the entity path, the value converter and the allowed
 * operations; the registry validates incoming filters and builds
 * {@link Specification}s and sort mappings from the same declaration.
 * <p>
 * Typed builder methods carry built-in converters. Only {@link Builder#field}
 * needs a {@link Converters} instance (use {@link #builder(Converters)}).
 */
public final class FilterFields<ENTITY> {

    private final Map<String, Entry<ENTITY>> entries;
    @SuppressWarnings("rawtypes")
    private final FilterSpecifications specifications = new FilterSpecifications();

    private FilterFields(Map<String, Entry<ENTITY>> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    public static <E> Builder<E> builder() {
        return new Builder<>(null);
    }

    public static <E> Builder<E> builder(Converters converters) {
        Objects.requireNonNull(converters, "converters");
        return new Builder<>(converters);
    }

    static final class Entry<ENTITY> {
        final String name;
        final String path;
        final Set<FilterOperation> operations;
        final Function<String, ? extends Comparable<?>> converter;
        final Function<PageFilterRequest.Filter, Specification<ENTITY>> customFactory;

        Entry(String name, String path, Set<FilterOperation> operations,
              Function<String, ? extends Comparable<?>> converter,
              Function<PageFilterRequest.Filter, Specification<ENTITY>> customFactory) {
            this.name = name;
            this.path = path;
            this.operations = operations;
            this.converter = converter;
            this.customFactory = customFactory;
        }
    }

    public static final class Builder<ENTITY> {

        private final Converters converters;
        private final Map<String, Entry<ENTITY>> entries = new LinkedHashMap<>();

        private Builder(Converters converters) {
            this.converters = converters;
        }

        public Builder<ENTITY> string(String name, FilterOperation... operations) {
            return string(name, name, operations);
        }

        public Builder<ENTITY> string(String name, String path, FilterOperation... operations) {
            return typed(name, path, s -> s, operations);
        }

        public Builder<ENTITY> ofLong(String name, FilterOperation... operations) {
            return ofLong(name, name, operations);
        }

        public Builder<ENTITY> ofLong(String name, String path, FilterOperation... operations) {
            return typed(name, path, Long::valueOf, operations);
        }

        public Builder<ENTITY> ofInteger(String name, FilterOperation... operations) {
            return ofInteger(name, name, operations);
        }

        public Builder<ENTITY> ofInteger(String name, String path, FilterOperation... operations) {
            return typed(name, path, Integer::valueOf, operations);
        }

        public Builder<ENTITY> ofDouble(String name, FilterOperation... operations) {
            return ofDouble(name, name, operations);
        }

        public Builder<ENTITY> ofDouble(String name, String path, FilterOperation... operations) {
            return typed(name, path, Double::valueOf, operations);
        }

        public Builder<ENTITY> ofFloat(String name, FilterOperation... operations) {
            return ofFloat(name, name, operations);
        }

        public Builder<ENTITY> ofFloat(String name, String path, FilterOperation... operations) {
            return typed(name, path, Float::valueOf, operations);
        }

        public Builder<ENTITY> ofBoolean(String name, FilterOperation... operations) {
            return ofBoolean(name, name, operations);
        }

        public Builder<ENTITY> ofBoolean(String name, String path, FilterOperation... operations) {
            return typed(name, path, Boolean::valueOf, operations);
        }

        public Builder<ENTITY> instant(String name, FilterOperation... operations) {
            return instant(name, name, operations);
        }

        public Builder<ENTITY> instant(String name, String path, FilterOperation... operations) {
            return typed(name, path, Instant::parse, operations);
        }

        public Builder<ENTITY> ofLocalDate(String name, FilterOperation... operations) {
            return ofLocalDate(name, name, operations);
        }

        public Builder<ENTITY> ofLocalDate(String name, String path, FilterOperation... operations) {
            return typed(name, path, LocalDate::parse, operations);
        }

        public Builder<ENTITY> ofLocalDateTime(String name, FilterOperation... operations) {
            return ofLocalDateTime(name, name, operations);
        }

        public Builder<ENTITY> ofLocalDateTime(String name, String path, FilterOperation... operations) {
            return typed(name, path, LocalDateTime::parse, operations);
        }

        public <E extends Enum<E>> Builder<ENTITY> ofEnum(String name, Class<E> enumClass,
                                                          FilterOperation... operations) {
            return ofEnum(name, name, enumClass, operations);
        }

        public <E extends Enum<E>> Builder<ENTITY> ofEnum(String name, String path, Class<E> enumClass,
                                                          FilterOperation... operations) {
            Objects.requireNonNull(enumClass, "enumClass");
            return typed(name, path, value -> Enum.valueOf(enumClass, value), operations);
        }

        /**
         * Field of any other type; the converter must be registered in the
         * application's {@link Converters} subclass and the builder must be
         * created via {@link FilterFields#builder(Converters)}.
         */
        public Builder<ENTITY> field(String name, String path, Class<?> type, FilterOperation... operations) {
            Objects.requireNonNull(type, "type");
            if (converters == null) {
                throw new IllegalStateException(String.format(
                        "Field '%s': builder was created without Converters; use FilterFields.builder(converters)", name));
            }
            Function<String, ? extends Comparable<?>> converter = converters.getFunction(type);
            if (converter == null) {
                throw new IllegalStateException(String.format(
                        "Field '%s': no converter registered for type %s", name, type.getName()));
            }
            return typed(name, path, converter, operations);
        }

        /**
         * Escape hatch: the factory receives the raw filter and builds the
         * {@link Specification} itself. Operations are not validated.
         */
        public Builder<ENTITY> custom(String name,
                                      Function<PageFilterRequest.Filter, Specification<ENTITY>> factory) {
            Objects.requireNonNull(factory, "factory");
            put(new Entry<>(name, name, Set.of(), null, factory));
            return this;
        }

        private Builder<ENTITY> typed(String name, String path,
                                      Function<String, ? extends Comparable<?>> converter,
                                      FilterOperation... operations) {
            if (operations.length == 0) {
                throw new IllegalStateException(String.format(
                        "Field '%s' must declare at least one operation", name));
            }
            put(new Entry<>(name, path, Set.of(operations), converter, null));
            return this;
        }

        private void put(Entry<ENTITY> entry) {
            Objects.requireNonNull(entry.name, "name");
            Objects.requireNonNull(entry.path, "path");
            if (entries.putIfAbsent(entry.name, entry) != null) {
                throw new IllegalStateException(String.format("Duplicate filter field: '%s'", entry.name));
            }
        }

        public FilterFields<ENTITY> build() {
            return new FilterFields<>(entries);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -f library/pom.xml test "-Dtest=FilterFieldsBuilderTest"`
Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```powershell
git add library/src/main/java/by/nhorushko/crudgeneric/flex/exception/FilterValidationException.java library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/FilterFields.java library/src/test/java/by/nhorushko/crudgeneric/flex/pageable/FilterFieldsBuilderTest.java
git commit -m "feat(flex): add FilterFields registry builder and FilterValidationException"
```

---

### Task 2: `FilterFields.toSpecification`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/FilterFields.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/pageable/FilterFieldsToSpecificationTest.java`

**Interfaces:**
- Consumes: `Entry` fields and `entries` map from Task 1; `FilterCriteria`, `FilterSpecificationUtils.getOperation` from filter-specifications-lib.
- Produces: `public Optional<Specification<ENTITY>> toSpecification(PageFilterRequest.Filter filter)` — Task 4's service walks filter groups with exactly this signature. Throws `FilterValidationException` on unknown field / disallowed or unparseable operation / unconvertible value; returns `Optional.empty()` for blank values.

- [ ] **Step 1: Write the failing test**

```java
package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import org.junit.Test;

import java.util.Optional;

import static by.nhorushko.filterspecification.FilterOperation.*;
import static org.junit.Assert.*;

public class FilterFieldsToSpecificationTest {

    private static class Person {
    }

    private enum Status {ACTIVE, ARCHIVED}

    private final FilterFields<Person> fields = FilterFields.<Person>builder()
            .string("name", CONTAINS)
            .ofLong("userId", "user.id", EQUAL)
            .instant("createdAt", BETWEEN, IS_NULL, NOT_NULL)
            .ofEnum("status", Status.class, EQUAL)
            .custom("special", filter -> (root, query, cb) -> cb.conjunction())
            .build();

    private static PageFilterRequest.Filter filter(String name, String value) {
        return new PageFilterRequest.Filter(name, value);
    }

    @Test
    public void buildsSpecificationForAllowedOperation() {
        assertTrue(fields.toSpecification(filter("name", "like#john")).isPresent());
        assertTrue(fields.toSpecification(filter("userId", "eq#5")).isPresent());
        assertTrue(fields.toSpecification(
                filter("createdAt", "btn#2024-01-01T00:00:00Z,2024-12-31T00:00:00Z")).isPresent());
        assertTrue(fields.toSpecification(filter("createdAt", "en#null")).isPresent());
        assertTrue(fields.toSpecification(filter("status", "eq#ACTIVE")).isPresent());
    }

    @Test
    public void blankValueIsSkipped() {
        assertEquals(Optional.empty(), fields.toSpecification(filter("name", null)));
        assertEquals(Optional.empty(), fields.toSpecification(filter("name", "  ")));
        assertEquals(Optional.empty(), fields.toSpecification(filter("special", "")));
    }

    @Test
    public void unknownFieldIsRejected() {
        FilterValidationException e = assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("nope", "eq#1")));
        assertTrue(e.getMessage().contains("nope"));
    }

    @Test
    public void disallowedOperationIsRejected() {
        FilterValidationException e = assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("name", "eq#john")));
        assertTrue(e.getMessage().contains("name"));
    }

    @Test
    public void malformedOperationIsRejected() {
        assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("name", "garbage")));
        assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("name", "zz#john")));
    }

    @Test
    public void unconvertibleValueIsRejected() {
        assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("userId", "eq#abc")));
        assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("status", "eq#NO_SUCH")));
        assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("createdAt", "btn#not-a-date,2024-12-31T00:00:00Z")));
    }

    @Test
    public void customEntryDelegatesToFactory() {
        assertTrue(fields.toSpecification(filter("special", "eq#anything")).isPresent());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f library/pom.xml test "-Dtest=FilterFieldsToSpecificationTest"`
Expected: COMPILATION ERROR — `toSpecification` not defined.

- [ ] **Step 3: Add `toSpecification` to `FilterFields`**

Add imports to `FilterFields.java`:

```java
import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import by.nhorushko.filterspecification.FilterCriteria;
import by.nhorushko.filterspecification.FilterSpecificationUtils;
import java.util.Optional;
```

Add methods to the `FilterFields` class body (after `builder(...)`):

```java
    /**
     * Validates the filter against this registry and builds a specification.
     * Blank values produce {@link Optional#empty()} (the filter is skipped).
     *
     * @throws FilterValidationException on unknown field, disallowed or
     *                                   unparseable operation, or a value that fails conversion
     */
    public Optional<Specification<ENTITY>> toSpecification(PageFilterRequest.Filter filter) {
        Entry<ENTITY> entry = entries.get(filter.getName());
        if (entry == null) {
            throw new FilterValidationException(String.format("Unknown filter field: '%s'", filter.getName()));
        }
        String value = filter.getFilter();
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        if (entry.customFactory != null) {
            return Optional.of(entry.customFactory.apply(filter));
        }
        FilterOperation operation = FilterSpecificationUtils.getOperation(value);
        if (operation == null || !entry.operations.contains(operation)) {
            throw new FilterValidationException(String.format(
                    "Filter: '%s', expect operations: '%s', but was '%s'",
                    filter.getName(), entry.operations, operation));
        }
        return Optional.of(buildSpecification(entry, value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Specification<ENTITY> buildSpecification(Entry<ENTITY> entry, String filterValue) {
        FilterCriteria criteria;
        try {
            criteria = new FilterCriteria(entry.path, filterValue, entry.converter);
        } catch (RuntimeException e) {
            throw new FilterValidationException(String.format(
                    "Filter '%s': can't parse value '%s': %s", entry.name, filterValue, e.getMessage()), e);
        }
        return (Specification<ENTITY>) specifications.getSpecification(criteria.getOperation()).apply(criteria);
    }
```

Note: `FilterCriteria` converts values eagerly in its constructor, so every conversion failure (bad number, bad date, unknown enum constant) is caught by the `try` block above.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -f library/pom.xml test "-Dtest=FilterFieldsToSpecificationTest"`
Expected: `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```powershell
git add library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/FilterFields.java library/src/test/java/by/nhorushko/crudgeneric/flex/pageable/FilterFieldsToSpecificationTest.java
git commit -m "feat(flex): FilterFields.toSpecification with built-in validation"
```

---

### Task 3: `FilterFields.sort`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/FilterFields.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/pageable/FilterFieldsSortTest.java`

**Interfaces:**
- Consumes: `entries` map from Task 1.
- Produces: `public Sort sort(String sortExpression)` returning `org.springframework.data.domain.Sort` — Task 4's service calls `PageRequest.of(page, size, fields.sort(request.getSort()))`.

- [ ] **Step 1: Write the failing test**

```java
package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import org.junit.Test;
import org.springframework.data.domain.Sort;

import static by.nhorushko.filterspecification.FilterOperation.CONTAINS;
import static by.nhorushko.filterspecification.FilterOperation.EQUAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class FilterFieldsSortTest {

    private static class Person {
    }

    private final FilterFields<Person> fields = FilterFields.<Person>builder()
            .string("name", CONTAINS)
            .ofLong("userId", "user.id", EQUAL)
            .build();

    @Test
    public void parsesAscAndDesc() {
        assertEquals(Sort.by(Sort.Direction.ASC, "name"), fields.sort("asc#name"));
        assertEquals(Sort.by(Sort.Direction.DESC, "name"), fields.sort("desc#name"));
    }

    @Test
    public void barePropertyIsAscending() {
        assertEquals(Sort.by(Sort.Direction.ASC, "name"), fields.sort("name"));
    }

    @Test
    public void mapsPropertyThroughRegistry() {
        assertEquals(Sort.by(Sort.Direction.DESC, "user.id"), fields.sort("desc#userId"));
    }

    @Test
    public void unknownPropertyPassesThrough() {
        assertEquals(Sort.by(Sort.Direction.ASC, "id"), fields.sort("asc#id"));
    }

    @Test
    public void legacyPlusMinusSyntaxIsRejected() {
        assertThrows(FilterValidationException.class, () -> fields.sort("+name"));
        assertThrows(FilterValidationException.class, () -> fields.sort("-name"));
    }

    @Test
    public void malformedExpressionIsRejected() {
        assertThrows(FilterValidationException.class, () -> fields.sort("up#name"));
        assertThrows(FilterValidationException.class, () -> fields.sort("asc#"));
        assertThrows(FilterValidationException.class, () -> fields.sort(""));
        assertThrows(FilterValidationException.class, () -> fields.sort(null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f library/pom.xml test "-Dtest=FilterFieldsSortTest"`
Expected: COMPILATION ERROR — `sort` not defined.

- [ ] **Step 3: Add `sort` to `FilterFields`**

Add import: `import org.springframework.data.domain.Sort;`

Add method after `toSpecification`:

```java
    /**
     * Parses a sort expression — {@code asc#field}, {@code desc#field} or a
     * bare {@code field} (ascending) — and maps the property through this
     * registry ({@code userId} → {@code user.id}). Unknown properties pass
     * through unchanged. The legacy {@code +field}/{@code -field} syntax is
     * rejected: {@code +} is URL-decoded to a space and caused real bugs.
     *
     * @throws FilterValidationException on blank, legacy or malformed expressions
     */
    public Sort sort(String sortExpression) {
        if (sortExpression == null || sortExpression.trim().isEmpty()) {
            throw new FilterValidationException("Sort expression is blank");
        }
        String expression = sortExpression.trim();
        if (expression.startsWith("+") || expression.startsWith("-")) {
            throw new FilterValidationException(String.format(
                    "Legacy sort syntax '%s' is not supported; use asc#field or desc#field", expression));
        }
        Sort.Direction direction = Sort.Direction.ASC;
        String property = expression;
        if (expression.startsWith("asc#")) {
            property = expression.substring("asc#".length());
        } else if (expression.startsWith("desc#")) {
            direction = Sort.Direction.DESC;
            property = expression.substring("desc#".length());
        } else if (expression.contains("#")) {
            throw new FilterValidationException(String.format("Malformed sort expression: '%s'", expression));
        }
        if (property.isEmpty()) {
            throw new FilterValidationException(String.format("Malformed sort expression: '%s'", expression));
        }
        Entry<ENTITY> entry = entries.get(property);
        return Sort.by(direction, entry != null ? entry.path : property);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -f library/pom.xml test "-Dtest=FilterFieldsSortTest"`
Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```powershell
git add library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/FilterFields.java library/src/test/java/by/nhorushko/crudgeneric/flex/pageable/FilterFieldsSortTest.java
git commit -m "feat(flex): FilterFields.sort with asc#/desc# parsing and path mapping"
```

---

### Task 4: Rework `AbsFlexPagingAndSortingService`, delete `AbsFilterSpecification`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/AbsFlexPagingAndSortingService.java` (full rewrite)
- Delete: `library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/AbsFilterSpecification.java`
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/BasePageRequest.java` (default sort + javadoc)
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/PageFilterRequest.java` (sort javadoc only)
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/pageable/AbsFlexPagingAndSortingServiceTest.java`

**Interfaces:**
- Consumes: `FilterFields`, `toSpecification`, `sort` from Tasks 1–3; `AbsModelMapper.map(Object, Class)`.
- Produces: `AbsFlexPagingAndSortingService<ID, DTO, ENTITY>` with constructors `(JpaSpecificationExecutor<ENTITY>, AbsModelMapper, Class<DTO>)` and `(..., Converters)`, abstract `protected FilterFields<ENTITY> filterFields(FilterFields.Builder<ENTITY> builder)`, overridable `protected DTO toDto(ENTITY)`, `public Page<DTO> page(PageFilterRequest)`. Tasks 5–6 subclass exactly this.

- [ ] **Step 1: Write the failing test**

```java
package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

import static by.nhorushko.filterspecification.FilterOperation.CONTAINS;
import static by.nhorushko.filterspecification.FilterOperation.EQUAL;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbsFlexPagingAndSortingServiceTest {

    static class PersonEntity implements AbstractEntity<Long> {
        private Long id;

        public PersonEntity(Long id) {
            this.id = id;
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

    static class PersonDto implements AbstractDto<Long> {
        private final Long id;

        PersonDto(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    static class PersonPageableService extends AbsFlexPagingAndSortingService<Long, PersonDto, PersonEntity> {

        PersonPageableService(JpaSpecificationExecutor<PersonEntity> repository) {
            super(repository, null, PersonDto.class);
        }

        @Override
        protected FilterFields<PersonEntity> filterFields(FilterFields.Builder<PersonEntity> builder) {
            return builder
                    .string("name", CONTAINS)
                    .ofLong("userId", "user.id", EQUAL)
                    .build();
        }

        @Override
        protected PersonDto toDto(PersonEntity entity) {
            return new PersonDto(entity.getId());
        }
    }

    @SuppressWarnings("unchecked")
    private final JpaSpecificationExecutor<PersonEntity> repository = mock(JpaSpecificationExecutor.class);
    private PersonPageableService service;

    @Before
    public void setUp() {
        service = new PersonPageableService(repository);
        // ArgumentMatchers.<T>any() (unlike any(Class)) also matches the null
        // Specification passed when no filters are present.
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<PersonEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new PersonEntity(1L))));
    }

    @Test
    public void pageBuildsPageRequestWithMappedSort() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        service.page(PageFilterRequest.pageRequestAnd(2, 15, "desc#userId",
                new PageFilterRequest.Filter("name", "like#jo")));

        org.mockito.Mockito.verify(repository)
                .findAll(org.mockito.ArgumentMatchers.<Specification<PersonEntity>>any(), pageable.capture());
        assertEquals(PageRequest.of(2, 15, Sort.by(Sort.Direction.DESC, "user.id")), pageable.getValue());
    }

    @Test
    public void pageWithoutFiltersPassesNullSpecification() {
        ArgumentCaptor<Specification<PersonEntity>> spec = ArgumentCaptor.forClass(Specification.class);

        service.page(PageFilterRequest.pageRequestAnd(0, 20, "asc#name"));

        org.mockito.Mockito.verify(repository).findAll(spec.capture(), any(Pageable.class));
        assertNull(spec.getValue());
    }

    @Test
    public void pageMapsEntitiesWithToDto() {
        assertEquals(Long.valueOf(1L),
                service.page(PageFilterRequest.pageRequestAnd(0, 20, "asc#name"))
                        .getContent().get(0).getId());
    }

    @Test
    public void invalidFilterPropagatesValidationError() {
        assertThrows(FilterValidationException.class,
                () -> service.page(PageFilterRequest.pageRequestAnd(0, 20, "asc#name",
                        new PageFilterRequest.Filter("name", "eq#jo"))));
    }

    @Test
    public void legacySortPropagatesValidationError() {
        assertThrows(FilterValidationException.class,
                () -> service.page(PageFilterRequest.pageRequestAnd(0, 20, "-name")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f library/pom.xml test "-Dtest=AbsFlexPagingAndSortingServiceTest"`
Expected: COMPILATION ERROR — current class still requires the `SPECS` type parameter and its constructor takes `(repository, filterSpecs)`.

- [ ] **Step 3: Rewrite the service, delete the old class, update defaults**

Replace the entire content of `AbsFlexPagingAndSortingService.java` with:

```java
package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import by.nhorushko.filterspecification.Converters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Base service for paged, filtered, sorted reads. Subclasses declare their
 * filterable fields once in {@link #filterFields(FilterFields.Builder)};
 * validation, specification building and sort mapping all derive from that
 * single declaration.
 */
public abstract class AbsFlexPagingAndSortingService<
        ID,
        DTO extends AbstractDto<ID>,
        ENTITY extends AbstractEntity<ID>> {

    protected final JpaSpecificationExecutor<ENTITY> repository;
    protected final AbsModelMapper mapper;
    private final Class<DTO> dtoClass;
    private final Converters converters;
    private volatile FilterFields<ENTITY> filterFields;

    public AbsFlexPagingAndSortingService(JpaSpecificationExecutor<ENTITY> repository,
                                          AbsModelMapper mapper,
                                          Class<DTO> dtoClass) {
        this(repository, mapper, dtoClass, null);
    }

    /**
     * Use this constructor only when {@link #filterFields(FilterFields.Builder)}
     * declares {@code field()} entries, which resolve their converter from the
     * application's {@link Converters} subclass.
     */
    public AbsFlexPagingAndSortingService(JpaSpecificationExecutor<ENTITY> repository,
                                          AbsModelMapper mapper,
                                          Class<DTO> dtoClass,
                                          Converters converters) {
        this.repository = repository;
        this.mapper = mapper;
        this.dtoClass = dtoClass;
        this.converters = converters;
    }

    public Page<DTO> page(PageFilterRequest request) {
        FilterFields<ENTITY> fields = fields();
        Specification<ENTITY> specification = buildSpecFromFilterGroup(fields, request.getFilterGroup());
        Pageable pageable = PageRequest.of(request.getPage(), request.getPageSize(), fields.sort(request.getSort()));
        return repository.findAll(specification, pageable).map(this::toDto);
    }

    /**
     * Declares the filterable fields of this endpoint — the single place a
     * field's name, path, type and allowed operations are defined. Called
     * once; the result is cached.
     */
    protected abstract FilterFields<ENTITY> filterFields(FilterFields.Builder<ENTITY> builder);

    protected DTO toDto(ENTITY entity) {
        return mapper.map(entity, dtoClass);
    }

    private FilterFields<ENTITY> fields() {
        FilterFields<ENTITY> result = filterFields;
        if (result == null) {
            synchronized (this) {
                result = filterFields;
                if (result == null) {
                    FilterFields.Builder<ENTITY> builder = converters == null
                            ? FilterFields.builder()
                            : FilterFields.builder(converters);
                    result = filterFields(builder);
                    filterFields = result;
                }
            }
        }
        return result;
    }

    private Specification<ENTITY> buildSpecFromFilterGroup(FilterFields<ENTITY> fields,
                                                           PageFilterRequest.FilterGroup filterGroup) {
        if (filterGroup == null || filterGroup.isEmpty()) {
            return null;
        }

        List<Specification<ENTITY>> specs = new ArrayList<>();

        for (PageFilterRequest.Filter filter : filterGroup.getFilters()) {
            fields.toSpecification(filter).ifPresent(specs::add);
        }

        for (PageFilterRequest.FilterGroup subgroup : filterGroup.getSubGroups()) {
            Specification<ENTITY> subgroupSpec = buildSpecFromFilterGroup(fields, subgroup);
            if (subgroupSpec != null) {
                specs.add(subgroupSpec);
            }
        }

        if (specs.isEmpty()) {
            return null;
        }

        Specification<ENTITY> resultSpec = specs.get(0);
        for (int i = 1; i < specs.size(); i++) {
            resultSpec = filterGroup.getCondition() == PageFilterRequest.ConcatCondition.AND
                    ? resultSpec.and(specs.get(i))
                    : resultSpec.or(specs.get(i));
        }
        return resultSpec;
    }
}
```

Delete the old class:

```powershell
git rm library/src/main/java/by/nhorushko/crudgeneric/flex/pageable/AbsFilterSpecification.java
```

In `BasePageRequest.java` change the sort field (keep the rest of the class):

```java
    /**
     * The sorting criterion used to order the data returned in the page. The format is
     * {@code asc#field} or {@code desc#field}; a bare field name sorts ascending. By default,
     * records are sorted by the ID in descending order.
     *
     * Example: "desc#startTime" sorts by startTime in descending order, while "asc#startTime"
     * (or "startTime") sorts in ascending order.
     */
    @Parameter(description = "sort Criteria. Example: sort=desc#startTime [startTime, status]")
    protected String sort = "desc#id";
```

In `PageFilterRequest.java` update the `sort` field javadoc (lines 13–19) to:

```java
    /**
     * sort looks like asc# OR desc# and property name (bare property name = ascending):
     * asc#id
     * desc#id
     * asc#name
     * desc#name
     */
```

- [ ] **Step 4: Run the full library test suite**

Run: `mvn -f library/pom.xml test`
Expected: BUILD SUCCESS, all tests green (new tests + `FilterGroupBuilderTest` + existing flex tests). No references to `AbsFilterSpecification` remain (`git grep AbsFilterSpecification library/src` returns only nothing or README hits handled in Task 7).

- [ ] **Step 5: Install the library for downstream tasks**

Run: `mvn -f library/pom.xml install "-DskipTests"`
Expected: BUILD SUCCESS — `5.0-SNAPSHOT` in the local repo now contains the new API.

- [ ] **Step 6: Commit**

```powershell
git add -A library/src
git commit -m "feat(flex)!: FilterFields-driven AbsFlexPagingAndSortingService; drop AbsFilterSpecification"
```

---

### Task 5: test-application fixture — Meeting paged endpoint

**Files:**
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/entity/MeetingEntity.java`
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/entity/MeetingStatus.java`
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/MeetingDto.java`
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/mapper/MeetingMapper.java`
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/repository/MeetingRepository.java`
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/service/MeetingPageableService.java`
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/controller/MeetingController.java`
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/controller/TestExceptionHandler.java`
- Test: `test-application/src/test/java/by/nhorushko/crudgenerictest/pageable/MeetingPageSmokeIT.java`

**Interfaces:**
- Consumes: `AbsFlexPagingAndSortingService(repository, mapper, MeetingDto.class)`, `FilterFields.Builder` (Task 4), existing `RegionEntity` (assigned id, `name`), `AbsMapEntityToDto`, `AbsModelMapper`.
- Produces: `GET /meeting/page` endpoint with query params `page`, `size`, `sort` (default `desc#id`), `titleFilter`, `statusFilter`, `startTimeFilter`, `regionIdFilter`, `dayFilter`; `MeetingRepository` (also `JpaSpecificationExecutor`); HTTP 400 via `TestExceptionHandler` for `FilterValidationException`. Task 6 tests exactly these params and filter names `title`, `status`, `startTime`, `regionId`, `day`.

- [ ] **Step 1: Write the failing smoke test**

```java
package by.nhorushko.crudgenerictest.pageable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingPageSmokeIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void emptyPageIsReturnedWithoutFilters() throws Exception {
        mockMvc.perform(get("/meeting/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f test-application/pom.xml test "-Dtest=MeetingPageSmokeIT"`
Expected: COMPILATION ok but test FAILS with 404 — endpoint does not exist (or compilation error if run after writing only the test — either failure is acceptable evidence).

- [ ] **Step 3: Write the fixture**

`MeetingStatus.java`:

```java
package by.nhorushko.crudgenerictest.domain.entity;

public enum MeetingStatus {
    PLANNED, DONE, CANCELED
}
```

`MeetingEntity.java`:

```java
package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Fixture for the flex pageable stack: one field per supported filter kind —
 * string, enum, instant, local date and a nested path via {@link RegionEntity}.
 */
@Entity
@Table(name = "meeting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingEntity implements AbstractEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MeetingStatus status;

    @Column(name = "start_time")
    private Instant startTime;

    // "day" is a reserved word in H2 2.x
    @Column(name = "meeting_day")
    private LocalDate day;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private RegionEntity region;
}
```

`MeetingDto.java`:

```java
package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgenerictest.domain.entity.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDto implements AbstractDto<Long> {
    private Long id;
    private String title;
    private MeetingStatus status;
    private Instant startTime;
}
```

`MeetingMapper.java` (registers the entity→read-DTO map so the service's default `toDto` works):

```java
package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapEntityToDto;
import by.nhorushko.crudgenerictest.domain.dto.MeetingDto;
import by.nhorushko.crudgenerictest.domain.entity.MeetingEntity;
import org.springframework.stereotype.Component;

@Component
public class MeetingMapper extends AbsMapEntityToDto<MeetingEntity, MeetingDto> {

    public MeetingMapper(AbsModelMapper mapper) {
        super(mapper, MeetingEntity.class, MeetingDto.class);
    }

    @Override
    protected MeetingDto create(MeetingEntity entity) {
        return new MeetingDto(entity.getId(), entity.getTitle(), entity.getStatus(), entity.getStartTime());
    }
}
```

`MeetingRepository.java`:

```java
package by.nhorushko.crudgenerictest.repository;

import by.nhorushko.crudgenerictest.domain.entity.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MeetingRepository extends JpaRepository<MeetingEntity, Long>, JpaSpecificationExecutor<MeetingEntity> {
}
```

`MeetingPageableService.java`:

```java
package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.pageable.AbsFlexPagingAndSortingService;
import by.nhorushko.crudgeneric.flex.pageable.FilterFields;
import by.nhorushko.crudgenerictest.domain.dto.MeetingDto;
import by.nhorushko.crudgenerictest.domain.entity.MeetingEntity;
import by.nhorushko.crudgenerictest.domain.entity.MeetingStatus;
import by.nhorushko.crudgenerictest.repository.MeetingRepository;
import org.springframework.stereotype.Service;

import static by.nhorushko.filterspecification.FilterOperation.*;

@Service
public class MeetingPageableService extends AbsFlexPagingAndSortingService<Long, MeetingDto, MeetingEntity> {

    public MeetingPageableService(MeetingRepository repository, AbsModelMapper mapper) {
        super(repository, mapper, MeetingDto.class);
    }

    @Override
    protected FilterFields<MeetingEntity> filterFields(FilterFields.Builder<MeetingEntity> f) {
        return f.string("title", CONTAINS)
                .ofEnum("status", MeetingStatus.class, EQUAL, IN)
                .instant("startTime", GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN, LESSTHAN_OR_EQUAL_TO, BETWEEN)
                .ofLocalDate("day", EQUAL, BETWEEN)
                .ofLong("regionId", "region.id", EQUAL)
                .build();
    }
}
```

`MeetingController.java`:

```java
package by.nhorushko.crudgenerictest.controller;

import by.nhorushko.crudgeneric.flex.pageable.PageFilterRequest;
import by.nhorushko.crudgenerictest.domain.dto.MeetingDto;
import by.nhorushko.crudgenerictest.service.MeetingPageableService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static by.nhorushko.crudgeneric.flex.pageable.PageFilterRequest.pageRequestAnd;

@RestController
@RequestMapping("/meeting")
public class MeetingController {

    private final MeetingPageableService service;

    public MeetingController(MeetingPageableService service) {
        this.service = service;
    }

    @GetMapping("/page")
    public Page<MeetingDto> page(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "desc#id") String sort,
            @RequestParam(value = "titleFilter", required = false) String titleFilter,
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            @RequestParam(value = "startTimeFilter", required = false) String startTimeFilter,
            @RequestParam(value = "regionIdFilter", required = false) String regionIdFilter,
            @RequestParam(value = "dayFilter", required = false) String dayFilter) {

        PageFilterRequest request = pageRequestAnd(page, size, sort,
                new PageFilterRequest.Filter("title", titleFilter),
                new PageFilterRequest.Filter("status", statusFilter),
                new PageFilterRequest.Filter("startTime", startTimeFilter),
                new PageFilterRequest.Filter("regionId", regionIdFilter),
                new PageFilterRequest.Filter("day", dayFilter));
        return service.page(request);
    }
}
```

`TestExceptionHandler.java`:

```java
package by.nhorushko.crudgenerictest.controller;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TestExceptionHandler {

    @ExceptionHandler(FilterValidationException.class)
    public ResponseEntity<String> handleFilterValidation(FilterValidationException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -f test-application/pom.xml test "-Dtest=MeetingPageSmokeIT"`
Expected: `Tests run: 1, Failures: 0, Errors: 0` (H2 schema for `meeting` is created automatically by Hibernate ddl-auto).

- [ ] **Step 5: Commit**

```powershell
git add test-application/src
git commit -m "test(test-application): Meeting paged-endpoint fixture on FilterFields"
```

---

### Task 6: Full pageable IT coverage

**Files:**
- Test (create): `test-application/src/test/java/by/nhorushko/crudgenerictest/pageable/MeetingPageIT.java`

**Interfaces:**
- Consumes: `GET /meeting/page` params from Task 5, `MeetingRepository`, `RegionRepository` (existing), `MeetingPageableService.page` + `PageFilterRequest` for the service-level unknown-field case.
- Produces: regression coverage; nothing downstream.

- [ ] **Step 1: Write the tests (they must pass immediately — this task verifies Task 5's fixture end-to-end; a failure here is a bug in Tasks 1–5)**

```java
package by.nhorushko.crudgenerictest.pageable;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import by.nhorushko.crudgeneric.flex.pageable.PageFilterRequest;
import by.nhorushko.crudgenerictest.domain.entity.MeetingEntity;
import by.nhorushko.crudgenerictest.domain.entity.MeetingStatus;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import by.nhorushko.crudgenerictest.repository.MeetingRepository;
import by.nhorushko.crudgenerictest.repository.RegionRepository;
import by.nhorushko.crudgenerictest.service.MeetingPageableService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingPageIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private MeetingPageableService service;

    private RegionEntity minsk;
    private RegionEntity vitebsk;

    @BeforeEach
    void seed() {
        minsk = regionRepository.save(new RegionEntity(101L, "minsk"));
        vitebsk = regionRepository.save(new RegionEntity(102L, "vitebsk"));
        meetingRepository.save(MeetingEntity.builder()
                .title("sprint planning").status(MeetingStatus.PLANNED)
                .startTime(Instant.parse("2026-07-01T10:00:00Z"))
                .day(LocalDate.parse("2026-07-01")).region(minsk).build());
        meetingRepository.save(MeetingEntity.builder()
                .title("retro").status(MeetingStatus.DONE)
                .startTime(Instant.parse("2026-07-05T10:00:00Z"))
                .day(LocalDate.parse("2026-07-05")).region(minsk).build());
        meetingRepository.save(MeetingEntity.builder()
                .title("planning poker").status(MeetingStatus.CANCELED)
                .startTime(Instant.parse("2026-07-10T10:00:00Z"))
                .day(LocalDate.parse("2026-07-10")).region(vitebsk).build());
    }

    @AfterEach
    void cleanUp() {
        meetingRepository.deleteAll();
        regionRepository.deleteAll();
    }

    @Test
    void filtersByStringContains() throws Exception {
        mockMvc.perform(get("/meeting/page").param("titleFilter", "like#planning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void filtersByEnumEqualAndIn() throws Exception {
        mockMvc.perform(get("/meeting/page").param("statusFilter", "eq#DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("retro"));

        mockMvc.perform(get("/meeting/page").param("statusFilter", "in#PLANNED,DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void filtersByInstantBetween() throws Exception {
        mockMvc.perform(get("/meeting/page")
                        .param("startTimeFilter", "btn#2026-07-01T00:00:00Z,2026-07-06T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void filtersByLocalDateEqual() throws Exception {
        mockMvc.perform(get("/meeting/page").param("dayFilter", "eq#2026-07-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void filtersByNestedRegionId() throws Exception {
        mockMvc.perform(get("/meeting/page").param("regionIdFilter", "eq#101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void combinesFiltersWithAnd() throws Exception {
        mockMvc.perform(get("/meeting/page")
                        .param("titleFilter", "like#planning")
                        .param("regionIdFilter", "eq#101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("sprint planning"));
    }

    @Test
    void sortsByMappedAndPlainProperties() throws Exception {
        mockMvc.perform(get("/meeting/page").param("sort", "asc#startTime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("sprint planning"));

        mockMvc.perform(get("/meeting/page").param("sort", "desc#startTime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("planning poker"));
    }

    @Test
    void disallowedOperationIsBadRequest() throws Exception {
        mockMvc.perform(get("/meeting/page").param("titleFilter", "eq#retro"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unconvertibleValueIsBadRequest() throws Exception {
        mockMvc.perform(get("/meeting/page").param("regionIdFilter", "eq#abc"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/meeting/page").param("statusFilter", "eq#NO_SUCH"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void legacySortSyntaxIsBadRequest() throws Exception {
        mockMvc.perform(get("/meeting/page").param("sort", "-startTime"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownFilterFieldIsRejectedAtServiceLevel() {
        assertThatThrownBy(() -> service.page(PageFilterRequest.pageRequestAnd(0, 20, "desc#id",
                new PageFilterRequest.Filter("nope", "eq#1"))))
                .isInstanceOf(FilterValidationException.class);
    }
}
```

- [ ] **Step 2: Run the IT**

Run: `mvn -f test-application/pom.xml test "-Dtest=MeetingPageIT"`
Expected: `Tests run: 11, Failures: 0, Errors: 0`. If a test fails, fix the library/fixture (Tasks 1–5), not the assertion — each assertion encodes spec behaviour.

- [ ] **Step 3: Run the full test-application suite (surefire skips `*IT` by default — include them explicitly)**

Run: `mvn -f test-application/pom.xml test "-Dtest=*,*IT"`
Expected: BUILD SUCCESS, ~50+ tests, 0 failures.

- [ ] **Step 4: Commit**

```powershell
git add test-application/src/test/java/by/nhorushko/crudgenerictest/pageable/MeetingPageIT.java
git commit -m "test(test-application): full pageable IT matrix for FilterFields"
```

---

### Task 7: README migration notes

**Files:**
- Modify: `README.md` (section "Migration to 5.0 (flex-only)", lines ~126–166)

**Interfaces:**
- Consumes: final API from Tasks 1–4.
- Produces: documentation only.

- [ ] **Step 1: Update the moved-classes table**

Remove the row (AbsFilterSpecification no longer exists in 5.0):

```
| `by.nhorushko.crudgeneric.v2.pageable.AbsFilterSpecification` | `by.nhorushko.crudgeneric.flex.pageable.AbsFilterSpecification` |
```

- [ ] **Step 2: Update the removed-classes table**

Replace the `AbsPagingAndSortingService` row's replacement text with:

```
| `v2.pageable.AbsPagingAndSortingService` (deprecated), `v2.pageable.AbsFilterSpecification` | `AbsFlexPagingAndSortingService` + one `filterFields(builder)` declaration (see “Pageable in 5.0” below) |
```

And in the `SpecificationUtils` row replace `or use AbsFilterSpecification + PageFilterRequest for filter-driven paging` with `or use AbsFlexPagingAndSortingService + PageFilterRequest for filter-driven paging`.

- [ ] **Step 3: Add a "Pageable in 5.0" subsection after the tables**

````markdown
### Pageable in 5.0 — declarative FilterFields

`AbsFilterSpecification` (path/type map) and the per-service `buildSpecification` switch are
replaced by a single declaration. Per entity: delete the `FilterSpecification*` class, fold its
map and the switch into `filterFields(builder)`, drop the `toDto` override if the default
mapper-based one suffices:

```java
@Service
public class RtRoutePageableService
        extends AbsFlexPagingAndSortingService<Long, RtRoute, RtRouteEntity> {

    public RtRoutePageableService(RtRouteRepository repository, AbsModelMapper mapper) {
        super(repository, mapper, RtRoute.class);
    }

    @Override
    protected FilterFields<RtRouteEntity> filterFields(FilterFields.Builder<RtRouteEntity> f) {
        return f.string("name", CONTAINS)
                .string("description", CONTAINS)
                .ofLong("userId", "user.id", EQUAL)
                .instant("archivedAt", GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN, LESSTHAN_OR_EQUAL_TO, BETWEEN, IS_NULL, NOT_NULL)
                .instant("createdTime", GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN, LESSTHAN_OR_EQUAL_TO, BETWEEN)
                .ofBoolean("oneOff", EQUAL)
                .build();
    }
}
```

- Operation validation is always on: unknown field, disallowed operation, unconvertible value
  or malformed sort raise `FilterValidationException` — map it to HTTP 400 in your exception
  handler. Controller-side `isAvailableOperation`/`checkFilterOperation` calls and the ops
  `Set` constants are deleted.
- Typed builder methods (`string`, `ofLong`, `ofInteger`, `ofDouble`, `ofFloat`, `ofBoolean`,
  `instant`, `ofLocalDate`, `ofLocalDateTime`, `ofEnum`) carry built-in converters. Enum and
  date entries no longer need `map.put(X.class, X::valueOf)` in your `Converters` subclass —
  delete the subclass if only `field()`-free declarations remain. `field(name, path, type, ops)`
  is the only method that reads `Converters` (pass it via the 4-arg service constructor).
- Custom one-off specs move to `.custom(name, filter -> Specification)`.
- **Sort syntax narrowed**: `asc#field`, `desc#field` or bare `field` (ascending). The legacy
  `+field`/`-field` prefixes are rejected with `FilterValidationException` (in URLs `+` decodes
  to a space). `BasePageRequest`'s default sort changed from `-id` to `desc#id`.
````

- [ ] **Step 4: Verify and commit**

Run: `git grep -n "AbsFilterSpecification" README.md`
Expected: no hits.

```powershell
git add README.md
git commit -m "docs: README migration notes for FilterFields pageable in 5.0"
```

---

## Final verification (after all tasks)

- [ ] `mvn -f library/pom.xml clean test` → BUILD SUCCESS
- [ ] `mvn -f library/pom.xml install "-DskipTests"` → BUILD SUCCESS
- [ ] `mvn -f test-application/pom.xml clean test "-Dtest=*,*IT"` → BUILD SUCCESS, 0 failures
- [ ] `git grep -rn "AbsFilterSpecification" library/src test-application/src README.md` → no hits
