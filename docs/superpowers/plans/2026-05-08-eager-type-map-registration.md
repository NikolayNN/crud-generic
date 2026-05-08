# Eager TypeMap Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make crud-generic mapper beans (`AbsMapperBase`/`AbsMapperDto`/`AbsMapperEntityDto`/`AbsMapBasic`/`AbstractMapper`/`RegisterableMapper`) eagerly initialized under `spring.main.lazy-initialization=true`, so their constructor-side TypeMap and Converter registrations land in the shared `ModelMapper` before any consumer (including tests) calls `modelMapper.map(...)`.

**Architecture:** Library publishes a new `BeanDefinitionRegistryPostProcessor` (`AbsMapperEagerInitPostProcessor`) registered via a `static @Bean` method in `AbsGenericCrudConfiguration`. The processor reads `AbsCrudCustomizer.eagerTypeMapRegistration` (default `true`) and, when enabled, walks all `BeanDefinition`s and sets `lazyInit=false` for those whose resolved type is a mapper-class. In eager-init contexts the call is a no-op.

**Tech Stack:** Java 11 (library) / 17 (test-application), Spring Framework 5.2.x (library) / Spring Boot 2.5.3 (test-application), Lombok 1.18, ModelMapper 2.3.9, JUnit 4 + Mockito 3.12 (library tests), JUnit 5 + spring-boot-starter-test (test-application integration tests).

**Spec:** `docs/superpowers/specs/2026-05-08-eager-type-map-registration-design.md`

---

## File Map

**Create (production):**
- `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsMapperEagerInitPostProcessor.java` — new BDRPP that flips `lazyInit=false` for mapper-class bean definitions.

**Modify (production):**
- `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java` — add `eagerTypeMapRegistration` field (default `true`).
- `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfiguration.java` — register `AbsMapperEagerInitPostProcessor` as a `static @Bean`.

**Create (library unit tests, JUnit 4 + Mockito):**
- `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizerEagerFlagTest.java` — verify default and explicit values for the new flag.
- `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsMapperEagerInitPostProcessorTest.java` — verify BDRPP flips `lazyInit` for mapper types, leaves non-mappers alone, and respects the flag.

**Create (integration tests in test-application, JUnit 5 + Spring Boot):**
- `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/MockAImmutableDto.java` — Lombok `@Value` DTO without no-arg constructor; reproduces the immutable-DTO scenario.
- `test-application/src/main/java/by/nhorushko/crudgenerictest/mockmapper/MockAImmutableMapper.java` — `AbsMapperDto<MockAEntity, MockAImmutableDto>` registered as `@Service`.
- `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationLazyInitDefaultTest.java` — golden path: `lazy-initialization=true`, default customizer, immutable mapping works.
- `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationDisabledTest.java` — opt-out: `lazy-initialization=true` + `eagerTypeMapRegistration(false)`, mapper bean definition remains lazy.
- `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationEagerContextTest.java` — regression: standard eager context still works.

---

## Task 1: Add `eagerTypeMapRegistration` flag to `AbsCrudCustomizer`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizerEagerFlagTest.java`

- [ ] **Step 1: Write the failing test**

Create `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizerEagerFlagTest.java`:

```java
package by.nhorushko.crudgeneric.flex.config;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbsCrudCustomizerEagerFlagTest {

    @Test
    public void builder_defaultEagerTypeMapRegistration_isTrue() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder().build();
        assertTrue(customizer.isEagerTypeMapRegistration());
    }

    @Test
    public void builder_eagerTypeMapRegistrationFalse_returnsFalse() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .eagerTypeMapRegistration(false)
                .build();
        assertFalse(customizer.isEagerTypeMapRegistration());
    }

    @Test
    public void builder_typeMapCheckerStaysIndependent() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .eagerTypeMapRegistration(false)
                .build();
        assertTrue(customizer.isTypeMapCheckerEnabled());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl library test -Dtest=AbsCrudCustomizerEagerFlagTest`

Expected: FAIL — compilation error, method `eagerTypeMapRegistration(boolean)` does not exist on the builder, and `isEagerTypeMapRegistration()` does not exist on `AbsCrudCustomizer`.

