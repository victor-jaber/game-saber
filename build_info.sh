#!/bin/bash
export JAVA_HOME=$(dirname $(dirname $(which java)))
echo "=== Jogo da Memória Bobo - Android App ==="
echo ""
echo "This is a native Android application."
echo "It requires the Android SDK to build."
echo ""
echo "Java: $(java -version 2>&1 | head -1)"
echo "Gradle wrapper: ready"
echo ""
echo "To build the APK:"
echo "  ./gradlew assembleDebug"
echo ""
echo "Note: Android SDK (build-tools, platform-35) must be"
echo "available for the build to succeed."
echo ""
echo "Project structure:"
find /home/runner/workspace/app/src/main -name "*.kt" -o -name "*.xml" | sort
echo ""
echo "=== Build attempt ==="
./gradlew assembleDebug 2>&1
