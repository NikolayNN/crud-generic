# Changelog

История версий библиотеки. Версии до 14.0 выпускались через JitPack и в этом файле
не восстанавливаются — их список остался в тегах репозитория.

## Не выпущено

## 15.0

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
  приводятся к ближайшему `@Entity`-предку. Промах — `MappingNotFoundException` при вызове.
- Ошибки регистрации ломают старт и называют бины по имени и классу: дубль пары, `null` в
  `fromClass()` / `toClass()`, пара для наследника сущности без `@Entity` (при поиске он
  считается прокси, и такую пару нельзя было бы найти). Реестр собирается на старте и при
  `spring.main.lazy-initialization=true`.
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
- `patch` отвергает собственные read, update и create DTO сервиса (`IllegalArgumentException`
  до хуков): update DTO через `patch` прошёл бы мимо `checkId` и `beforeUpdateHook`.
- `beforeUpdateHook` типизирован `UPDATE_DTO` и вызывается только из `update`; у `patch` свой
  `beforePatchHook(id, body)`. `AbsUpdateChangesHookable.beforeUpdateHook` принимает `current`
  как `Object`, а снимок `previous` берётся из строки, которую вернул `loadForUpdate`: проверка в
  `loadForUpdate` срабатывает раньше этого хука, строка читается один раз.
- `mapEntity` / `mapAllEntities` переехали из `AbsFlexServiceRUD` в `AbsFlexServiceCRUD` и
  типизированы `CREATE_DTO`.
- Библиотека больше не задаёт семантику null: `null` в DTO очищает поле, только если
  `updateEntity` или `patches()` так написаны. Раньше null не очищал поле ни на одном пути.

### Миграция
- Потребители на `13.3.15-jakarta` переходят на 15.0 одним шагом, минуя 14.0. Таблица «было →
  стало» по API 13.3.15 — в README, раздел «Миграция на 15.0 (явные мапперы)».

## 14.0

### Публикация
- Библиотека переехала с JitPack в GitHub Packages. Новые координаты:
  `by.nhorushko:crud-abstract-generic` вместо `com.github.NikolayNN:crud-generic`.
  Потребителю нужен `<repository>` на `https://maven.pkg.github.com/NikolayNN/crud-generic`
  и PAT с правом `read:packages` в `~/.m2/settings.xml` — GitHub Packages требует
  аутентификации даже для публичных пакетов. Инструкция в README.
- Исходники `filter-specifications-lib` (ревизия `3.1-jakarta`) перенесены в этот
  репозиторий с сохранением пакета `by.nhorushko.filterspecification`. Импорты у
  потребителей не меняются, зависимость от JitPack исчезла полностью.
- Удалены `jitpack.yml`, публикация артефактов в ветку `mvn-repo` через
  `site-maven-plugin` и `distributionManagement` на локальную папку.
- `jakarta.persistence-api` теперь объявлен явно и со scope `provided` — раньше он
  доходил до потребителей со scope `compile` через pom старого JitPack-артефакта.
  Оба известных потребителя используют JPA starter, так что для них это без эффекта.

### Сборка
- Корневой `pom.xml` стал родителем обоих модулей и единственным местом, где живёт
  версия. Раньше `versions:set` в корне не доходил до `library`, и релиз опубликовал бы
  старую версию.
- `test-application` перешёл с `spring-boot-starter-parent` на импорт
  `spring-boot-dependencies` и помечен `maven.deploy.skip` — это демо, а не артефакт.
- Подключён `maven-failsafe-plugin`: 31 интеграционный тест в 9 классах `*IT` раньше
  не выполнялся вообще, потому что surefire их не видит, а failsafe не был настроен.
  `mvn verify` теперь гоняет 115 тестов вместо 72.
- Добавлены `.gitattributes`, `.mvn/maven.config` и Maven wrapper 3.9.16 (`only-script`).

### CI
- `.github/workflows/ci.yml`: PR и push в develop — только тесты; push в master —
  публикация в GitHub Packages, тег `v<версия>` и GitHub Release из раздела этого файла.
  Теги больше не ставятся руками. Дополнительных секретов не требуется.
- `.claude/settings.json` и хук `guard-bash.sh` запрещают агенту `mvn deploy`,
  `git tag` и `git push --force`.
