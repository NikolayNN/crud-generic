# Явные мапперы вместо ModelMapper — дизайн

**Дата:** 2026-09-24
**Статус:** утверждён

## Контекст

Каждое преобразование DTO ↔ entity в flex-стеке идёт через ModelMapper. По-настоящему явным
сегодня является только одно направление: entity → read DTO, где `AbsMapEntityToDto` ставит
конвертер, вызывающий `create(entity)` потребителя. Всё остальное — неявное сопоставление полей,
с которым владелец постоянно борется:

- **create DTO → entity** и **update DTO → entity** копируют поля по имени (STANDARD matching,
  field matching включён, `skipNull`), после чего вызывается хук `mapSpecificFields`. Что именно
  скопируется, зависит от имён акцессоров, аннотаций Lombok и правил сопоставления ModelMapper,
  а не от кода, который написал потребитель.
- **read DTO → entity** регистрируется каждым `AbsFlexMapConfigDefault` только ради
  `AbsTypeMapChecker` и `updatePartial`. `updatePartial` не вызывает никто: ни контроллеры
  библиотеки, ни LocatorServer, ни bi-dvr. В комментариях мапперов LocatorServer этот маппинг
  описан как опасный на 14.x: неявное копирование положило бы `owner` глубоко поверх managed
  `UserEntity` и затёрло `areaM2`.
- **self-map entity → entity** регистрируется «для клонирования» и никем не используется.
- **вложенные дети** (например, `OrderCreateDto.lines` → `OrderEntity.lines`) каскадятся неявно
  через те TypeMap, которые случайно оказались зарегистрированы; `nullifyZeroId` для детей живёт
  в пост-конвертере, потому что это единственный хук, который ModelMapper вызывает для вложенных
  объектов.
- Регистрация — побочный эффект конструктора (`AbsMapBasic` зовёт `createTypeMap` в
  конструкторе), из-за чего и существует `AbsMapperEagerInitPostProcessor`: он принудительно
  создаёт все бины-мапперы, чтобы их TypeMap были зарегистрированы до первого `map()`.
- `AbsMapperExtRelation` находит поле связи рефлексией (первое поле типа `EXT`).

Потребители: LocatorServer (17 `AbsFlexMapConfigDefault`, 13 `AbsMapperExtRelation`, 11 v2-мапперов,
138 прямых вызовов `mapper.map(x, Y.class)`) и bi-dvr (4 маппера). Оба сидят на `13.3.15-jakarta`
и на 14.x не переезжали, поэтому перейдут на эту версию одним проходом и с неявным маппингом
на 14.x бороться не будут вовсе.

## Решения, принятые с владельцем

1. **Жёсткий переход.** ModelMapper удаляется из библиотеки. Никакого fallback: пара, которой нет
   в реестре, — ошибка на старте (чекер) или при вызове.
2. **`updatePartial` и маппинг read DTO → entity удаляются** вместе с `FieldCopyUtil`
   (копирование полей рефлексией, использовалось только там). PATCH обслуживают `patch(dto)`
   (через реестр) и `changeEntity(id, change)` (через замыкание), оба явные.
3. **`AbsMapperExtRelation.setRelation` становится абстрактным**; рефлексивный поиск поля удаляется.
4. **Форма API:** два интерфейса `Mapper` / `Updater`, `MapperRegistry` и один
   `AbsFlexMapConfig` на сущность с тремя абстрактными методами. Для CRUD-тройки потребитель
   интерфейсы напрямую не реализует; view-DTO и детей закрывают базовые классы с одним
   абстрактным методом.
5. **Фасад переименован** `AbsModelMapper` → `AbsMapper`. Сигнатуры методов сохраняются, поэтому
   138 прямых вызовов в LocatorServer не меняются, меняется только тип параметра конструктора.
6. **Версия 15.0.** Цикл 14.1 пуст (`## Не выпущено` пустой), поэтому первый коммит
   переименовывает цикл: `chore: start 15.0`.

## Целевой API

Все типы мапперов живут в `by.nhorushko.crudgeneric.flex.mapper`. Подпакеты `mapper.core`,
`mapper.composite` и `mapper.mapper` исчезают.

### Базовые интерфейсы

