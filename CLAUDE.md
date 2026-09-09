# crud-generic — заметки для агентов

Библиотека CRUD-абстракций для Spring Boot. Публикуется в GitHub Packages как
`by.nhorushko:crud-abstract-generic`. Потребители: LocatorServer, bi-dvr.

## Команды

- Сборка и тесты: `./mvnw -B verify` (или `mise exec -- mvn -B verify`)
- Только библиотека: `./mvnw -B -pl library test`
- JDK и Maven — из mise (`mise.toml`), системного `mvn` на PATH нет.

## Структура

- `library/` — публикуемый артефакт `crud-abstract-generic`
- `test-application/` — демо-приложение, **не публикуется**; несёт 31 интеграционный
  тест (`*IT`), которые гоняет failsafe
- Версия живёт **только** в корневом `pom.xml`, модули наследуют её через `<parent>`

## Чего не делать

- Не публиковать: `mvn deploy` выполняет CI на push в master (хук блокирует).
- Не ставить теги руками: `v<версия>` создаёт тот же workflow. Тег — выход, а не вход.
- Не поднимать версию перед релизом: версия в pom — та, что разрабатывается.
  Бамп делает `/aurora-release` после релиза.
- Пакет `by.nhorushko.filterspecification` переехал сюда из отдельной библиотеки;
  переименовывать его нельзя — на него завязаны 57 файлов в LocatorServer и bi-dvr.

Полный стандарт релизов — раздел «Docker image & release flow» в `~/.claude/CLAUDE.md`.