- [ ] **Step 3: Add the field to `AbsCrudCustomizer`**

Open `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java`. Replace the entire file with:

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
 *
 * <p>Example: opt out of eager mapper-bean initialization under
 * {@code spring.main.lazy-initialization=true}:</p>
 * <pre>
 * &#64;Bean
 * public AbsCrudCustomizer absCrudCustomizer() {
 *     return AbsCrudCustomizer.builder()
 *             .eagerTypeMapRegistration(false)
 *             .build();
 * }
 * </pre>
 *
 * <p>The {@code eagerTypeMapRegistration} flag controls whether crud-generic
 * forces the following bean classes to be eagerly initialized regardless of
 * the global lazy-init setting:</p>
 * <ul>
 *   <li>{@code by.nhorushko.crudgeneric.v2.mapper.AbsMapperBase} and subclasses</li>
 *   <li>{@code by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBasic} and subclasses</li>
 *   <li>{@code by.nhorushko.crudgeneric.mapper.AbstractMapper} (deprecated) and subclasses</li>
 *   <li>{@code by.nhorushko.crudgeneric.flex.mapper.core.RegisterableMapper} implementations</li>
 * </ul>
 * <p>These beans register {@code TypeMap} / {@code Converter} entries in the shared
 * {@code ModelMapper} from their constructors, so they must be instantiated before
 * any consumer calls {@code modelMapper.map(...)}.</p>
 */
@Getter
@Builder
public class AbsCrudCustomizer {

    @Builder.Default
    private final boolean typeMapCheckerEnabled = true;

