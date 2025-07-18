#!/bin/bash

# JavaDoc generation script for log4Rich
# This script generates comprehensive JavaDoc documentation

echo "Generating JavaDoc for log4Rich..."

# Create javadoc directory if it doesn't exist
mkdir -p target/javadoc

# Generate JavaDoc with comprehensive settings
javadoc -d target/javadoc \
    -sourcepath src/main/java \
    -subpackages com.log4rich \
    -author \
    -version \
    -use \
    -windowtitle "log4Rich API Documentation" \
    -doctitle "log4Rich Logging Framework" \
    -header "log4Rich v1.0.0" \
    -bottom "Copyright © 2024 log4Rich. All rights reserved." \
    -link https://docs.oracle.com/en/java/javase/17/docs/api/ \
    -classpath "target/classes:$(find ~/.m2/repository -name '*.jar' -path '*/junit/*' | head -1)" \
    -quiet

if [ $? -eq 0 ]; then
    echo "JavaDoc generated successfully in target/javadoc/"
    echo "Open target/javadoc/index.html in your browser to view the documentation."
    
    # Count documentation coverage
    echo ""
    echo "Documentation Coverage Analysis:"
    echo "==============================="
    
    # Count total public methods
    total_public_methods=$(grep -r "public.*(" src/main/java --include="*.java" | wc -l)
    echo "Total public methods: $total_public_methods"
    
    # Count documented methods (those with /** before them)
    documented_methods=$(grep -B1 -r "public.*(" src/main/java --include="*.java" | grep "/\*\*" | wc -l)
    echo "Documented methods: $documented_methods"
    
    # Calculate percentage
    if [ $total_public_methods -gt 0 ]; then
        percentage=$(echo "scale=1; $documented_methods * 100 / $total_public_methods" | bc)
        echo "Documentation coverage: $percentage%"
    fi
    
    echo ""
    echo "Missing JavaDoc Analysis:"
    echo "========================"
    
    # Find methods without JavaDoc
    echo "Methods without JavaDoc comments:"
    grep -n -B2 -A1 "public.*(" src/main/java/com/log4rich/**/*.java | grep -v "/\*\*" | grep "public.*(" | head -20
    
else
    echo "JavaDoc generation failed. Please check for compilation errors."
    exit 1
fi