```java
public interface Mapper<FROM, TO> {
    Class<FROM> fromClass();
    Class<TO> toClass();
    TO map(FROM from);

    static <F, T> Mapper<F, T> of(Class<F> from, Class<T> to, Function<F, T> fn);
}

public interface Updater<FROM, TO> {
    Class<FROM> fromClass();
    Class<TO> toClass();
    void update(FROM from, TO into);

    static <F, T> Updater<F, T> of(Class<F> from, Class<T> to, BiConsumer<F, T> fn);
}

/** Бин, отдающий несколько мапперов разом (его реализует AbsFlexMapConfig). */
public interface MapperSource {
    Collection<Mapper<?, ?>> mappers();
    Collection<Updater<?, ?>> updaters();
}
```

Классы пары объявляются явно (`fromClass()` / `toClass()`), ровно так же, как сегодняшние базовые
классы принимают их в конструкторе. Вывод generic-параметров через `ResolvableType` намеренно не
используется: это ещё один вид магии, и для лямбд он не работает.

### `MapperRegistry`

- Собирается один раз из всех бинов `Mapper`, `Updater` и `MapperSource`; `MapperSource`
  разворачиваются. После сборки неизменяем, кроме кэша поиска.
- Две карты с ключом `(fromClass, toClass)`. Повторная регистрация пары ломает сборку
  `IllegalStateException` с указанием пары и обеих реализаций.
- **Поиск** `mapper(Class<?> from, Class<T> to)`: сначала точный ключ, затем `from.getSuperclass()`
  вплоть до `Object`. Интерфейсы не рассматриваются (класс, реализующий два зарегистрированных
  интерфейса, был бы двусмысленным). Именно так находятся Hibernate-прокси и наследники DTO.
- **Поиск** `updater(Class<?> from, Class<?> to)`: идёт по цепочке источника, как выше, и
  дополнительно по цепочке назначения, потому что назначение — существующий экземпляр, который
  может оказаться Hibernate-прокси (`getReference`).
- Найденные пары кэшируются по `(runtime from, runtime to)` в `ConcurrentHashMap`.
- Промах бросает `MappingNotFoundException` (`flex.exception`, наследует `RuntimeException`)
  с направлением и обоими полными именами классов, например
  `No Updater registered for com.x.OrderUpdateDto -> com.x.OrderEntity`.
- Варианты `find*` возвращают `Optional` для чекера.

### Фасад `AbsMapper` (заменяет `AbsModelMapper`)

```java
public class AbsMapper {
    public AbsMapper(MapperRegistry registry, EntityManager entityManager);           // тесты
    public AbsMapper(Supplier<MapperRegistry> registry, EntityManager entityManager); // Spring, лениво

    public <T> T map(Object source, Class<T> destinationType);   // null source → null
    public <T> T map(Object source, T destination);              // null source → destination без изменений; через Updater
    public <T> List<T> mapAll(Collection<?> source, Class<T> destinationType); // null → null
    public <T extends AbstractEntity<?>> T reference(AbstractDto<?> dto, Class<T> entityClass);
    public <T extends AbstractEntity<?>> T referenceById(Object id, Class<T> entityClass);
    public EntityManager getEntityManager();
}
```

`getModelMapper()` исчезает. Реестр получается из supplier при первом обращении и запоминается.
Именно это рвёт цикл бинов *бин-маппер → AbsMapper → MapperRegistry → все бины-мапперы*:
`AbsMapper` не трогает реестр при создании, поэтому мапперы продолжают инжектить `AbsMapper`
в конструктор ровно так, как сегодня инжектят `AbsModelMapper`.

### Spring-связка (`flex.config`)

`AbsGenericCrudConfiguration` (импортируется через `@EnableAbsGenericCrud`, аннотация не меняется):

| Бин | Зависит от | Примечание |
|---|---|---|
| `MapperRegistry` | `List<Mapper<?,?>>`, `List<Updater<?,?>>`, `List<MapperSource>` | Spring создаёт все бины-мапперы, чтобы удовлетворить инъекцию, lazy-init или нет. |
| `AbsMapper` | `ObjectProvider<MapperRegistry>`, `EntityManager` | `new AbsMapper(provider::getObject, em)`. |
| `AbsMappingChecker` | `List<? extends AbsFlexServiceR>`, `MapperRegistry`, `AbsCrudCustomizer` | `SmartLifecycle`, фаза `Integer.MAX_VALUE`, заменяет `AbsTypeMapChecker`. |