    @Builder.Default
    private final boolean eagerTypeMapRegistration = true;
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl library test -Dtest=AbsCrudCustomizerEagerFlagTest`

Expected: PASS — three tests green.

- [ ] **Step 5: Run the full library test suite to confirm no regressions**

Run: `mvn -pl library test`

Expected: PASS — `AbsCrudCustomizerTest`, `AbsTypeMapCheckerTest`, `AbsGenericCrudConfigurationTest`, `AbsCrudCustomizerEagerFlagTest`, and any pre-existing tests stay green.

- [ ] **Step 6: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizerEagerFlagTest.java
git commit -m "$(cat <<'EOF'
feat: add eagerTypeMapRegistration flag to AbsCrudCustomizer

Default true. Will be consumed by an upcoming
BeanDefinitionRegistryPostProcessor that flips mapper bean definitions
back to eager init when the consumer app runs with
spring.main.lazy-initialization=true.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Implement `AbsMapperEagerInitPostProcessor`

**Files:**
- Create: `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsMapperEagerInitPostProcessor.java`
- Test: `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsMapperEagerInitPostProcessorTest.java`

- [ ] **Step 1: Write the failing test**

Create `library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsMapperEagerInitPostProcessorTest.java`:

```java
package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBasic;
import by.nhorushko.crudgeneric.flex.mapper.core.RegisterableMapper;
import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperBase;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbsMapperEagerInitPostProcessorTest {

    @Test
    public void postProcessBeanFactory_flagOn_setsLazyInitFalseForMapperSubtype() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition mapperBd = new RootBeanDefinition(StubMapperBaseBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("stubMapper", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("stubMapper").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOn_alsoFlipsAbsMapBasicSubtypes() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition mapperBd = new RootBeanDefinition(StubAbsMapBasicBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("absMapBasicBean", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("absMapBasicBean").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOn_alsoFlipsAbstractMapperSubtypes() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition mapperBd = new RootBeanDefinition(StubAbstractMapperBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("legacyMapper", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("legacyMapper").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOn_alsoFlipsRegisterableMapperImplementors() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition mapperBd = new RootBeanDefinition(StubRegisterableBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("registerable", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("registerable").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOn_leavesNonMapperLazyAlone() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition unrelatedBd = new RootBeanDefinition(StubUnrelatedBean.class);
        unrelatedBd.setLazyInit(true);
        bf.registerBeanDefinition("unrelated", unrelatedBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertTrue(bf.getBeanDefinition("unrelated").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOff_leavesMapperLazyAlone() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(false);

        BeanDefinition mapperBd = new RootBeanDefinition(StubMapperBaseBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("stubMapper", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertTrue(bf.getBeanDefinition("stubMapper").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_noCustomizerBean_defaultsToFlagOn() {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

        BeanDefinition mapperBd = new RootBeanDefinition(StubMapperBaseBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("stubMapper", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("stubMapper").isLazyInit());
    }

    private static DefaultListableBeanFactory newFactoryWithCustomizer(boolean eager) {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .eagerTypeMapRegistration(eager)
                .build();
        // Register the customizer as a singleton so getBeanProvider().getIfAvailable() finds it.
        bf.registerSingleton("absCrudCustomizer", customizer);
        return bf;
    }

    // --- stubs --- //

    public static class StubMapperBaseBean extends AbsMapperBase<Object, Object> {
        public StubMapperBaseBean() {
            super(null, Object.class, Object.class);
        }
    }

    public static class StubAbsMapBasicBean extends AbsMapBasic<Object, Object> {
        public StubAbsMapBasicBean() {
            super(null, Object.class, Object.class);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class StubAbstractMapperBean extends AbstractMapper {
        public StubAbstractMapperBean() {
            super(by.nhorushko.crudgeneric.domain.AbstractEntity.class,
                  by.nhorushko.crudgeneric.domain.AbstractDto.class,
                  null);
        }
    }

    public static class StubRegisterableBean implements RegisterableMapper {
        @Override
        public void register() { /* no-op */ }
    }

    public static class StubUnrelatedBean { }
}
```

Notes about the stubs:
- `StubMapperBaseBean` and `StubAbsMapBasicBean` pass `null` ModelMapper because we never instantiate the bean — the BDRPP only inspects bean *definitions*, not instances, so the constructor never runs in this test.
- `StubAbstractMapperBean` is similar; raw types are used because `AbstractMapper` is `@Deprecated` and uses raw type parameters in its existing constructors.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -pl library test -Dtest=AbsMapperEagerInitPostProcessorTest`

Expected: FAIL — compilation error, `AbsMapperEagerInitPostProcessor` does not exist.

- [ ] **Step 3: Implement `AbsMapperEagerInitPostProcessor`**

Create `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsMapperEagerInitPostProcessor.java`:

```java
package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBasic;
import by.nhorushko.crudgeneric.flex.mapper.core.RegisterableMapper;
import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperBase;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * Forces eager initialization of crud-generic mapper beans when the consumer
 * application runs with {@code spring.main.lazy-initialization=true}.
 * <p>
 * crud-generic mapper beans (subclasses of {@link AbsMapperBase}, {@link AbsMapBasic},
 * the deprecated {@link AbstractMapper}, and any {@link RegisterableMapper})
 * register {@code TypeMap} and {@code Converter} entries in the shared
 * {@link org.modelmapper.ModelMapper} from their constructors. Under global
 * lazy-init, these beans are not instantiated until somebody injects them by
 * type, which means a direct {@code modelMapper.map(entity, ImmutableDto.class)}
 * call from unrelated code (e.g. event listeners or audit trail) finds no
 * registered converter and falls back to reflective instantiation of the
 * destination type. For Lombok {@code @Value} DTOs without a no-arg constructor
 * this surfaces as a {@code NoSuchMethodException}.
 * </p>
 * <p>
 * This processor walks the {@link BeanDefinitionRegistry} and clears
 * {@code lazyInit} on every bean whose resolved type is assignable to one of the
 * known mapper roots. Behavior is gated by
 * {@link AbsCrudCustomizer#isEagerTypeMapRegistration()} (default {@code true}).
 * In eager-init contexts the {@code setLazyInit(false)} call is a no-op.
 * </p>
 */
public class AbsMapperEagerInitPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // No-op: we only need a hook in postProcessBeanFactory(), where we have the
        // ConfigurableListableBeanFactory and can resolve bean types via getType().
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        AbsCrudCustomizer customizer = beanFactory
                .getBeanProvider(AbsCrudCustomizer.class)
                .getIfAvailable(() -> AbsCrudCustomizer.builder().build());
        if (!customizer.isEagerTypeMapRegistration()) {
            return;
        }
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Class<?> beanType = resolveBeanType(beanFactory, beanName);
            if (beanType != null && isMapperType(beanType)) {
                beanFactory.getBeanDefinition(beanName).setLazyInit(false);
            }
        }
    }

    private static Class<?> resolveBeanType(ConfigurableListableBeanFactory bf, String beanName) {
        try {
            return bf.getType(beanName, false);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    private static boolean isMapperType(Class<?> clazz) {
        return AbsMapperBase.class.isAssignableFrom(clazz)
            || AbsMapBasic.class.isAssignableFrom(clazz)
            || AbstractMapper.class.isAssignableFrom(clazz)
            || RegisterableMapper.class.isAssignableFrom(clazz);
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -pl library test -Dtest=AbsMapperEagerInitPostProcessorTest`

Expected: PASS — all seven tests green.

- [ ] **Step 5: Run the full library test suite**

Run: `mvn -pl library test`

Expected: PASS — every existing and new test green.

- [ ] **Step 6: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsMapperEagerInitPostProcessor.java library/src/test/java/by/nhorushko/crudgeneric/flex/config/AbsMapperEagerInitPostProcessorTest.java
git commit -m "$(cat <<'EOF'
feat: add AbsMapperEagerInitPostProcessor

BeanDefinitionRegistryPostProcessor that flips lazyInit=false on every
bean whose type is assignable to AbsMapperBase, AbsMapBasic,
AbstractMapper, or RegisterableMapper, gated by
AbsCrudCustomizer.eagerTypeMapRegistration. Not yet wired in
AbsGenericCrudConfiguration.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Register the post-processor in `AbsGenericCrudConfiguration`

**Files:**
- Modify: `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfiguration.java`

This task wires the new processor into the published configuration. There is no dedicated unit test — the wiring is exercised end-to-end by the integration tests added in Task 5.

- [ ] **Step 1: Add the `static @Bean` factory method**

Open `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfiguration.java`. Append a new method **inside** the class, after `crudAbstractGenericMappingChecker`:

```java
    /**
     * Registers {@link AbsMapperEagerInitPostProcessor} so that crud-generic
     * mapper beans are eagerly initialized regardless of
     * {@code spring.main.lazy-initialization=true}.
     * <p>
     * Declared {@code static} because {@link
     * org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor}
     * beans must be created before the enclosing {@code @Configuration} class
     * is fully processed; a non-static factory method would emit a Spring
     * warning and prevent {@code @Bean} processing of this configuration.
     * </p>
     *
     * @return the registered post-processor.
     */
    @Bean
    public static AbsMapperEagerInitPostProcessor absMapperEagerInitPostProcessor() {
        return new AbsMapperEagerInitPostProcessor();
    }
```

The class header (`@Configuration`, package, existing imports) is unchanged. No new imports required because `AbsMapperEagerInitPostProcessor` lives in the same package.

- [ ] **Step 2: Run the full library test suite**

Run: `mvn -pl library test`

Expected: PASS — existing tests stay green; no new tests added in this task.

- [ ] **Step 3: Run the test-application build to confirm wiring compiles and the existing context starts**

Run: `mvn -pl test-application -am test`

Expected: BUILD SUCCESS — both modules build, all existing test-application tests still pass. This proves the new bean does not break the eager-init path.

- [ ] **Step 4: Commit**

```bash
git add library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsGenericCrudConfiguration.java
git commit -m "$(cat <<'EOF'
feat: register AbsMapperEagerInitPostProcessor in AbsGenericCrudConfiguration

Wires the BDRPP via a static @Bean so consumer apps using
spring.main.lazy-initialization=true automatically get eager
mapper-bean initialization (and therefore working ModelMapper
TypeMap/Converter registration).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Add immutable DTO + mapper fixtures in `test-application`

**Why we need them:** the existing `MockADto` is annotated with `@NoArgsConstructor`, so ModelMapper falls back to reflective instantiation under lazy-init even when no Converter is registered — the bug never surfaces with that DTO. To get an end-to-end test that fails *before* this fix and passes *after* it, we add a Lombok `@Value` DTO with no no-arg constructor, plus an `AbsMapperDto`-based mapper that registers a Converter for it.

**Files:**
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/MockAImmutableDto.java`
- Create: `test-application/src/main/java/by/nhorushko/crudgenerictest/mockmapper/MockAImmutableMapper.java`

- [ ] **Step 1: Create the immutable DTO**

Create `test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/MockAImmutableDto.java`:

```java
package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import lombok.Value;

/**
 * Immutable, Lombok-{@link Value} DTO without a no-arg constructor.
 * Used to verify that crud-generic registers ModelMapper Converters before
 * any consumer calls {@code modelMapper.map(...)} — including under
 * {@code spring.main.lazy-initialization=true}.
 */
@Value
public class MockAImmutableDto implements AbsBaseDto {
    Long id;
    String name;
}
```

- [ ] **Step 2: Create the mapper bean**

Create `test-application/src/main/java/by/nhorushko/crudgenerictest/mockmapper/MockAImmutableMapper.java`:

```java
package by.nhorushko.crudgenerictest.mockmapper;

import by.nhorushko.crudgeneric.v2.mapper.AbsMapperDto;
import by.nhorushko.crudgenerictest.domain.dto.MockAImmutableDto;
import by.nhorushko.crudgenerictest.domain.entity.MockAEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class MockAImmutableMapper extends AbsMapperDto<MockAEntity, MockAImmutableDto> {

    public MockAImmutableMapper(ModelMapper modelMapper) {
        super(modelMapper, MockAEntity.class, MockAImmutableDto.class);
    }

    @Override
    protected MockAImmutableDto create(MockAEntity entity) {
        return new MockAImmutableDto(entity.getId(), entity.getName());
    }
}
```

- [ ] **Step 3: Build to verify both files compile**

Run: `mvn -pl test-application -am compile`

Expected: BUILD SUCCESS.

- [ ] **Step 4: Run the existing test-application test suite**

Run: `mvn -pl test-application -am test`

Expected: PASS — existing tests still green. The new mapper bean joins the context but doesn't break anything because the existing tests do not use lazy-init.

- [ ] **Step 5: Commit**

```bash
git add test-application/src/main/java/by/nhorushko/crudgenerictest/domain/dto/MockAImmutableDto.java test-application/src/main/java/by/nhorushko/crudgenerictest/mockmapper/MockAImmutableMapper.java
git commit -m "$(cat <<'EOF'
test: add immutable DTO + AbsMapperDto fixture

MockAImmutableDto is a Lombok @Value class without no-arg constructor;
MockAImmutableMapper registers a ModelMapper Converter for it via
AbsMapperDto. Used by upcoming integration tests for the eager
TypeMap registration fix.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Integration test — golden path under `lazy-initialization=true`

**Files:**
- Test: `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationLazyInitDefaultTest.java`

This is the headline test: under `spring.main.lazy-initialization=true` with the default customizer, mapping `MockAEntity → MockAImmutableDto` succeeds because the new BDRPP forced the mapper bean to be eager and its constructor registered the Converter.

- [ ] **Step 1: Write the test**

Create `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationLazyInitDefaultTest.java`:

```java
package by.nhorushko.crudgenerictest.eagerinit;

import by.nhorushko.crudgenerictest.domain.dto.MockAImmutableDto;
import by.nhorushko.crudgenerictest.domain.entity.MockAEntity;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class EagerTypeMapRegistrationLazyInitDefaultTest {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    void modelMapperMap_immutableDto_succeedsUnderLazyInitWithDefaults() {
        MockAEntity entity = new MockAEntity(42L, "alice", "desc");

        MockAImmutableDto dto = modelMapper.map(entity, MockAImmutableDto.class);

        assertEquals(Long.valueOf(42L), dto.getId());
        assertEquals("alice", dto.getName());
    }

    @Test
    void mockAImmutableMapper_beanDefinitionIsEager() {
        // The post-processor should have flipped this mapper's BeanDefinition out of lazy.
        boolean lazy = applicationContext.getBeanFactory()
                .getBeanDefinition("mockAImmutableMapper")
                .isLazyInit();
        assertFalse(lazy, "mockAImmutableMapper should be eager because eagerTypeMapRegistration defaults to true");
    }
}
```

Notes:
- We do not register a custom `AbsCrudCustomizer` bean, so the default (`eagerTypeMapRegistration=true`, `typeMapCheckerEnabled=true`) applies.
- The first test will fail without the fix because under lazy-init nothing creates `MockAImmutableMapper`, no Converter is registered, and ModelMapper tries to instantiate `MockAImmutableDto` reflectively — but Lombok `@Value` produces a class with no no-arg constructor, so reflection throws and the `map()` call surfaces a `MappingException`.
- The second test directly asserts the BDRPP's effect on the bean definition, providing a precise contract check independent of ModelMapper internals.

- [ ] **Step 2: Run the test**

Run: `mvn -pl test-application -am test -Dtest=EagerTypeMapRegistrationLazyInitDefaultTest`

Expected: PASS — both tests green. (If they fail, the fix is incomplete — re-check Tasks 1–3.)

- [ ] **Step 3: Confirm the test would have caught the bug — sanity check**

Temporarily flip `AbsCrudCustomizer.eagerTypeMapRegistration` default to `false` in `library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java` (change `@Builder.Default private final boolean eagerTypeMapRegistration = true;` → `false`) and rerun:

Run: `mvn -pl test-application -am test -Dtest=EagerTypeMapRegistrationLazyInitDefaultTest`

Expected: FAIL — both `modelMapperMap_immutableDto_succeedsUnderLazyInitWithDefaults` (with `org.modelmapper.MappingException` caused by `NoSuchMethodException: <init>()`) and `mockAImmutableMapper_beanDefinitionIsEager` (lazy assertion fails).

Then **revert** the change:

```bash
git checkout -- library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java
```

Run again to confirm green:

Run: `mvn -pl test-application -am test -Dtest=EagerTypeMapRegistrationLazyInitDefaultTest`

Expected: PASS.

This sanity check is intentionally manual — it proves the test is *real*. Do not commit the temporary flip.

- [ ] **Step 4: Commit the new test only**

```bash
git status   # confirm only the new test file is staged/changed
git add test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationLazyInitDefaultTest.java
git commit -m "$(cat <<'EOF'
test: verify eager TypeMap registration under lazy-init

Asserts that with default AbsCrudCustomizer and
spring.main.lazy-initialization=true, modelMapper.map(entity,
ImmutableDto.class) works because the BDRPP made the mapper bean
eager and the Converter is registered.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Integration test — opt-out behavior

**Files:**
- Test: `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationDisabledTest.java`

When the consumer registers `AbsCrudCustomizer.builder().eagerTypeMapRegistration(false).build()`, the BDRPP must **not** touch mapper bean definitions — they remain lazy. We also disable the type-map checker so the context can start with mappings missing.

- [ ] **Step 1: Write the test**

Create `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationDisabledTest.java`:

```java
package by.nhorushko.crudgenerictest.eagerinit;

import by.nhorushko.crudgeneric.flex.config.AbsCrudCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class EagerTypeMapRegistrationDisabledTest {

    @TestConfiguration
    static class Config {
        @Bean
        AbsCrudCustomizer absCrudCustomizer() {
            return AbsCrudCustomizer.builder()
                    .eagerTypeMapRegistration(false)
                    .typeMapCheckerEnabled(false)
                    .build();
        }
    }

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    void mockAImmutableMapper_beanDefinitionRemainsLazy() {
        boolean lazy = applicationContext.getBeanFactory()
                .getBeanDefinition("mockAImmutableMapper")
                .isLazyInit();
        assertTrue(lazy, "mockAImmutableMapper should stay lazy when eagerTypeMapRegistration is false");
    }
}
```

Notes:
- `typeMapCheckerEnabled(false)` is required because under `lazy-initialization=true` with eager flag off, the checker would observe missing TypeMaps at startup and throw — that's the previously-fixed behavior, not what we are testing here.
- We assert on the bean definition, not on `modelMapper.map(...)`, because the *contract* of the opt-out is "BDRPP leaves bean definitions alone". The downstream behavior (mapping fails) is the original bug; we don't need to redemonstrate it.

- [ ] **Step 2: Run the test**

Run: `mvn -pl test-application -am test -Dtest=EagerTypeMapRegistrationDisabledTest`

Expected: PASS — `mockAImmutableMapper` stays lazy because the BDRPP returned early.

- [ ] **Step 3: Commit**

```bash
git add test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationDisabledTest.java
git commit -m "$(cat <<'EOF'
test: verify eagerTypeMapRegistration opt-out keeps mappers lazy

When AbsCrudCustomizer disables the eager flag, the BDRPP must not
touch mapper bean definitions. Asserts the contract via
BeanDefinition.isLazyInit() on the test mapper.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Regression test — BDRPP is harmless in eager context

**Files:**
- Test: `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationEagerContextTest.java`

Without `lazy-initialization=true` everything should keep working: mapper beans are already eager, `setLazyInit(false)` on them is a no-op, the type-map checker still passes, and immutable mapping still works.

- [ ] **Step 1: Write the test**

Create `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationEagerContextTest.java`:

```java
package by.nhorushko.crudgenerictest.eagerinit;

import by.nhorushko.crudgenerictest.domain.dto.MockAImmutableDto;
import by.nhorushko.crudgenerictest.domain.entity.MockAEntity;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class EagerTypeMapRegistrationEagerContextTest {

    @Autowired
    private ModelMapper modelMapper;

    @Test
    void modelMapperMap_immutableDto_succeedsInEagerContext() {
        MockAEntity entity = new MockAEntity(7L, "bob", "desc");

        MockAImmutableDto dto = modelMapper.map(entity, MockAImmutableDto.class);

        assertEquals(Long.valueOf(7L), dto.getId());
        assertEquals("bob", dto.getName());
    }
}
```

Notes: no `lazy-initialization` property is set, no `AbsCrudCustomizer` override is registered. This is the baseline configuration; the test fails only if the new code somehow breaks the existing path.

- [ ] **Step 2: Run the test**

Run: `mvn -pl test-application -am test -Dtest=EagerTypeMapRegistrationEagerContextTest`

Expected: PASS.

- [ ] **Step 3: Run the full project test suite**

Run: `mvn test`

Expected: BUILD SUCCESS — every test in both `library` and `test-application` modules passes.

- [ ] **Step 4: Commit**

```bash
git add test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/EagerTypeMapRegistrationEagerContextTest.java
git commit -m "$(cat <<'EOF'
test: regression check that BDRPP is harmless in eager contexts

Plain @SpringBootTest (no lazy-init, no customizer override): immutable
DTO mapping must keep working. Guards against accidentally breaking
the eager-init path.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Verification Checklist

After every task is complete:

- [ ] `mvn -pl library test` — all library unit tests pass.
- [ ] `mvn test` from project root — both modules build, all unit + integration tests pass.
- [ ] Spec `docs/superpowers/specs/2026-05-08-eager-type-map-registration-design.md` requirements covered:
  - `AbsCrudCustomizer.eagerTypeMapRegistration` field with default `true`. ✓ Task 1.
  - `AbsMapperEagerInitPostProcessor` BDRPP that resolves bean types via `getType(beanName, false)` and flips `lazyInit=false` for `AbsMapperBase` / `AbsMapBasic` / `AbstractMapper` / `RegisterableMapper` subtypes. ✓ Task 2.
  - Customizer flag gates the post-processor. ✓ Task 2 (unit) + Task 6 (integration).
  - Registered as `static @Bean` in `AbsGenericCrudConfiguration`. ✓ Task 3.
  - Golden-path test with `spring.main.lazy-initialization=true` and immutable DTO mapping. ✓ Task 5.
  - Opt-out test with `eagerTypeMapRegistration(false)`. ✓ Task 6.
  - Regression test under eager context. ✓ Task 7.
  - No new `library/pom.xml` dependencies. ✓ all tasks.

## Out of Scope (per spec)

- Refactoring `AbsMapperDto.configureMapper()` to extract `create()` into a strategy object (breaking API change).
- Property-based flag (`crud-generic.eager-type-map-registration=...`).
- Adding Spring Boot as a `library/pom.xml` dependency for `LazyInitializationExcludeFilter`.
- README / user-facing documentation updates.
