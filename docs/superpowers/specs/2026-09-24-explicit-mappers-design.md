# Явные мапперы вместо ModelMapper — дизайн

**Дата:** 2026-09-24
**Статус:** утверждён; ревизия после ревью 2026-09-24 (решения 2 и 7–9, швы записи, пропуск моков)

## Контекст

Каждое преобразование DTO ↔ entity в flex-стеке идёт через ModelMapper. По-настоящему явным
сегодня является только одно направление: entity → read DTO, где `AbsMapEntityToDto` ставит
конвертер, вызывающий `create(entity)` потребителя. Всё остальное — неявное сопоставление полей,
с которым владелец постоянно борется:

- **create DTO → entity** и **update DTO → entity** копируют поля по имени (STANDARD matching,
  field matching включён, `skipNull`), после чего вызывается хук `mapSpecificFields`. Что именно
  скопируется, зависит от имён акцессоров, аннотаций Lombok и правил сопоставления ModelMapper,
  а не от кода, который написал потребитель.
- **read DTO → entity** регистрируется каждым `AbsFlexMapConfigDefault` ради
  `AbsTypeMapChecker` и `updatePartial`. `updatePartial(id, partial)` копирует поля partial-тела
  на прочитанный read DTO рефлексией (`FieldCopyUtil`), а затем неявно кладёт read DTO на
  сущность. В LocatorServer это основной путь PATCH: 22 вызова в 11 файлах (grep без
  комментариев и объявлений) — контроллеры Unit, User, MechanismMode, Retranslator, TelegramChat,
  UnitGroup и сервисы Unit, Vehicle, Sensor, Warehouse, NotificationSource; 38 PATCH-эндпоинтов
  во flex-контроллерах; тела — partial-классы без id (`PartialName implements PartialNameI`),
  id из path. Контроллеры библиотеки и bi-dvr его не используют. В комментариях мапперов LocatorServer
  read DTO → entity описан как опасный на 14.x: неявное копирование положило бы `owner` глубоко
  поверх managed `UserEntity` и затёрло `areaM2`.
- **self-map entity → entity** регистрируется «для клонирования»; на HEAD его никто не
  использует, но в 13.3.15 это механизм Preset-мапперов: `AbsMapDtoToPresetEntity` в
  `handleAfterMapSpecificFields` находил сохранённую строку через `entityManager.find` и звал
  `mergeActualAndReceived(actual, received)`, который по умолчанию делал
  `modelMapper.map(received, actual)` — неявно накладывал свежесмапленную отсоединённую копию на
  managed-строку вместе с её непустыми дефолтами вроде пустых коллекций. LocatorServer построен
  на этом: 35 Preset-мапперов, из них 15 переопределяют `mergeActualAndReceived`, чтобы
  переносить только поля запроса.
- **вложенные дети** (например, `OrderCreateDto.lines` → `OrderEntity.lines`) каскадятся неявно
  через те TypeMap, которые случайно оказались зарегистрированы; `nullifyZeroId` для детей живёт
  в пост-конвертере, потому что это единственный хук, который ModelMapper вызывает для вложенных
  объектов.
- Регистрация — побочный эффект конструктора (`AbsMapBasic` зовёт `createTypeMap` в
  конструкторе), из-за чего и существует `AbsMapperEagerInitPostProcessor`: он принудительно
  создаёт все бины-мапперы, чтобы их TypeMap были зарегистрированы до первого `map()`.
- `AbsMapperExtRelation` находит поле связи рефлексией (первое поле типа `EXT`).

Потребители (цифры по ветке `feature/flex-migration` LocatorServer, на develop flex-конфигов
три): LocatorServer — 17 `AbsFlexMapConfigDefault`, 35 Preset-мапперов
(`AbsMapUpdateDtoToPresetEntity` / `AbsMapDtoToPresetEntity`) в 19 файлах с 21 переопределением
`handleAfterMapSpecificFields` и 15 — `mergeActualAndReceived`, 13 `AbsMapperExtRelation`,
11 v2- и 42 v1-маппера, 60 вызовов `map` / `mapAll` фасада в файлах, импортирующих
`AbsModelMapper`; bi-dvr — 4 v1-маппера со своим бином ModelMapper, flex не используется.
Оба сидят на `13.3.15-jakarta` и на 14.x не переезжали, поэтому перейдут на эту версию одним
проходом и с неявным маппингом на 14.x бороться не будут вовсе. **Сверять API нужно с 13.3.15,
а не с HEAD библиотеки:** Preset-классы существуют только там, HEAD удалил их в `aee52e5` как
«неиспользуемые», не заглянув в потребителя.

