#!/bin/bash

# Line counter script for Kotlin and Scala files

echo "=== Kotlin Files in kotlin-archive ==="
kotlin_files=$(find kotlin-archive -name "*.kt" -o -name "*.kotlin" 2>/dev/null)
if [ -n "$kotlin_files" ]; then
    kotlin_count=$(echo "$kotlin_files" | xargs wc -l 2>/dev/null | tail -1 | awk '{print $1}')
    kotlin_file_count=$(echo "$kotlin_files" | wc -l | tr -d ' ')
    echo "Files found: $kotlin_file_count"
    echo "Total lines: $kotlin_count"
else
    echo "No Kotlin files found"
fi

echo ""
echo "=== Scala Files in project directory ==="
scala_files=$(find . -name "*.scala" -not -path "./kotlin-archive/*" 2>/dev/null)
if [ -n "$scala_files" ]; then
    scala_count=$(echo "$scala_files" | xargs wc -l 2>/dev/null | tail -1 | awk '{print $1}')
    scala_file_count=$(echo "$scala_files" | wc -l | tr -d ' ')
    echo "Files found: $scala_file_count"
    echo "Total lines: $scala_count"
else
    echo "No Scala files found"
fi
