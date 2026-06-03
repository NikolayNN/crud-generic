# Дизайн: починка create-пути `save()` под Hibernate 6.6 (Spring Boot 3.5)

Дата: 2026-06-03
Статус: согласован, готов к плану реализации
Ветка: master (jakarta, crud-generic 13.3.x)

## 1. Проблема

После миграции Spring Boot 3.3 → 3.5 (Spring Data JPA 3.5, Hibernate ORM 6.6) массово
падает создание сущностей через `save(...)`:

```
jakarta.persistence.OptimisticLockException:
  Row was updated or deleted by another transaction
  (or unsaved-value mapping was incorrect): [...Entity#0]
```

Корень: `SimpleJpaRepository.save()` = `isNew(e) ? persist : merge`, а Spring Data
`isNew` для `Long` считает новым только `id == null`. Для сущностей с `id != null`,
строки которых нет в БД:

- **Hibernate 6.5**: `merge` detached-сущности с отсутствующей строкой молча делал INSERT → работало.
- **Hibernate 6.6**: рассинхрон unsaved-value больше не прощается → `OptimisticLockException` / `StaleObjectStateException`.

Изолированный probe на живой БД (Hibernate 6.6) подтверждает:
`em.merge(entityWithId0)` → `OptimisticLockException`; `em.persist(entityWithIdNull)` → OK.

### Два ломающихся сценария создания

1. **Sentinel `id = 0`** (соглашение «0 = новая»; клиенты POST-ят `id:0`).
   В т.ч. **каскадом**: `save(существующий_родитель)` с новым ребёнком `id=0` в коллекции —
   решение persist/merge для ребёнка принимает Hibernate по каскаду, не `save()`.
2. **Реальный присвоенный / shared-PK id, строки нет** (`@MapsId`, явный ненулевой id у новой сущности;
   напр. `VehicleEntity(unitId)`, `RecalculationTaskEntity(1L,…)`). Зануление неприменимо — id законно ненулевой.

## 2. Фактическое состояние кода (важно — расходится с исходным тикетом)

- **v1 sentinel-корень уже занулялся.** `CrudGenericService.save()` → `checkIdForSave(entity)`
  (`ImmutableGenericService:118-121`) уже делает `e.setId(null)` после валидации. Прямой корень с `id=0`
  уже идёт в `persist`.
- **v2 sentinel-корень уже занулялся.** `AbsServiceCRUD.save()` зовёт `entity.nullifyZeroId()`
  (`AbstractEntity:21`) перед `repository.save()`.
- **v1 `save()` сейчас запрещает реальный id.** `checkIdForSave(Long)` (`ImmutableGenericService:123-128`)
  кидает `IllegalArgumentException` на любом не-null/не-0 id. → Сценарий 2 через v1 `CrudGenericService.save()`
  **недостижим** (кидает до репозитория), а Part 2 тикета там — мёртвый код, пока guard не снят.
- **v2 `save()` уже upsert.** Guard'а нет; семантика «insert или update» уже есть. Ломается только
  «реальный id, строки нет» на 6.6.
- **v2 имеет отдельный `update()`** (`AbsServiceRUD:32-48`) с guard'ом `checkId` (требует не-new id).
  Раскладка save=upsert / update=guarded — целевая модель.

### Вывод: реально не закрыты ровно две дыры

1. **Каскадные дети `id=0`** — ни `checkIdForSave`, ни `nullifyZeroId` не трогают детей в коллекциях,
   только корень.
2. **Реальный присвоенный id, строки нет** — все три семейства всё ещё зовут `repository.save()` → `merge` → падение на 6.6.

## 3. Решение: два независимых, композируемых фикса

### Fix A — нормализация `0 → null` в post-converter'ах мапперов (Сценарий 1, вкл. каскады)

Ответственность за зануление sentinel переходит **мапперу** — единственному месту, видящему **весь граф**
(корень + вложенные дети). Дети маппятся через свои зарегистрированные TypeMap'ы, поэтому post-converter
срабатывает на каждой сущности.

- **v1** `AbstractMapper.toEntityConverter()` — после `mapSpecificFields`:
  `if (e.getId() != null && e.getId() == 0L) e.setId(null);` (v1 `AbstractEntity` имеет `setId(Long)`).
- **v2** `AbsMapperEntityDto.createConverterDtoToEntity()` — добавить `destination.nullifyZeroId();`.

**Момент срабатывания:** внутри `mapper.toEntity(dto)`, в хвосте конверсии каждого TypeMap — до того, как
entity вернётся в `save()`. Для каскада зануление детей происходит рекурсивно, при маппинге каждого элемента
коллекции, в post-converter'е дочернего маппера. Сервис детей не видит — поэтому зануление обязано быть в мапперe.

**Следствие:** `setupEntityBeforeSave` теперь видит `id=null`, а не `0`. По осмотру либы мест, завязанных на
`id==0` после маппинга, нет. Поведенческий сдвиг отмечен.

**Известное ограничение (документируется):** post-converter срабатывает только если для типа зарегистрирован
TypeMap. Корень — всегда (`setupMapper`). Ребёнок — только при наличии своего `AbstractMapper`. Ребёнок, маппящийся
«неявно» (без зарегистрированного crud-generic-маппера), не занулится. В этой либе сущности идут через
зарегистрированные мапперы → на практике покрыто.

### Fix B — INSERT-aware `save()` через общий helper (Сценарий 2)