`AbsMappingChecker.start()` проверяет по каждому сервису: `Mapper<ENTITY, READ_DTO>`; для
`AbsFlexServiceRUD` ещё `Updater<UPDATE_DTO, ENTITY>`; для `AbsFlexServiceCRUD` и
`AbsFlexServiceExtCRUD` ещё `Mapper<CREATE_DTO, ENTITY>`. Собирает все недостающие пары и падает
один раз `IllegalStateException` с их списком (сегодня останавливается на первой). `isRunning`
выставляется только после успешной проверки, как сейчас.

`AbsMapperEagerInitPostProcessor` удаляется. Почему он больше не нужен: регистрация перестала быть
побочным эффектом конструктора. Реестр вытягивает все бины-мапперы через инъекцию в конструктор.
Бины `SmartLifecycle` создаются при refresh даже при `spring.main.lazy-initialization=true`,
поэтому чекер собирает реестр, а тот создаёт все мапперы до `start()`. При выключенном чекере
реестр собирается при первом `map()` и всё равно видит все определения бинов.

В `AbsCrudCustomizer` остаётся один флаг, `mappingCheckerEnabled` (по умолчанию `true`).
`typeMapCheckerEnabled` и `eagerTypeMapRegistration` удаляются.

### Базовые классы для потребителей

```java
public abstract class AbsFlexMapConfig<CREATE_DTO extends AbsBaseDto,
                                       UPDATE_DTO extends AbstractDto<?>,
                                       READ_DTO extends AbstractDto<?>,
                                       ENTITY extends AbstractEntity<?>> implements MapperSource {
    protected final AbsMapper mapper;
    public AbsFlexMapConfig(AbsMapper mapper, Class<CREATE_DTO> createDtoClass, Class<UPDATE_DTO> updateDtoClass,
                            Class<READ_DTO> readDtoClass, Class<ENTITY> entityClass);

    protected abstract ENTITY toEntity(CREATE_DTO dto);
    protected abstract void updateEntity(UPDATE_DTO dto, ENTITY entity);
    protected abstract READ_DTO toReadDto(ENTITY entity);

    /** Дополнительные in-place апдейтеры для тел PATCH; по умолчанию пусто. */
    protected void patches(Patches<ENTITY> p) { }
}

public final class Patches<ENTITY> {
    public <P extends AbstractDto<?>> Patches<ENTITY> add(Class<P> patchClass, BiConsumer<P, ENTITY> apply);
}
```

`mappers()` возвращает `Mapper<CREATE_DTO, ENTITY>` (оборачивает `toEntity`, затем зовёт
`entity.nullifyZeroId()` на результате) и `Mapper<ENTITY, READ_DTO>` (оборачивает `toReadDto`).
`updaters()` возвращает `Updater<UPDATE_DTO, ENTITY>` (оборачивает `updateEntity`) плюс по одному
`Updater` на каждую запись `patches()`. Нормализация «id 0 значит новый» остаётся: это
документированное правило библиотеки, а не неявное сопоставление.

Одиночные базы, у каждой один абстрактный метод:

| Класс | Реализует | Абстрактный метод | Примечание |
|---|---|---|---|
| `AbsMapEntityToDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractDto<?>>` | `Mapper<ENTITY, DTO>` | `DTO create(ENTITY from)` | Контракт как сегодня, внутренности ModelMapper удалены. |
| `AbsMapDtoToEntity<DTO extends AbsBaseDto, ENTITY extends AbstractEntity<?>>` | `Mapper<DTO, ENTITY>` | `ENTITY create(DTO from)` | `map()` = `create()` + `nullifyZeroId()`. Заменяет `AbsMapBasic`, `AbsMapBaseDtoToEntity`, `AbsMapCreateDtoToEntity`. |
| `AbsMapUpdateDtoToEntity<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>>` | `Updater<DTO, ENTITY>` | `void update(DTO from, ENTITY into)` | |
| `AbsMapperExtRelation<DTO extends AbsCreateDto, ENTITY, EXT_ID, EXT extends AbstractEntity<?>>` | — | `void setRelation(ENTITY target, EXT relation)` | `map(extId, dto)` / `mapAll` без изменений: `mapper.map(dto, entityClass)` + `referenceById`. Поиск через `FieldUtils` удалён. |

