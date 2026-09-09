# Changelog

История версий библиотеки. Версии до 14.0 выпускались через JitPack и в этом файле
не восстанавливаются — их список остался в тегах репозитория.

## Не выпущено

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
