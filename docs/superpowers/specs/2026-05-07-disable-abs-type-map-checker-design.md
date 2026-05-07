# Отключаемый AbsTypeMapChecker через customizer-бин

## Контекст

`AbsTypeMapChecker` — `SmartLifecycle`-бин из библиотеки `crud-generic`, который при старте Spring-приложения проверяет, что для всех сервисов, наследующих `AbsFlexServiceR`, в `ModelMapper` зарегистрированы необходимые `TypeMap`. Если маппинг отсутствует — приложение падает.

Бин регистрируется безусловно в `AbsGenericCrudConfiguration#crudAbstractGenericMappingChecker`. Сейчас приложение, использующее библиотеку, не может отключить эту проверку.

## Цель

Дать приложению, подключающему библиотеку, возможность отключить проверку маппингов через Java-конфигурацию (без property-флагов и без параметров аннотации).

## Решение

Customizer-бин: библиотека публикует класс `AbsCrudCustomizer` с настройкой `typeMapCheckerEnabled` (default `true`). Если приложение регистрирует свой `@Bean AbsCrudCustomizer` — библиотека учитывает его настройки. Если нет — поведение прежнее (проверка включена).

### Компоненты

**Новый класс `AbsCrudCustomizer`** (`library/src/main/java/by/nhorushko/crudgeneric/flex/config/AbsCrudCustomizer.java`)

POJO с lombok `@Builder` и `@Getter`. Одно поле:

```java
@Builder.Default
private final boolean typeMapCheckerEnabled = true;
```

**Изменения в `AbsTypeMapChecker`**

- Добавить поле `private final boolean enabled`.
- Заменить `@RequiredArgsConstructor` на два явных конструктора:
  - `(services, modelMapper)` → делегирует к `(services, modelMapper, true)` для обратной совместимости.
  - `(services, modelMapper, enabled)` → основной.
- В `start()`: вызывать `checkMappers()` только если `enabled == true`. Поле `isRunning = true` ставится в любом случае.

**Изменения в `AbsGenericCrudConfiguration`**

В метод `crudAbstractGenericMappingChecker` добавить параметр `ObjectProvider<AbsCrudCustomizer> customizerProvider`. Получить customizer:

```java
AbsCrudCustomizer customizer = customizerProvider.getIfAvailable(
    () -> AbsCrudCustomizer.builder().build());
```

Передать `customizer.isTypeMapCheckerEnabled()` в конструктор `AbsTypeMapChecker`.

### Использование в приложении

```java
@Configuration
public class CrudConfig {
    @Bean
    public AbsCrudCustomizer absCrudCustomizer() {
        return AbsCrudCustomizer.builder()
            .typeMapCheckerEnabled(false)
            .build();
    }
}
```

## Обоснование выбранных решений

**Почему бин всегда создаётся, а не отключается условно?** Условная регистрация (`@ConditionalOnBean`/`@Conditional`) на основе значения поля другого бина в Spring проблематична — условия резолвятся до создания бинов. Создавать всегда, но делать no-op в `start()` — проще и предсказуемее. Стоимость — пустой `SmartLifecycle` в контексте, что незначительно.

**Почему `ObjectProvider`, а не `@Autowired(required=false)`?** `ObjectProvider` — современный идиоматичный Spring-способ для опциональных зависимостей с поддержкой ленивого резолва и фабрики дефолта.

**Почему два конструктора в `AbsTypeMapChecker`?** Сохранение старого `(services, modelMapper)` гарантирует, что любой код, явно создающий checker (тесты, кастомные конфигурации в приложениях), продолжит работать без изменений.

## Что НЕ входит в скоуп

- Изменения в `@EnableAbsGenericCrud` (никаких новых параметров аннотации).
- Property-флаги (`crud-generic.type-map-checker.enabled` и подобные).
- Возможность кастомизации других аспектов библиотеки через `AbsCrudCustomizer` — поле одно, под текущую задачу. Расширение — по потребности.
- Документация `README` библиотеки (если потребуется — отдельная задача).

## Тестирование

В `test-application` добавить интеграционный тест(ы), проверяющие:

1. Без кастомного `AbsCrudCustomizer` бина — checker запускается и проверяет маппинги (поведение по умолчанию).
2. С `AbsCrudCustomizer.builder().typeMapCheckerEnabled(false).build()` — checker не падает даже при намеренно отсутствующем маппинге.

## Совместимость

- Старый публичный API (`new AbsTypeMapChecker(services, modelMapper)`) сохранён.
- Дефолтное поведение неизменно: при отсутствии customizer-бина проверка работает как раньше.
- Приложения, не подключающие customizer, не затрагиваются.