Все конструкторы принимают первым `AbsMapper`, затем классы пары, как сегодня.

Пример (test-application, `OrderMapConfig`):

```java
@Component
public class OrderMapConfig extends AbsFlexMapConfig<OrderCreateDto, OrderUpdateDto, OrderDto, OrderEntity> {
    public OrderMapConfig(AbsMapper mapper) {
        super(mapper, OrderCreateDto.class, OrderUpdateDto.class, OrderDto.class, OrderEntity.class);
    }
    @Override protected OrderEntity toEntity(OrderCreateDto dto) {
        OrderEntity e = new OrderEntity();
        e.setName(dto.getName());
        e.setLines(mapper.mapAll(dto.getLines(), OrderLineEntity.class)); // вложенные дети: явно
        return e;
    }
    @Override protected void updateEntity(OrderUpdateDto dto, OrderEntity e) { e.setName(dto.getName()); }
    @Override protected OrderDto toReadDto(OrderEntity e) { return new OrderDto(e.getId(), e.getName()); }
    @Override protected void patches(Patches<OrderEntity> p) {
        p.add(OrderNamePatch.class, (patch, e) -> e.setName(patch.name()));
    }
}
```

### Сервисы (`flex.service`)

- `AbsFlexServiceR`: тип поля `AbsMapper`; `mapReadDto` / `mapAllReadDto` без изменений.
- `AbsFlexServiceRUD`:
  - `update(UPDATE_DTO)` по сигнатуре не меняется; in-place `mapper.map(dto, entity)` теперь
    находит `Updater<UPDATE_DTO, ENTITY>`.
  - **Новый** `public READ_DTO patch(AbstractDto<ENTITY_ID> body)`: тот же приватный конвейер
    `runUpdate`, что и у `update` (проверка id, `beforeUpdateHook`, хуки
    `AbsUpdateChangesHookable`, загрузка, `mapper.map(body, entity)`, сохранение,
    `afterUpdateHook`). Требует `Updater<класс тела, ENTITY>`; иначе `MappingNotFoundException`.
  - **Новый** `protected READ_DTO changeEntity(ENTITY_ID id, Consumer<ENTITY> change)`: загрузить
    или `AppNotFoundException`; если сервис реализует `AbsUpdateChangesHookable`, снять
    `previous = mapReadDto(entity)` до изменения; применить `change`; `repository.save`;
    `current = mapReadDto`; `afterUpdateHook(current)`; hookable `afterUpdateHook(previous, current)`.
    Before-хуки, принимающие DTO, пропускаются, потому что DTO нет; javadoc это оговаривает.
  - **Удаляются**: `updatePartial`, `copyPartial`, `IGNORE_PARTIAL_UPDATE_PROPERTIES`,
    `mapEntity(Object)`, `mapAllEntities(Collection<?>)`.
- `AbsFlexServiceCRUD`: **новые** `protected ENTITY mapEntity(CREATE_DTO)` и
  `protected List<ENTITY> mapAllEntities(Collection<CREATE_DTO>)` (типизированы, переехали сюда
  из RUD). `save` / `saveAll` / `persistOrMerge` без изменений.
- `AbsFlexServiceExtCRUD`, `AbsFlexPagingAndSortingService`, все `AbsFlexController*`: только тип
  `AbsMapper`.
- `AbsUpdateChangesHookable` не меняется (`beforeUpdateHook(READ_DTO previous, AbstractDto<ENTITY_ID> current)`
  уже подходит для тел патчей).

### Поведение при ошибках

| Ситуация | Исключение | Когда |
|---|---|---|
| Пары нет в реестре | `MappingNotFoundException` (направление + оба FQCN) | при вызове |
| Одна пара зарегистрирована дважды | `IllegalStateException` (пара + обе реализации) | сборка реестра |
| У сервиса нет его мапперов | `IllegalStateException` со списком всех недостающих пар | старт контекста (`AbsMappingChecker`) |

