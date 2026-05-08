# Жадная регистрация TypeMap'ов под `spring.main.lazy-initialization=true`

## Контекст

Маппер-бины crud-generic (`AbsMapperBase` / `AbsMapperDto` / `AbsMapperEntityDto`, а также legacy `AbstractMapper` и `AbsMapBasic`/`RegisterableMapper`) регистрируют `TypeMap` и кастомный `Converter` в общем `ModelMapper` **в собственном конструкторе**:

```java
// AbsMapperDto
private void configureMapper() {
    modelMapper.createTypeMap(entityClass, dtoClass)
        .setCondition(...)
        .setConverter(context -> create(context.getSource())); // лямбда захватывает this
}
```

Конвертер ссылается на `create(ENTITY)` — абстрактный метод, реализуемый пользовательским сабклассом. Без инстанса конкретного бина зарегистрировать его нельзя.

При `spring.main.lazy-initialization=true` эти бины не создаются, пока кто-то не запросит их по типу. Тестовый код, инжектящий общий `ModelMapper` напрямую (например, через `audit-trail` / `event-listener`-цепочки) и вызывающий `modelMapper.map(entity, ImmutableValueDto.class)`, не запускает создание маппер-бина → конвертер не зарегистрирован → ModelMapper фолбекается на дефолтную инстанциацию `Logistic$Order` → `NoSuchMethodException: <init>()` для immutable Lombok `@Value`-DTO.

В 13.3.7 уже добавлен флаг `AbsCrudCustomizer.typeMapCheckerEnabled(false)` — он закрывает падение `AbsTypeMapChecker` на старте, но не лечит сценарий «общий ModelMapper использован раньше создания маппер-бина».

## Цель

Под `spring.main.lazy-initialization=true` маппер-бины crud-generic должны создаваться до фазы реальной работы приложения / тестов — чтобы их конструкторы успели зарегистрировать TypeMap'ы и конвертеры в общем `ModelMapper`. Бизнес-сервисы и репозитории остаются ленивыми.

API должно быть опт-аутом: дефолт «жадно» (под lazy-init починено), при необходимости отключается через `AbsCrudCustomizer`.

## Решение

`BeanDefinitionRegistryPostProcessor` (далее BDRPP), регистрируемый библиотекой, обходит все `BeanDefinition` контекста и для тех, чей класс присваиваем к одному из маппер-типов, выставляет `bd.setLazyInit(false)`.

Под глобальным `spring.main.lazy-initialization=true` Spring Boot поднимает свой `LazyInitializationBeanFactoryPostProcessor` (приоритет `Ordered.HIGHEST_PRECEDENCE`), он в `postProcessBeanFactory` помечает все `BeanDefinition` как ленивые. Наш BDRPP с дефолтным приоритетом отрабатывает позже и снимает `lazyInit` обратно для маппер-бинов. Те создаются в общей singleton-фазе, их конструкторы регистрируют converter'ы, дальше всё работает.

В eager-режиме (без `lazy-initialization=true`) `setLazyInit(false)` — no-op, `BeanDefinition`-ы и без того eager. Поведение не меняется.

### Почему не `LazyInitializationExcludeFilter`

Это идиоматичный механизм Spring Boot, но `library/pom.xml` сейчас не зависит от Spring Boot — только от Spring Framework, `spring-data-jpa` 2.3.1 и `spring-web` 5.2.6. Добавлять `spring-boot` как `provided`-зависимость ради одного фильтра — лишний кросс-граничный долг. `BeanDefinitionRegistryPostProcessor` живёт в Spring Framework и даёт идентичный эффект.

### Почему не «чистый (A)» с регистрацией converter'ов из BFPP

Converter в `AbsMapperDto.configureMapper()` — лямбда `context -> create(context.getSource())`. `create(ENTITY)` — абстрактный метод; реализация в пользовательском сабклассе. Чтобы зарегистрировать converter без создания бина, надо вынести метод в отдельный объект-стратегию. Это breaking change в публичном API библиотеки и требует миграции пользователей. Вне скоупа этой задачи.

