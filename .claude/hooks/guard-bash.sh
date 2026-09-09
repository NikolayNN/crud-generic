#!/usr/bin/env bash
# PreToolUse-хук Claude Code для инструмента Bash.
# Не даёт агенту публиковать артефакты и ставить версионные теги: публикует CI
# (.github/workflows/ci.yml), релиз запускает человек через /aurora-release.
#
# Вход: JSON от Claude Code на stdin, команда лежит в .tool_input.command.
# Выход: exit 2 + текст в stderr — вызов заблокирован, текст показывается модели;
#        exit 0 — команда разрешена.
# jq на машине может отсутствовать, поэтому проверяем по сырому JSON.

input=$(cat)

has() { printf '%s' "$input" | grep -Eq "$1"; }

blocked=""
if has '(mvn|mvnw)[^"]*[[:space:]]deploy([[:space:]]|\\?"|$)'; then
    blocked="mvn deploy"
elif has '(^|[^[:alnum:]_./-])git[[:space:]]+tag([[:space:]]|\\?"|$)'; then
    blocked="git tag"
elif has '(^|[^[:alnum:]_./-])git[[:space:]]+push[^"]*(--force|-f)([[:space:]]|\\?"|$)'; then
    blocked="git push --force"
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
