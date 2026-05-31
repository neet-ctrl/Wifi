#!/bin/bash
# Detects non-Room data classes with a default numeric id=0 field.
# These cause "Key '0' was already used" crashes in Compose LazyColumn/LazyRow
# when objects are constructed without explicitly setting the id field.
#
# Exits with code 1 if risky patterns are found.

set -euo pipefail

KT_DIR="android-control-center/app/src/main/java"
ISSUES=0

echo "================================================"
echo "  LazyList Duplicate Key Safety Scanner"
echo "================================================"
echo ""

while IFS= read -r file; do
    # Skip Room entity files — they use autoGenerate so default 0 is safe
    if grep -q "@PrimaryKey" "$file"; then
        continue
    fi

    # Look for: val id: Long = 0 | val id: Int = 0 | val id: Long = 0L
    if grep -qE "val id\s*:\s*(Long|Int)\s*=\s*0(L)?" "$file"; then
        CLASS_NAME=$(grep -oP "(?<=data class )\w+" "$file" | head -1)
        if [ -n "$CLASS_NAME" ]; then
            LINE=$(grep -nE "val id\s*:\s*(Long|Int)\s*=\s*0(L)?" "$file" | head -1)
            echo "::error file=$file::RISKY PATTERN — '$CLASS_NAME' has a default id=0."
            echo "   File : $file"
            echo "   Line : $LINE"
            echo "   Risk : If multiple instances are created without setting 'id',"
            echo "          all will share key '0' in a LazyColumn/LazyRow -> crash."
            echo ""
            ISSUES=$((ISSUES + 1))
        fi
    fi
done < <(find "$KT_DIR" -name "*.kt" 2>/dev/null)

if [ "$ISSUES" -gt 0 ]; then
    echo "================================================"
    echo "  FAILED: $ISSUES risky pattern(s) found."
    echo ""
    echo "  Fix: always assign a unique id when constructing"
    echo "  these objects, e.g. use mapIndexed { i, _ -> }"
    echo "  and set id = i.toLong(), or use a stable field"
    echo "  (packageName, route, timestamp) that is always"
    echo "  unique across all items in the list."
    echo "================================================"
    exit 1
fi

echo "  OK — No risky default-zero ID data classes found."
echo "================================================"
