#!/usr/bin/env bash
# PreToolUse-хук Claude Code для инструмента Bash.
# Не даёт агенту публиковать артефакты и ставить версионные теги: публикует CI
# (.github/workflows/ci.yml), релиз запускает человек через /aurora-release.
#
# Границы регулярок специально шире буквального написания команды: ловят полную форму
# `groupId:artifactId:version:deploy` и её `deploy-file`-вариант (заливка произвольного файла
# на произвольный URL), `git.exe`/`/usr/bin/git`, а также `--force-with-lease`/`--force-if-includes`
# и их формы с `=<значение>` наравне с голым `--force`/`-f`. Это не экзотика, а самые вероятные
# варианты, которыми агент обойдёт узкую проверку просто выбрав другую форму команды, а не
# намеренно её обходя.
#
# Вход: JSON от Claude Code на stdin, команда лежит в .tool_input.command.
# Выход: exit 2 + текст в stderr — вызов заблокирован, текст показывается модели;
#        exit 0 — команда разрешена.
# jq на машине может отсутствовать, поэтому проверяем по сырому JSON.

input=$(cat)

has() { printf '%s' "$input" | grep -Eq "$1"; }

blocked=""
if has '(mvn|mvnw)[^"]*[[:space:]:]deploy(-file)?([[:space:]]|\\?"|$)'; then
    # Фаза `deploy` и полная форма `groupId:artifactId:version:deploy` — отсюда `:` в границе слева.
    # `(-file)?` ловит и `deploy-file`/`...:deploy-file` (загрузка произвольного файла на
    # произвольный URL). Перечисляем ровно эти два гола, а не `[[:alnum:]-]*`: список опасных
    # написаний тут маленький и известный, а wildcard блокирует и легитимные `-P deployment`,
    # `-pl deployment-module` — профиль или модуль с таким именем вполне реален.
    blocked="публикация артефакта"
elif has '(^|[^[:alnum:]_])git(\.exe)?[[:space:]]+tag([[:space:]]|\\?"|$)'; then
    # Слева достаточно любого не-буквенно-цифрового символа: так ловятся и `git`, и `/usr/bin/git`,
    # и `git.exe`. Исключать `/`, `.` и `-` нельзя — именно они и стоят в путях.
    blocked="создание версионного тега"
elif has '(^|[^[:alnum:]_])git(\.exe)?[[:space:]]+push[^"]*(--force[^[:space:]"]*|-f)([[:space:]]|\\?"|$)'; then
    # `--force` до ближайшего пробела/кавычки ловит `--force-with-lease`, `--force-if-includes` и их
    # формы с `=<значение>` (например, `--force-with-lease=refs/heads/develop:abc123`), не только
    # голый `--force`/`-f`.
    blocked="принудительный push"
fi

if [ -n "$blocked" ]; then
    cat >&2 <<EOF
Заблокировано хуком .claude/hooks/guard-bash.sh: «$blocked».
Артефакты в GitHub Packages публикует CI на push в master, тег v<версия> ставит тот же
workflow. Релиз запускается человеком через /aurora-release, см. README.md
(раздел «Релиз и версии»).
Если команда нужна прямо сейчас, пользователь может выполнить её сам: «! <команда>».
EOF
    exit 2
fi

exit 0