## Решения, принятые с владельцем

1. **Жёсткий переход.** ModelMapper удаляется из библиотеки. Никакого fallback: пара, которой нет
   в реестре, — ошибка на старте (чекер) или при вызове.
2. **`updatePartial` и маппинг read DTO → entity удаляются** вместе с `FieldCopyUtil`
   (копирование полей рефлексией, использовалось только там). Форма вызова сохраняется:
   `patch(ENTITY_ID id, Object body)` принимает те же partial-тела без id, но каждое тело
   получает явный `Updater`, объявленный в `patches()` конфига. Для замыканий —
   `changeEntity(id, change)`. Оба пути явные.
3. **`AbsMapperExtRelation.setRelation` становится абстрактным**; рефлексивный поиск поля удаляется.
4. **Форма API:** два интерфейса `Mapper` / `Updater`, `MapperRegistry` и один
   `AbsFlexMapConfig` на сущность с тремя абстрактными методами. Для CRUD-тройки потребитель
   интерфейсы напрямую не реализует; view-DTO и детей закрывают базовые классы с одним
   абстрактным методом.
5. **Фасад переименован** `AbsModelMapper` → `AbsMapper`. Сигнатуры методов сохраняются, поэтому
   60 прямых вызовов в LocatorServer не меняются, меняется только тип параметра конструктора.
6. **Версия 15.0.** Цикл 14.1 пуст (`## Не выпущено` пустой), поэтому первый коммит
   переименовывает цикл: `chore: start 15.0`.
7. **Чекер пропускает сервисы, у которых `getEntityClass()` вернул null** (Mockito-моки в
   тестах потребителя), с WARN в лог. Отдельного свойства для выключения нет: единственный
   переключатель — `AbsCrudCustomizer`.
8. **Поиск в реестре — только по точному ключу; единственная нормализация — JPA-прокси до
   ближайшего `@Entity`-предка.** Никакого подъёма по суперклассам и интерфейсам: в
   LocatorServer `Carrier extends CarrierUpdate extends CarrierCreate`, и подъём молча подобрал
   бы маппер родителя. Для иерархий и интерфейсов — пара на каждый конкретный класс плюс
   диспетчер на абстрактный тип.
9. **Путь миграции LocatorServer:** хвост v1/v2 доводится до flex на текущей 13.3.15 (flex-стек
   там уже есть), затем 13.3.15 → 15.0 одним шагом, минуя 14.0. Через 14.0 идти нельзя: там
   `updatePartial` кладёт read DTO на managed-сущность неявно, через этот путь пошли бы все 38
   PATCH, а 36 фиктивных TypeMap read DTO → entity и проверки 17 хуков на коллекции с
   `orphanRemoval` были бы выброшены на 15.0. Окно без компиляции сокращается подготовкой на
   13.3.15: переопределить `setRelation` в 13 ext-мапперах (метод уже protected), сделать явными
   поля в конфигах, которые сегодня опираются на неявное сопоставление (Skill, Carrier,
   Warehouse). Промежуточного релиза библиотеки 13.3.16 не будет: новое ядро переиспользует
   имена `AbsMapEntityToDto` / `AbsMapUpdateDtoToEntity` и рядом со старым жить не может.

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
- **Поиск — только по точному ключу.** Ни подъёма по суперклассам, ни просмотра интерфейсов:
  в LocatorServer `Carrier extends CarrierUpdate extends CarrierCreate` (так же Skill и
  Warehouse), четыре read DTO наследуют `*Short`, всего 71 DTO наследует другой DTO. Подъём по
  иерархии молча подобрал бы `Mapper<CarrierCreate, …>` для update DTO и создал новую сущность
  из тела обновления — та же магия «по совпадению», только по типу вместо имени поля.