```java
@PersistenceContext protected EntityManager entityManager;

protected ENTITY persistOrMerge(ENTITY entity) {
    Long id = entity.getId();                       // ENTITY_ID для v2/flex
    if (id == null || !repository.existsById(id)) {
        entityManager.persist(entity);              // новая строка → INSERT
        return entity;
    }
    return repository.save(entity);                 // существующая строка → MERGE/UPDATE
}
```

Восстанавливает «insert-if-absent», которое раньше выполнял `merge` на 6.5.

**Размещение:**
- **v1**: `EntityManager` + `persistOrMerge` в `ImmutableGenericService` (база с `repository`).
  Используется в `CrudGenericService.save/saveAll` и `CrudAdditionalGenericService.save/saveAll`.
- **v2**: `EntityManager` + `persistOrMerge` в `AbsServiceR` (база), использование в `AbsServiceCRUD.save/saveAll`.
- **flex**: то же в `AbsFlexServiceCRUD.save/saveAll`.

## 4. Точки правки

### v1 (`by.nhorushko.crudgeneric`)
- `mapper.AbstractMapper.toEntityConverter()` — добавить зануление `0→null` (Fix A).
- `service.ImmutableGenericService` — добавить `@PersistenceContext EntityManager` + `persistOrMerge(ENTITY)`.
- `service.CrudGenericService.save/saveAll` — убрать вызов `checkIdForSave(entity)` из create-пути,
  сохранять через `persistOrMerge`. (Снятие guard → `save()` становится upsert.)
- `service.CrudAdditionalGenericService.save/saveAll` — то же (убрать `checkIdForSave`, звать `persistOrMerge`).
- `service.ImmutableGenericService.checkIdForSave(ENTITY)` — убрать `setId(null)` (теперь это делает маппер).
  Метод остаётся для совместимости, но из create-пути `save()` не вызывается.
- `service.CrudExpandGenericService` — no-op override `checkIdForSave` становится нерелевантным для save-пути; оставить (безвредно) либо удалить — решить при реализации.

### v2 (`by.nhorushko.crudgeneric.v2`)
- `mapper.AbsMapperEntityDto.createConverterDtoToEntity()` — добавить `destination.nullifyZeroId()` (Fix A).
- `service.AbsServiceR` — добавить `@PersistenceContext EntityManager` + `persistOrMerge`.
- `service.AbsServiceCRUD.save/saveAll` — заменить `repository.save(...)` на `persistOrMerge(...)`.
  Семантика upsert не меняется — это смена механизма.

### flex (`by.nhorushko.crudgeneric.flex`)
- `service.AbsFlexServiceCRUD.save/saveAll` — заменить `repository.save(...)` на `persistOrMerge(...)`.
- Мапперы flex используют v2 `AbsMapperEntityDto` → нормализация `0→null` приходит из Fix A автоматически.

## 5. Решения (зафиксировано с автором)

1. **Охват**: все три семейства (v1 + v2 + flex), без дублирования уже работающего; v1 догоняет v2/flex.
2. **v1 `save()` → upsert** (guard `checkIdForSave` снят с create-пути). Не дыра: строгий путь — `update()`/`updatePartial()`
   с `checkIdForUpdate` (не трогаем). Выравнивание с уже существующей моделью v2.
3. **Зануление `0→null` — единственная ответственность маппера.** Убрать дублирующие нормализации на уровне сервиса:
   `setId(null)` из v1 `checkIdForSave`; редундантные `entity.nullifyZeroId()` в v2 `AbsServiceCRUD` (маппер уже занулил).
   Один источник истины. Обратимо.

## 6. Отвергнутые альтернативы

- **`Persistable` на `AbstractEntity`** (переопределить Spring `isNew`): элегантно для sentinel-корня, но (а) не
  помогает каскадным детям — Hibernate не консультируется с `Persistable` для каскада, только с id/unsaved-value;
  (б) не отличает новую строку с реальным id от существующей. Решает лёгкую половину, обе тяжёлые — нет.
- **Только Fix A**: оставляет Сценарий 2 (ненулевой assigned id) в `merge` → падение на 6.6.
- **Только Fix B**: `persistOrMerge` на корне не достаёт каскадных детей → Сценарий 1 (каскад) всё ещё падает.

→ A и B ортогональны и оба обязательны.

## 7. Риски / совместимость

- **Производительность**: лишний `existsById` (1 SELECT) только для ненулевого присвоенного id; создание по
  sentinel/null идёт без него. Update shared-PK-сущности стоит 2 SELECT (existsById + merge) вместо 1 — приемлемо
  ради корректности.
- **Семантика существующих корректных потоков не меняется**: update по реальному id → ветка `existsById=true` →
  прежний `repository.save`.
- **v1 поведенческий сдвиг**: `save()` create-only → upsert; `setupEntityBeforeSave` видит `id=null` вместо `0`.
- **`@PersistenceContext`** требует `jakarta.persistence` — проект уже на jakarta.

## 8. Тесты верификации (в `test-application`, реальная персистентность)

1. `save(dto id=0)` IDENTITY-сущность → INSERT, id сгенерирован, без `OptimisticLockException`.
2. `save(существующий parent)` с новым child `id=0` в коллекции → child вставлен (каскад).
3. `save(@MapsId / присвоенный id)`, строки нет → INSERT; строка есть → UPDATE.
4. Регресс: `save(реальный существующий id)` → один UPDATE, без дубликата; существующие `update()`/sentinel-потоки не меняются.
5. По каждому семейству: v1, v2, flex.

Примечание: `*IT`-тесты test-application пропускаются обычным `mvn test` (нет Failsafe);
полный прогон — `-Dtest=*,*IT`.
