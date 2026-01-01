#!/bin/bash
# Quick script to check if logs are being created

echo "==== Server Status ===="
lsof -nP -iTCP:8082 -sTCP:LISTEN 2>/dev/null || echo "Server NOT listening on 8082"

echo ""
echo "==== Author Log (last 50 lines) ===="
tail -n 50 deployment/author.log 2>/dev/null | grep -E "Profile:|error\.log|Framework started|ERROR|WARN" || tail -n 50 deployment/author.log 2>/dev/null

echo ""
echo "==== Logs Directory ===="
ls -lah deployment/author/launcher/logs/ 2>/dev/null || echo "Logs directory does not exist"

echo ""
echo "==== error.log Status ===="
if [ -f deployment/author/launcher/logs/error.log ]; then
    echo "✅ SUCCESS! error.log EXISTS"
    echo "File size: $(wc -c < deployment/author/launcher/logs/error.log) bytes"
    echo "Line count: $(wc -l < deployment/author/launcher/logs/error.log) lines"
    echo ""
    echo "First 20 lines:"
    head -n 20 deployment/author/launcher/logs/error.log
else
    echo "❌ error.log NOT FOUND"
fi

echo ""
echo "==== access.log Status ===="
if [ -f deployment/author/launcher/logs/access.log ]; then
    echo "✅ access.log EXISTS ($(wc -l < deployment/author/launcher/logs/access.log) lines)"
else
    echo "❌ access.log NOT FOUND"
fi

echo ""
echo "==== request.log Status ===="
if [ -f deployment/author/launcher/logs/request.log ]; then
    echo "✅ request.log EXISTS ($(wc -l < deployment/author/launcher/logs/request.log) lines)"
else
    echo "❌ request.log NOT FOUND"
fi