## Компоненты

### 1. `AbsCrudCustomizer` — новое поле

`library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java`

```java
@Builder.Default
private final boolean eagerTypeMapRegistration = true;
```

Существующее поле `typeMapCheckerEnabled` не трогаем. Javadoc дополняем описанием нового поля и тем, какие классы попадают под «жадность».

### 2. `AbsMapperEagerInitPostProcessor` — новый класс

`library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsMapperEagerInitPostProcessor.java`

```java
public class AbsMapperEagerInitPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) { /* no-op */ }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
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

Замечания по реализации:
- `beanFactory.getType(beanName, false)` — ключевой выбор. Он умеет резолвить тип бина и для классов из `BeanDefinition.getBeanClassName()`, и для `@Bean`-методов (через `factoryMethodName`/`factoryBeanName`), не инстанцируя ни сам бин, ни factory-bean (`allowFactoryBeanInit=false`).
- `getBeanProvider(...).getIfAvailable(default)` создаёт `AbsCrudCustomizer` синхронно во время BFPP-фазы. Это безопасно: customizer — POJO без зависимостей, без побочных эффектов в конструкторе.
- На `NoSuchBeanDefinitionException` или `null` — пропускаем `BeanDefinition`; никакой регистрации `lazyInit=false` для такого бина не делаем.

### 3. `AbsGenericCrudConfiguration` — регистрация BDRPP

```java
@Bean
public static AbsMapperEagerInitPostProcessor absMapperEagerInitPostProcessor() {
    return new AbsMapperEagerInitPostProcessor();
}
```

`static` обязательно: `BeanDefinitionRegistryPostProcessor`-бины создаются раньше регулярных `@Configuration`-классов; нестатический `@Bean` BDRPP вызывает предупреждение Spring и может потерять `@Bean`-обработку самой конфигурации.

## Поток данных

1. Spring запускает `BeanDefinitionRegistryPostProcessor`-фазу: `ConfigurationClassPostProcessor` сканирует `@Configuration`, регистрирует все `BeanDefinition`-ы (включая `AbsCrudCustomizer`-фабрику и наш BDRPP).
2. Spring Boot's `LazyInitializationBeanFactoryPostProcessor` (если активирован `spring.main.lazy-initialization=true`, `Ordered.HIGHEST_PRECEDENCE`) проходит по `BeanDefinition`-ам и ставит `lazyInit=true` всем подряд.
3. Наш `AbsMapperEagerInitPostProcessor.postProcessBeanFactory` (приоритет дефолтный → запускается позже Boot's BFPP) вытаскивает `AbsCrudCustomizer`, проверяет флаг, для маппер-типов снимает `lazyInit`.
4. Spring переходит к фазе создания singleton-ов: маппер-бины создаются eager, их конструкторы вызывают `modelMapper.createTypeMap(...).setConverter(...)`. Бизнес-сервисы — лениво.
5. `AbsTypeMapChecker.start()` (если включён) — все TypeMap'ы зарегистрированы, проверка проходит.
6. Тесты вызывают `modelMapper.map(entity, ImmutableDto.class)` → конвертер найден → маппинг успешен.

## Обработка ошибок и крайних случаев

- **Нет `AbsCrudCustomizer`-бина у пользователя.** `getIfAvailable(default)` возвращает дефолт с `eagerTypeMapRegistration=true`. Поведение: жадная регистрация работает.
- **Пользователь явно `eagerTypeMapRegistration(false)`.** BDRPP делает `return;` сразу после проверки флага — никакие `BeanDefinition`-ы не трогаются. Эквивалентно поведению до правки.
- **`spring.main.lazy-initialization=true` отсутствует.** `BeanDefinition`-ы и без BDRPP eager; `setLazyInit(false)` на eager-определении — идемпотентный no-op.
- **Класс маппер-бина не резолвится** (например, AspectJ-ткань или прокси-бины с `null` className). `getType(beanName, false)` вернёт `null`; пропускаем.
- **Маппер-бины зарегистрированы как `@Lazy`-бины самим пользователем.** BDRPP всё равно перепишет `lazyInit=false`. Это ожидаемое поведение, ради которого функция и нужна. Документируем явно: «жадность маппер-бинов имеет приоритет; если вам действительно нужно держать конкретный маппер ленивым — отключите `eagerTypeMapRegistration` глобально и управляйте сами».
- **Несколько `AbsCrudCustomizer`-бинов в контексте.** `getBeanProvider.getIfAvailable` бросит `NoUniqueBeanDefinitionException` при ambiguous match. Это согласовано с поведением существующего кода в `AbsGenericCrudConfiguration#crudAbstractGenericMappingChecker`, который тоже использует `ObjectProvider.getIfAvailable`. Не правим.

