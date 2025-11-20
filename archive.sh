#!/usr/bin/env bash 

zip -r archive_edda21.zip . -x ".idea/*" ".git/*" ".gradle/*" "build/*" "*/build/*" "*/node_modules/*" "*.class" "*.jar" "*.zip"