- **Единственная нормализация — JPA-прокси.** Если runtime-класс не помечен `@Entity`, а
  среди его предков есть помеченный, класс заменяется ближайшим `@Entity`-предком. Так
  разрешаются Hibernate-прокси (`getReference`, lazy-ссылки): их класс — безымянный наследник
  сущности без аннотации. `@Entity`-наследник `@Entity`-класса не нормализуется, у него свой
  ключ. Правило одно для источника (`mapper`, `updater`) и для назначения (`updater`, где
  назначение — существующий экземпляр). `jakarta.persistence-api` в библиотеке уже есть
  (`provided`).
- Нормализация кэшируется по runtime-классу в `ConcurrentHashMap`; сами пары — обычный
  `HashMap`, заполненный при сборке.
- Промах бросает `MappingNotFoundException` (`flex.exception`, наследует `RuntimeException`)
  с направлением и обоими полными именами классов, например
  `No Updater registered for com.x.OrderUpdateDto -> com.x.OrderEntity`.
- Варианты `find*` возвращают `Optional` для чекера.

Иерархии DTO — интерфейс с реализациями (sealed + records) или абстрактный класс с
наследниками (`StepTransit ⊂ StepAddress ⊂ Step` в LocatorServer) в роли `UPDATE_DTO` или
`CREATE_DTO` — обслуживаются одним правилом: пара на каждый конкретный класс плюс один явный
диспетчер на абстрактный тип, чтобы чекер нашёл пару сервиса:
`Updater.of(Step.class, StepEntity.class, (body, e) -> mapper.map(body, e))`. Вызов внутри
диспетчера ищет по runtime-классу тела, то есть по конкретной реализации, и в сам диспетчер
не возвращается, потому что экземпляра с runtime-классом `Step` не бывает. Оговорка: диспетчер
только для абстрактных классов и интерфейсов; для конкретного базового класса (как
`CarrierUpdate`, у которого есть наследник `Carrier`) регистрируется настоящий маппер, иначе
диспетчер нашёл бы сам себя. Правило идёт в README.

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

Обратная сторона: первый `map()` собирает реестр, а сборка создаёт все бины-мапперы. Если
первый вызов случится в конструкторе или `@PostConstruct` бина, от которого транзитивно зависит
хотя бы один маппер, Spring бросит `BeanCurrentlyInCreationException`. Сегодня ModelMapper в
такой ситуации молча маппил неявно. Правило для README: не маппить в конструкторах и
`@PostConstruct`; прогревы кэшей делать в `ApplicationReadyEvent` или `SmartLifecycle`.
В LocatorServer `@PostConstruct` с маппингом нет.

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
один раз `IllegalStateException` с их списком (сегодня останавливается на первой). Сервис, у
которого `getEntityClass()` или `getReadDtoClass()` вернул null, пропускается с WARN и именем
бина: это Mockito-мок из теста потребителя (у настоящего сервиса классы — final-поля,
заполненные в конструкторе); в LocatorServer так замоканы `UnitService`, `GeofenceService`,
`SensorService`, `MileageNormConsumptionService`. `isRunning` выставляется только после
успешной проверки, как сейчас.

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

    /** Updater на каждое тело PATCH (partial-класс, id в теле не нужен); по умолчанию пусто. */
    protected void patches(Patches<ENTITY> p) { }
}