## Удаляется

Main библиотеки: `AbsModelMapper`, `mapper.core.AbsMapBasic`, `mapper.core.AbsMapBaseDtoToEntity`,
`mapper.core.AbsMapDtoToEntity` (старый), `mapper.core.RegisterableMapper`,
`mapper.AbsMapCreateDtoToEntity`, `mapper.composite.AbsFlexMapConfigAbstract`,
`mapper.composite.AbsFlexMapConfigDefault`, `config.AbsMapperEagerInitPostProcessor`,
`config.AbsTypeMapChecker`, `util.FieldCopyUtil`. Зависимость `org.modelmapper:modelmapper`
убирается из `library/pom.xml`. `commons-lang3` остаётся (`filterspecification`, `PageFilterRequest`).

Тесты библиотеки: `AbsModelMapperInPlaceMapTest`, `AbsMapperEagerInitPostProcessorTest`,
`AbsCrudCustomizerEagerFlagTest`, `AbsMapBasicRegistrationTest`, `AbsFlexMapConfigSharedEntityTest`,
`AbsMapBaseDtoToEntityNullifyZeroIdTest`, `AbsTypeMapCheckerTest` (последние два переписываются
под новыми именами, см. ниже).

test-application: `config/ModelMapperConfig`, `eagerinit/*` (три теста), `util/FieldCopyUtilTest`.

## Миграция test-application

- `OrderMapConfig`, `RegionMapConfig`, `TaskMapConfig` → `AbsFlexMapConfig` с явными
  `toEntity` / `updateEntity` / `toReadDto`; Order маппит `lines` через `mapper.mapAll` и
  объявляет `OrderNamePatch` в `patches()`.
- `OrderLineMapConfig` → `OrderLineMapper extends AbsMapDtoToEntity` (только create-направление;
  ни один IT не маппит `OrderLineEntity` обратно в `OrderLineDto`, поэтому read-направление
  не регистрируется).
- `MeetingMapper`, `OrderViewMapper`: только тип параметра конструктора.
- `TaskExtMapper`: реализует `setRelation(TaskEntity task, ProjectEntity project)`.
- Сервисы: тип параметра конструктора. `OrderServiceCRUD` получает `rename(id, name)` поверх
  `changeEntity`, его использует новый IT.
- Новый IT `FlexPatchIT`: `patch(OrderNamePatch)` идёт через зарегистрированный `Updater` и хуки
  обновления; `rename` идёт через `changeEntity`; тело патча без `Updater` падает с
  `MappingNotFoundException`.
- Новый IT `MapperRegistryLazyInitIT`: контекст, поднятый с `spring.main.lazy-initialization=true`,
  содержит в реестре все мапперы демо, и чекер проходит (заменяет `eagerinit/*`).
- `FlexSaveOverridingMapperIT`: переопределение `customizeTypeMap` из ModelMapper становится бином
  `Mapper.of(ZeroIdOrderCreate.class, OrderEntity.class, ...)`, копирующим id `0` как есть;
  проверка (persistOrMerge всё равно вставляет) не меняется.
- `FlexTwoConfigsForSameEntityIT`: по смыслу не меняется; два конфига на одну сущность регистрируют
  разные пары и не конфликтуют.
- Все остальные IT (`FlexSaveCascadeIT`, `FlexUpdateIT`, `FlexDeleteIT`, `FlexExtSaveIT`,
  `FlexAssignedIdSaveIT`, `MeetingPage*IT`) остаются как есть. То, что они остаются зелёными, и
  есть доказательство, что поведение сервисов не изменилось.

## Юнит-тесты библиотеки (новые или переписанные)

- `MapperRegistryTest`: точный поиск; подъём по суперклассам (зарегистрировано для `Base`,
  экземпляр `Sub extends Base` находится); апдейтер идёт и по цепочке назначения; интерфейсы
  не рассматриваются; дубль пары падает с обоими именами; промах бросает
  `MappingNotFoundException` с направлением; `MapperSource` разворачивается; два источника с одной
  сущностью и разными DTO сосуществуют.
