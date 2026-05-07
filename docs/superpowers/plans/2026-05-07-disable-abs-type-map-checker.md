# Disable AbsTypeMapChecker via Customizer Bean — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow applications using the `crud-generic` library to disable the startup-time `AbsTypeMapChecker` validation by registering a customizer bean.

**Architecture:** Introduce `AbsCrudCustomizer` POJO (lombok `@Builder`) with field `typeMapCheckerEnabled` (default `true`). `AbsGenericCrudConfiguration` injects this customizer optionally via `ObjectProvider`. `AbsTypeMapChecker` keeps its `SmartLifecycle` registration but gates `checkMappers()` on the `enabled` flag. Backward compatible: existing `(services, modelMapper)` constructor preserved.

**Tech Stack:** Java 11, Spring Framework 5.2.x, Lombok 1.18, JUnit 4, Mockito 3 (already in `library/pom.xml`).

**Spec:** `docs/superpowers/specs/2026-05-07-disable-abs-type-map-checker-design.md`

---

## File Map

- **Create:** `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java` — public POJO customizer with builder.
- **Create:** `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizerTest.java` — verify builder default.
- **Modify:** `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsTypeMapChecker.java` — add `enabled` flag, two constructors, gate `checkMappers()` in `start()`.
- **Create:** `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsTypeMapCheckerTest.java` — verify enabled/disabled behavior of `start()`.
- **Modify:** `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfiguration.java` — inject `ObjectProvider<AbsCrudCustomizer>`, pass `enabled` flag.
- **Create:** `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfigurationTest.java` — verify wiring uses customizer when present and defaults when absent.

---

### Task 1: Create `AbsCrudCustomizer`

**Files:**
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizerTest.java`

- [ ] **Step 1: Write the failing test**

Create `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizerTest.java`:

```java
package by.nhorushko.crudgeneric.flex.config;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbsCrudCustomizerTest {

    @Test
    public void builder_defaultTypeMapCheckerEnabled_isTrue() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder().build();
        assertTrue(customizer.isTypeMapCheckerEnabled());
    }

    @Test
    public void builder_typeMapCheckerEnabledFalse_returnsFalse() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .typeMapCheckerEnabled(false)
                .build();
        assertFalse(customizer.isTypeMapCheckerEnabled());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl library test -Dtest=AbsCrudCustomizerTest`
Expected: FAIL — compilation error, `AbsCrudCustomizer` does not exist.

- [ ] **Step 3: Implement `AbsCrudCustomizer`**

Create `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java`:

```java
package by.nhorushko.crudgeneric.flex.config;

import lombok.Builder;
import lombok.Getter;

/**
 * Customizes the runtime behavior of the Generic CRUD framework.
 * <p>
 * Register a single {@link AbsCrudCustomizer} bean in the application's Spring context
 * to override the framework defaults. When no bean is provided, the framework uses
 * defaults equivalent to {@code AbsCrudCustomizer.builder().build()}.
 * </p>
 *
 * <p>Example: disable startup-time ModelMapper validation:</p>
 * <pre>
 * &#64;Bean
 * public AbsCrudCustomizer absCrudCustomizer() {
 *     return AbsCrudCustomizer.builder()
 *             .typeMapCheckerEnabled(false)
 *             .build();
 * }
 * </pre>
 */
@Getter
@Builder
public class AbsCrudCustomizer {