public final class Patches<ENTITY> {
    public <P> Patches<ENTITY> add(Class<P> patchClass, BiConsumer<P, ENTITY> apply);
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

Что использует сервис каждого вида (заглушек нигде нет; промежуточный уровень конфига
«update + read» решено не вводить, хватает одиночных классов):

| Сервис | Пары, которые требует чекер | Классы потребителя |
|---|---|---|
| `AbsFlexServiceR` | `Mapper<ENTITY, READ_DTO>` | один `AbsMapEntityToDto` (как `MeetingMapper` в демо) |
| `AbsFlexPagingAndSortingService` | чекер не проверяет; `Mapper<ENTITY, DTO>` нужен в рантайме, если `toDto` не переопределён | один `AbsMapEntityToDto` |
| `AbsFlexServiceRUD` | `Mapper<ENTITY, READ_DTO>` + `Updater<UPDATE_DTO, ENTITY>` | `AbsMapEntityToDto` + `AbsMapUpdateDtoToEntity`; тела PATCH — через `patches()` конфига или бинами `Updater.of`. Если у сущности есть и create DTO (его пару в рантайме использует ext-маппер, как `MechanismModeExtMapper` в LocatorServer), удобнее один `AbsFlexMapConfig` |
| `AbsFlexServiceCRUD`, `AbsFlexServiceExtCRUD` | + `Mapper<CREATE_DTO, ENTITY>` | один `AbsFlexMapConfig` |

#### Preset-мапперы 13.3.15 → 15.0

Preset — то, на чём в 13.3.15 устроен update у LocatorServer: маппер сам находит сохранённую
строку и сам решает, что в неё переносить. В новом дизайне эти две обязанности разъезжаются
по сервису и конфигу, а кода становится меньше:

| 13.3.15 (Preset) | 15.0 |
|---|---|
| `AbsMapUpdateDtoToPresetEntity` из фабрики `mapperUpdateDtoToEntity(...)` | `updateEntity(UPDATE_DTO, ENTITY)` в `AbsFlexMapConfig`; фабрика исчезает |
| `handleAfterMapSpecificFields(source, received)` → `applyToStored(id, dto)`: `entityManager.find` + перенос полей | загрузка — `loadForUpdate(id)` в сервисе (по умолчанию `findById`, переопределяется ради fetch join или фильтра); перенос полей — тело `updateEntity` на managed-сущности |
| `mergeActualAndReceived(actual, received)`: перенос полей запроса с отсоединённой копии на `actual` | то же тело `updateEntity`, только источник — DTO, а не `received`; отсоединённой копии больше нет |
| `AbsMapDtoToPresetEntity` для read DTO → entity из `mapperReadDtoToEntity(...)` (путь `updatePartial`) | исчезает; тела PATCH — `patches()` |
| `mapSpecificFieldsCreateDtoToEntity(mapper, dto, entity)` поверх неявного копирования | `toEntity(CREATE_DTO)`: `new Entity()` + те же строки |
| `createReadDtoFromEntity(mapper, entity)` | `toReadDto(ENTITY)` |
| `mapperEntityToEntity` (self-map, движок `mergeActualAndReceived` по умолчанию) | исчезает без замены |
| `AbsMapperExtRelation.setRelation` — рефлексивный default, переопределяется редко | абстрактный |

`GeofenceMapperConfig` после переноса: `toEntity` = `new GeofenceEntity()` + `applyRequestFields`,
`updateEntity` = `applyRequestFields(entity, dto)`, `toReadDto` без изменений, `applyToStored` и
оба анонимных Preset-маппера удаляются; `saveAndFlush` уезжает в `saveUpdated` сервиса.

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
        p.add(OrderNamePatch.class, (patch, e) -> e.setName(patch.name())); // record OrderNamePatch(String name), без id
    }
}
```

### Сервисы (`flex.service`)

- `AbsFlexServiceR`: тип поля `AbsMapper`; `mapReadDto` / `mapAllReadDto` без изменений.
- `AbsFlexServiceRUD`. Три пути записи идут через один приватный конвейер и два защищённых
  шва:
  - **Швы** `protected ENTITY loadForUpdate(ENTITY_ID id)` (по умолчанию
    `repository.findById(id).orElseThrow(AppNotFoundException)`; место для fetch join или
    фильтра архивных) и `protected ENTITY saveUpdated(ENTITY entity)` (по умолчанию
    `repository.save(entity)`; `GeofenceService` переопределит на `saveAndFlush`, чтобы
    посчитанная базой площадь попала в ответ, `RetranslatorService` — чтобы получить сохранённую
    сущность для `syncWithDbAndLog`). Оба сегодня переопределяют `update` целиком через
    `mapEntity(dto)`, который уходит.
  - **Конвейер** для `update` и `patch`: before-хук → hookable `beforeUpdateHook(previous, body)`
    (если сервис реализует `AbsUpdateChangesHookable`; `previous = getById(id)`) →
    `entity = loadForUpdate(id)` → `mapper.map(body, entity)` → `saveUpdated(entity)` →
    `current = mapReadDto` → `afterUpdateHook(current)` → hookable `afterUpdateHook(previous, current)`.
  - `update(UPDATE_DTO dto)` по сигнатуре не меняется: `checkId(dto)`, `beforeUpdateHook(dto)`,
    конвейер с `id = dto.getId()`; `mapper.map(dto, entity)` находит `Updater<UPDATE_DTO, ENTITY>`.
    Хук `beforeUpdateHook` типизируется `UPDATE_DTO` вместо `AbstractDto<ENTITY_ID>`. В
    LocatorServer его переопределяют `EcoDrivingCriterionService`, `LogisticSkillService` и
    `NotificationSourceService`; правка у них — тип параметра, все три и так проверяют
    `instanceof` или читают только id. Правило для README: проверка, которая должна закрывать
    все пути записи, живёт в `loadForUpdate`, а не в `beforeUpdateHook`, потому что `patch` и
    `changeEntity` его не вызывают. Запрет править глобальный навык в `LogisticSkillService` —
    ровно такой случай, и он же убирает лишний `findById` из хука.
  - **Новый** `public READ_DTO patch(ENTITY_ID id, Object body)`: та же форма вызова, что у
    `updatePartial(id, partial)`, поэтому у потребителя меняется только имя метода, а
    partial-классы остаются. `requireNonNull(id)`, новый хук `beforePatchHook(ENTITY_ID id,
    Object body)`, конвейер; `mapper.map(body, entity)` ищет `Updater<body.getClass(), ENTITY>`,
    объявленный в `patches()`; иначе `MappingNotFoundException`.
  - **Новый** `protected READ_DTO changeEntity(ENTITY_ID id, Consumer<ENTITY> change)`:
    `loadForUpdate(id)`; если сервис реализует `AbsUpdateChangesHookable`, снять
    `previous = mapReadDto(entity)` до изменения; применить `change`; `saveUpdated`;
    `current = mapReadDto`; `afterUpdateHook(current)`; hookable `afterUpdateHook(previous, current)`.
    Before-хуки, принимающие тело, пропускаются, потому что тела нет; javadoc это оговаривает.
  - **Удаляются**: `updatePartial`, `copyPartial`, `IGNORE_PARTIAL_UPDATE_PROPERTIES`,
    `mapEntity(Object)`, `mapAllEntities(Collection<?>)`.
- `AbsFlexServiceCRUD`: **новые** `protected ENTITY mapEntity(CREATE_DTO)` и
  `protected List<ENTITY> mapAllEntities(Collection<CREATE_DTO>)` (типизированы, переехали сюда
  из RUD). `save` / `saveAll` / `persistOrMerge` без изменений.
- `AbsFlexServiceExtCRUD`, `AbsFlexPagingAndSortingService`, все `AbsFlexController*`: только тип
  `AbsMapper`.
- `AbsUpdateChangesHookable.beforeUpdateHook(READ_DTO previous, Object current)`: `current`
  ослабляется с `AbstractDto<ENTITY_ID>` до `Object`, потому что тело патча id не несёт.
  Реализации в потребителях, `DriverService` и `UserService`, `current` не используют.
  `afterUpdateHook(previous, current)` без изменений.

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
- Новый IT `FlexPatchIT`: `patch(id, new OrderNamePatch("x"))` идёт через зарегистрированный
  `Updater` и хуки обновления; `rename` идёт через `changeEntity`; тело патча без `Updater`
  падает с `MappingNotFoundException`; переопределённый `saveUpdated` вызывается на всех трёх
  путях записи.
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

- `MapperRegistryTest`: точный поиск; экземпляр `Sub extends Base` при паре только для `Base`
  даёт `MappingNotFoundException` (никакого подъёма); класс без `@Entity` с `@Entity`-предком
  (модель Hibernate-прокси) нормализуется до предка и для источника, и для назначения
  `updater`; `@Entity`-наследник `@Entity`-класса не нормализуется; интерфейсы не
  рассматриваются; дубль пары падает с обоими именами; промах бросает
  `MappingNotFoundException` с направлением; `MapperSource` разворачивается; два источника с одной
  сущностью и разными DTO сосуществуют.
- `AbsMapperTest`: обработка null во всех трёх `map*`; `map(from, into)` делегирует `Updater` и
  возвращает `into`; supplier реестра вызывается один раз.
- `AbsFlexMapConfigTest`: отдаёт ровно три адаптера с объявленными классами плюс по одному на
  запись `patches()`; результат `toEntity` с id `0` возвращается с `null` id.
- `AbsMapDtoToEntityNullifyZeroIdTest`: переписанный существующий тест на новой базе.
- `AbsMappingCheckerTest`: сообщает все недостающие пары одним исключением; пропускает проверку,
  когда выключен; сервис с null-классами (мок) пропускается с WARN и не ломает старт;
  `isRunning` равен false после неудачного старта.
- `AbsGenericCrudConfigurationTest`: три бина существуют, бина ModelMapper нет, `AbsMapper`
  создаётся раньше реестра; контекст без единого маппера и сервиса стартует (Spring подставляет
  пустые коллекции в параметры `@Bean`-метода, как сегодня живёт чекер в
  `DriverMapperConfigTest` LocatorServer).
- `AbsFlexServiceRUDPatchTest`: `patch(id, body)` запускает `beforePatchHook`, hookable-хуки и
  `afterUpdateHook`; отсутствие `Updater` всплывает как `MappingNotFoundException`;
  `changeEntity` применяет изменение, сохраняет, запускает after-хуки и пропускает before-хуки
  с телом; `loadForUpdate` и `saveUpdated` вызываются на всех трёх путях, `loadForUpdate`
  без сущности даёт `AppNotFoundException`.
- `AbsMapperExtRelationTest`: `map(extId, dto)` зовёт абстрактный `setRelation` со ссылкой.

## pom, документация, версия

- `library/pom.xml`: убрать `org.modelmapper:modelmapper`.
- Версия: `mise exec -- mvn -q versions:set -DnewVersion=15.0 -DgenerateBackupPoms=false`,
  коммит `chore: start 15.0` (переименование цикла, как 14.1 ничего не выпущено).
- README: из списка возможностей уходит ModelMapper; шаг 4 показывает явный `AbsFlexMapConfig`;
  новый раздел «Миграция на 15.0 (явные мапперы)» с таблицей «было → стало» **по API 13.3.15,
  на котором сидят потребители, а не по HEAD** (Preset-классы и их хуки
  `handleAfterMapSpecificFields` / `mergeActualAndReceived`, фабрики `mapper*` конфига, хуки
  `mapSpecificFields*` и `createReadDtoFromEntity`, `updatePartial`, `getModelMapper()`,
  `AbsCrudCustomizer.typeMapCheckerEnabled`), вариантами PATCH
  (`updatePartial(id, partial)` → `patch(id, body)` плюс `Updater` на каждый partial-класс в
  `patches()`; `changeEntity` для замыканий), швами `loadForUpdate` / `saveUpdated` вместо
  переопределения `update`, переименованием флага `AbsCrudCustomizer`, правилом «не маппить в
  конструкторах и `@PostConstruct`», правилом «проверки на все пути записи — в `loadForUpdate`,
  а не в `beforeUpdateHook`», правилом точного ключа с диспетчером для интерфейсных и
  абстрактных DTO (в LocatorServer: `Logistic.Step`, и пары на каждый класс цепочек
  Carrier / Skill / Warehouse) и предупреждением про
  AOP-pointcut'ы на `updatePartial` (`TrackerCertificateExpireCacheAspect` в LocatorServer
  сломается молча). Порядок для потребителя на 13.3.15: хвост v1/v2 → flex на 13.3.15 по
  таблице flex-only, затем 13.3.15 → 15.0 одним шагом по этой таблице, минуя 14.0.
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
  таблицы в README). LocatorServer доводит хвост до flex на 13.3.15, затем переходит на 15.0
  одним шагом, минуя 14.0; промежуточного релиза библиотеки не будет.
- Покрытие чекером `AbsFlexPagingAndSortingService` (его `toDto` может быть переопределён,
  поэтому отсутствие пары там не обязательно ошибка).
- Любые изменения в `filterspecification`, `pageable`, контроллерах и зависимости `commons-lang3`.