- `AbsMapperTest`: обработка null во всех трёх `map*`; `map(from, into)` делегирует `Updater` и
  возвращает `into`; supplier реестра вызывается один раз.
- `AbsFlexMapConfigTest`: отдаёт ровно три адаптера с объявленными классами плюс по одному на
  запись `patches()`; результат `toEntity` с id `0` возвращается с `null` id.
- `AbsMapDtoToEntityNullifyZeroIdTest`: переписанный существующий тест на новой базе.
- `AbsMappingCheckerTest`: сообщает все недостающие пары одним исключением; пропускает проверку,
  когда выключен; `isRunning` равен false после неудачного старта.
- `AbsGenericCrudConfigurationTest`: три бина существуют, бина ModelMapper нет, `AbsMapper`
  создаётся раньше реестра.
- `AbsFlexServiceRUDPatchTest`: `patch` запускает те же хуки, что и `update`; отсутствие `Updater`
  всплывает как `MappingNotFoundException`; `changeEntity` применяет изменение, сохраняет,
  запускает after-хуки и пропускает before-хуки с DTO.
- `AbsMapperExtRelationTest`: `map(extId, dto)` зовёт абстрактный `setRelation` со ссылкой.

## pom, документация, версия

- `library/pom.xml`: убрать `org.modelmapper:modelmapper`.
- Версия: `mise exec -- mvn -q versions:set -DnewVersion=15.0 -DgenerateBackupPoms=false`,
  коммит `chore: start 15.0` (переименование цикла, как 14.1 ничего не выпущено).
- README: из списка возможностей уходит ModelMapper; шаг 4 показывает явный `AbsFlexMapConfig`;
  новый раздел «Миграция на 15.0 (явные мапперы)» с таблицей «было → стало», вариантами PATCH
  (`patch(dto)` + `patches()`, `changeEntity`), переименованием флага `AbsCrudCustomizer` и
  замечанием, что потребители на 13.3.15 применяют таблицу flex-only и эту за один проход.
- CHANGELOG, `## Не выпущено`: описание изменения по-русски в стиле записи 14.0.
- CLAUDE.md: одна строка — маппинг только явный, через реестр; никакого ModelMapper и рефлексии
  в маппинге.

## Выполнение: 4 коммита, каждый зелёный на `./mvnw -B verify`

1. `chore: start 15.0` — только версия.
2. `feat(mapper): explicit Mapper/Updater registry` — аддитивно: `Mapper`, `Updater`,
   `MapperSource`, `MapperRegistry`, `Patches`, `MappingNotFoundException` и их юнит-тесты.
   ModelMapper ещё на месте; реестром пока никто не пользуется.
3. `feat!: replace ModelMapper with explicit mappers` — `AbsMapper`, `AbsFlexMapConfig`,
   переписанные базы, `AbsMappingChecker`, конфигурация, сервисы (`patch`, `changeEntity`,
   типизированный `mapEntity`), все удаления включая зависимость, миграция test-application и
   новые IT. Одним коммитом, потому что старые имена `AbsMapEntityToDto` / `AbsMapUpdateDtoToEntity`
   переиспользуются в том же пакете, и test-application не может компилироваться против обоих.
4. `docs: README, CHANGELOG and CLAUDE.md for explicit mappers`.

## Проверка

- `./mvnw -B verify` зелёный после каждого коммита (юнит + failsafe IT).
- `grep -ri modelmapper library/src test-application/src library/pom.xml` → пусто.
- `grep -rn "FieldUtils\|java.lang.reflect" library/src/main/java/by/nhorushko/crudgeneric/flex` → пусто.
- `grep -rn "updatePartial\|FieldCopyUtil\|RegisterableMapper\|AbsModelMapper" library/src test-application/src` → пусто.

## Вне задачи

- Миграция самих LocatorServer и bi-dvr (отдельная работа на каждого потребителя, опирается на
  таблицы в README).
- Покрытие чекером `AbsFlexPagingAndSortingService` (его `toDto` может быть переопределён,
  поэтому отсутствие пары там не обязательно ошибка).
- Любые изменения в `filterspecification`, `pageable`, контроллерах и зависимости `commons-lang3`.
