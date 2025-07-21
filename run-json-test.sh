#!/bin/bash

echo "==================================="
echo "log4Rich JSON Logging Test"
echo "==================================="

# Clean up previous test logs
echo "🧹 Cleaning up previous test logs..."
rm -rf logs/
mkdir -p logs

# Compile the project
echo "🔨 Compiling project..."
mvn compile test-compile -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi

# Run the JSON test application
echo "🚀 Running JSON logging test application..."
mvn exec:java -Dexec.mainClass="com.log4rich.JsonLoggingTestApp" -Dexec.classpathScope=test -Dlog4rich.config=json-test.config -q

echo ""
echo "📁 Generated log files:"
find logs/ -name "*.log" -exec ls -lh {} \; 2>/dev/null | awk '{print "   " $9 " (" $5 ")"}'

echo ""
echo "🔍 Sample JSON outputs:"
echo ""

# Show sample from compact JSON
if [ -f "logs/json-compact.log" ]; then
    echo "📄 Compact JSON (logs/json-compact.log):"
    head -3 logs/json-compact.log | sed 's/^/   /'
    echo ""
fi

# Show sample from pretty JSON
if [ -f "logs/json-pretty.log" ]; then
    echo "📄 Pretty JSON (logs/json-pretty.log):"
    head -15 logs/json-pretty.log | sed 's/^/   /'
    echo ""
fi

# Show exception JSON
if [ -f "logs/exceptions.log" ]; then
    echo "📄 Exception JSON (logs/exceptions.log):"
    head -2 logs/exceptions.log | sed 's/^/   /'
    echo ""
fi

echo "✅ JSON logging test completed!"
echo "📖 Review all log files in the logs/ directory"