    @Builder.Default
    private final boolean typeMapCheckerEnabled = true;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl library test -Dtest=AbsCrudCustomizerTest`
Expected: PASS — both tests green.

- [ ] **Step 5: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizerTest.java
git commit -m "feat: add AbsCrudCustomizer for framework configuration"
```

---

### Task 2: Add `enabled` flag to `AbsTypeMapChecker`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsTypeMapChecker.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsTypeMapCheckerTest.java`

- [ ] **Step 1: Write failing tests**

Create `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsTypeMapCheckerTest.java`:

```java
package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR;
import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbsTypeMapCheckerTest {

    private ModelMapper modelMapper;
    private AbsFlexServiceR<?, ?, ?, ?> serviceWithMissingMapping;

    @Before
    public void setUp() {
        modelMapper = mock(ModelMapper.class);
        // No type maps configured -> getTypeMap returns null for any pair.
        when(modelMapper.getTypeMap(Object.class, String.class)).thenReturn(null);

        serviceWithMissingMapping = mock(AbsFlexServiceR.class);
        when((Class) serviceWithMissingMapping.getEntityClass()).thenReturn(Object.class);
        when((Class) serviceWithMissingMapping.getReadDtoClass()).thenReturn(String.class);
    }

    @Test
    public void start_enabledTrue_throwsWhenMappingMissing() {
        AbsTypeMapChecker checker = new AbsTypeMapChecker(
                List.of(serviceWithMissingMapping), modelMapper, true);
        try {
            checker.start();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void start_enabledFalse_doesNotThrowWhenMappingMissing() {
        AbsTypeMapChecker checker = new AbsTypeMapChecker(
                List.of(serviceWithMissingMapping), modelMapper, false);
        checker.start();
        assertTrue(checker.isRunning());
    }

    @Test
    public void start_enabledFalse_emptyServices_isRunning() {
        AbsTypeMapChecker checker = new AbsTypeMapChecker(
                Collections.emptyList(), modelMapper, false);
        checker.start();
        assertTrue(checker.isRunning());
    }

    @Test
    public void legacyConstructor_defaultsToEnabled() {
        AbsTypeMapChecker checker = new AbsTypeMapChecker(
                List.of(serviceWithMissingMapping), modelMapper);
        try {
            checker.start();
            fail("Expected legacy constructor to default to enabled=true and throw");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl library test -Dtest=AbsTypeMapCheckerTest`
Expected: FAIL — compilation error, three-arg constructor does not exist.

- [ ] **Step 3: Modify `AbsTypeMapChecker`**

Replace the entire body of `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsTypeMapChecker.java` with:

```java
package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceExtCRUD;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceRUD;
import org.modelmapper.ModelMapper;
import org.springframework.context.SmartLifecycle;

import java.util.Collection;

/**
 * A utility class that verifies the existence and correctness of ModelMapper type mappings
 * for all services extending {@link AbsFlexServiceR} and its subclasses at application startup.
 * <p>
 * This class implements {@link SmartLifecycle}, allowing it to perform type mapping checks
 * during the startup phase of the application. It ensures that all required DTO to entity
 * mappings (and vice versa) are correctly configured in ModelMapper for the services used in
 * the application, throwing an exception if any required mapping is missing. This proactive
 * check helps in identifying configuration issues early in the development lifecycle.
 * </p>
 * <p>
 * The check can be disabled by registering an {@link AbsCrudCustomizer} bean with
 * {@code typeMapCheckerEnabled = false}. When disabled, this lifecycle bean still starts
 * but performs no validation.
 * </p>
 */
public class AbsTypeMapChecker implements SmartLifecycle {

    public static final int PHASE_NUMBER = Integer.MAX_VALUE;

    private final Collection<? extends AbsFlexServiceR<?, ?, ?, ?>> services;
    private final ModelMapper modelMapper;
    private final boolean enabled;

    private boolean isRunning = false;

    public AbsTypeMapChecker(Collection<? extends AbsFlexServiceR<?, ?, ?, ?>> services,
                             ModelMapper modelMapper) {
        this(services, modelMapper, true);
    }

    public AbsTypeMapChecker(Collection<? extends AbsFlexServiceR<?, ?, ?, ?>> services,
                             ModelMapper modelMapper,
                             boolean enabled) {
        this.services = services;
        this.modelMapper = modelMapper;
        this.enabled = enabled;
    }

    @Override
    public void start() {
        if (enabled) {
            checkMappers();
        }
        isRunning = true;
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public int getPhase() {
        return PHASE_NUMBER;
    }

    public void checkMappers() {
        for (AbsFlexServiceR<?, ?, ?, ?> service : services) {
            Class<?> entityClass = service.getEntityClass();
            Class<?> readDtoClass = service.getReadDtoClass();
            checkTypeMap(entityClass, readDtoClass);
            checkTypeMap(readDtoClass, entityClass);
            if (service instanceof AbsFlexServiceRUD) {
                Class<?> updateDtoClass = ((AbsFlexServiceRUD<?, ?, ?, ?, ?>) service).getUpdateDtoClass();
                checkTypeMap(updateDtoClass, entityClass);
            }
            if (service instanceof AbsFlexServiceCRUD) {
                Class<?> createDtoClass = ((AbsFlexServiceCRUD<?, ?, ?, ?, ?, ?>) service).getCreateDtoClass();
                checkTypeMap(createDtoClass, entityClass);
            }
            if (service instanceof AbsFlexServiceExtCRUD) {
                Class<?> createDtoClass = ((AbsFlexServiceExtCRUD<?, ?, ?, ?, ?, ?, ?, ?>) service).getCreateDtoClass();
                checkTypeMap(createDtoClass, entityClass);
            }
        }
    }

    protected void checkTypeMap(Class<?> sourceType, Class<?> destinationType) {
        var typeMap = modelMapper.getTypeMap(sourceType, destinationType);
        if (typeMap == null) {
            throw new UnsupportedOperationException(String.format("TypeMap for mapping %s -> %s is not exists", sourceType.getSimpleName(), destinationType.getSimpleName()));
        }
    }
}
```

Key changes vs. original:
- Removed `@RequiredArgsConstructor` import and annotation; added two explicit constructors.
- Added `private final boolean enabled` field.
- `start()` only calls `checkMappers()` when `enabled` is `true`.
- Updated class javadoc to mention the customizer-based disable mechanism.
- All other behavior (including `checkMappers()`, `checkTypeMap()`, `getPhase()`, `stop()`, `isRunning()`) is unchanged.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl library test -Dtest=AbsTypeMapCheckerTest`
Expected: PASS — all four tests green.

- [ ] **Step 5: Run full library test suite to confirm no regressions**

Run: `mvn -pl library test`
Expected: PASS — all existing tests continue to pass.

- [ ] **Step 6: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsTypeMapChecker.java library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsTypeMapCheckerTest.java
git commit -m "feat: support disabling AbsTypeMapChecker via constructor flag"
```

---

### Task 3: Wire customizer in `AbsGenericCrudConfiguration`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfiguration.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfigurationTest.java`

- [ ] **Step 1: Write failing tests**

Create `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfigurationTest.java`:

```java
package by.nhorushko.crudgeneric.flex.config;

import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbsGenericCrudConfigurationTest {

    private AbsGenericCrudConfiguration configuration;
    private ModelMapper modelMapper;

    @Before
    public void setUp() {
        configuration = new AbsGenericCrudConfiguration();
        modelMapper = mock(ModelMapper.class);
    }

    @Test
    public void crudAbstractGenericMappingChecker_noCustomizer_defaultsToEnabled() {
        ObjectProvider<AbsCrudCustomizer> provider = emptyProvider();

        AbsTypeMapChecker checker = configuration.crudAbstractGenericMappingChecker(
                Collections.emptyList(), modelMapper, provider);

        // start() with no services should not throw and should set running.
        checker.start();
        assertTrue(checker.isRunning());
    }

    @Test
    public void crudAbstractGenericMappingChecker_customizerDisabled_skipsValidation() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .typeMapCheckerEnabled(false)
                .build();
        ObjectProvider<AbsCrudCustomizer> provider = providerOf(customizer);

        // Build a service that would fail validation if the checker ran.
        var failingService = mock(by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR.class);
        when((Class) failingService.getEntityClass()).thenReturn(Object.class);
        when((Class) failingService.getReadDtoClass()).thenReturn(String.class);
        when(modelMapper.getTypeMap(any(), any())).thenReturn(null);

        AbsTypeMapChecker checker = configuration.crudAbstractGenericMappingChecker(
                Collections.singletonList(failingService), modelMapper, provider);

        checker.start();
        assertTrue(checker.isRunning());
    }

    @Test
    public void crudAbstractGenericMappingChecker_customizerEnabled_runsValidation() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .typeMapCheckerEnabled(true)
                .build();
        ObjectProvider<AbsCrudCustomizer> provider = providerOf(customizer);

        var failingService = mock(by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR.class);
        when((Class) failingService.getEntityClass()).thenReturn(Object.class);
        when((Class) failingService.getReadDtoClass()).thenReturn(String.class);
        when(modelMapper.getTypeMap(any(), any())).thenReturn(null);

        AbsTypeMapChecker checker = configuration.crudAbstractGenericMappingChecker(
                Collections.singletonList(failingService), modelMapper, provider);

        try {
            checker.start();
            fail("Expected UnsupportedOperationException because validation should run");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AbsCrudCustomizer> emptyProvider() {
        ObjectProvider<AbsCrudCustomizer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenAnswer(inv -> {
            java.util.function.Supplier<AbsCrudCustomizer> supplier = inv.getArgument(0);
            return supplier.get();
        });
        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AbsCrudCustomizer> providerOf(AbsCrudCustomizer customizer) {
        ObjectProvider<AbsCrudCustomizer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenReturn(customizer);
        return provider;
    }
}
```

Note: this codebase uses Mockito 3.12, which keeps the deprecated `Matchers.any()` (used elsewhere in `library/src/test`). The `InvocationOnMock#getArgument(int)` API is the Mockito 3.x equivalent of the older `getArgumentAt`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl library test -Dtest=AbsGenericCrudConfigurationTest`
Expected: FAIL — compilation error, `crudAbstractGenericMappingChecker` does not have an `ObjectProvider` parameter.

- [ ] **Step 3: Modify `AbsGenericCrudConfiguration`**

Open `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfiguration.java`. Add this import near the existing imports:

```java
import org.springframework.beans.factory.ObjectProvider;
```

Replace the entire `crudAbstractGenericMappingChecker` method (and its javadoc) with:

```java
    /**
     * Creates and configures an {@link AbsTypeMapChecker} bean to verify the correctness of ModelMapper type mappings.
     * <p>
     * Behavior is controlled by an optional {@link AbsCrudCustomizer} bean: if the application registers one,
     * the {@code typeMapCheckerEnabled} flag from that customizer determines whether validation runs. If no
     * customizer bean is registered, the default ({@code typeMapCheckerEnabled = true}) is used.
     * </p>
     * <p>
     * The {@link AbsTypeMapChecker} bean is always registered to preserve {@link SmartLifecycle} ordering.
     * When the customizer disables validation, the bean's {@code start()} method becomes a no-op.
     * </p>
     *
     * @param services            services subject to mapping validation.
     * @param modelMapper         the application's {@link ModelMapper} instance.
     * @param customizerProvider  optional provider for {@link AbsCrudCustomizer}.
     * @return the registered {@link AbsTypeMapChecker} bean.
     */
    @Bean
    public AbsTypeMapChecker crudAbstractGenericMappingChecker(
            List<? extends AbsFlexServiceR<?, ?, ?, ?>> services,
            ModelMapper modelMapper,
            ObjectProvider<AbsCrudCustomizer> customizerProvider) {
        AbsCrudCustomizer customizer = customizerProvider.getIfAvailable(
                () -> AbsCrudCustomizer.builder().build());
        return new AbsTypeMapChecker(services, modelMapper, customizer.isTypeMapCheckerEnabled());
    }
```

Leave the `modelMapper()` and `absModelMapper(...)` beans untouched.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl library test -Dtest=AbsGenericCrudConfigurationTest`
Expected: PASS — all three tests green.

- [ ] **Step 5: Run full library test suite**

Run: `mvn -pl library test`
Expected: PASS — all existing and new tests green.

- [ ] **Step 6: Build the whole project to verify nothing downstream breaks**

Run: `mvn -DskipTests=false test`
Expected: BUILD SUCCESS — both `library` and `test-application` build and test cleanly.

- [ ] **Step 7: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfiguration.java library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfigurationTest.java
git commit -m "feat: honor AbsCrudCustomizer in AbsGenericCrudConfiguration"
```

---

## Verification Checklist

After all tasks complete:

- [ ] `mvn -pl library test` — all library unit tests pass.
- [ ] `mvn test` from project root — both modules build and pass tests.
- [ ] Spec file `docs/superpowers/specs/2026-05-07-disable-abs-type-map-checker-design.md` requirements covered:
  - `AbsCrudCustomizer` exists with `typeMapCheckerEnabled` field and builder. ✓ Task 1.
  - `AbsTypeMapChecker` has both legacy and new constructors; legacy keeps validation enabled. ✓ Task 2.
  - `AbsGenericCrudConfiguration` consumes optional customizer via `ObjectProvider` and passes flag through. ✓ Task 3.
  - When validation is disabled, missing mappings do not cause startup failure. ✓ Task 2 + Task 3 tests.

## Out of Scope (per spec)

- No changes to `EnableAbsGenericCrud` annotation.
- No property-based configuration.
- Customizer has only `typeMapCheckerEnabled` — no other fields.
- README / user-facing documentation updates not included.