## Что НЕ входит в скоуп

- Рефакторинг `AbsMapperDto.configureMapper()`, чтобы вынести `create()` в отдельный класс-стратегию (breaking change, отдельная задача).
- Property-флаг `crud-generic.eager-type-map-registration=...` — пока решаем через customizer, чтобы оставаться единообразно с `typeMapCheckerEnabled`.
- Поддержка Spring Boot 1.x / Spring Framework < 5.x. BDRPP существует с 3.0.1, `lazyInit` на BeanDefinition — с 1.x. Минимально допустимые версии не меняются.
- Документация README библиотеки. Если потребуется — отдельная задача.

## Тестирование

В `test-application/src/test/java/by/nhorushko/crudgenerictest/eagerinit/`:

1. **`EagerTypeMapRegistrationLazyInitDefaultTest`** — `@SpringBootTest(properties = "spring.main.lazy-initialization=true")`, без кастомного `AbsCrudCustomizer`. Инжектим только `ModelMapper` (не маппер-бины). Вызываем `modelMapper.map(mockAEntity, MockADto.class)` — успех. Это золотой путь, ради которого всё затевается.

2. **`EagerTypeMapRegistrationDisabledTest`** — `@SpringBootTest(properties = "spring.main.lazy-initialization=true")` + `AbsCrudCustomizer.builder().eagerTypeMapRegistration(false).typeMapCheckerEnabled(false).build()`. Инжектим `ModelMapper`. Документирующий тест: проверяем, что когда жадность отключена, маппер-бины не создаются автоматически (например, через `applicationContext.containsBean("mockAMapper") == true`, но `applicationContext.getBeanDefinition("mockAMapper").isLazyInit() == true`). `typeMapCheckerEnabled(false)` обязателен — иначе чекер упадёт на старте. Тест фиксирует, что выключатель действительно выключает.

3. **`EagerTypeMapRegistrationEagerContextTest`** — обычный `@SpringBootTest` без lazy-init, дефолтный customizer. Проверяем, что маппинг работает, чекер не падает. Регрессионный тест: убеждаемся, что BDRPP не ломает обычный сценарий.

Тесты в `test-application`, потому что:
- `library/src/test` использует JUnit 4 + Mockito без Spring-контекста — для проверки eager/lazy-flow нужен полноценный Boot-контекст.
- `test-application` уже содержит Boot 2.5.3 + h2 + JPA-mocks; туда логично класть интеграцию.

Прогон: `mvn -pl test-application test`.

## Совместимость

- Старый публичный API `AbsCrudCustomizer.builder().typeMapCheckerEnabled(false).build()` работает как раньше; новое поле — опциональное с дефолтом `true`.
- Существующие приложения, не использующие `lazy-initialization=true`, не замечают изменения: BDRPP проходит, всем маппер-бинам ставит `lazyInit=false`, что и так было.
- Существующие приложения, использующие `lazy-initialization=true` и не подключающие customizer, **получают новое поведение по умолчанию** — маппер-бины становятся жадными. Это и есть фикс. Если кто-то намеренно полагался на ленивость маппер-бинов — может опт-аутнуться через `eagerTypeMapRegistration(false)`.
- Никаких новых обязательных зависимостей в `library/pom.xml`.
