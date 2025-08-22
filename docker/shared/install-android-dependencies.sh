#!/usr/bin/env bash
#
# Install required dependencies
# sdkmanager can be found in $ANDROID_HOME/tools/bin/sdkmanager
#

# Accept licences
# src http://vgaidarji.me/blog/2017/05/31/automatically-accept-android-sdkmanager-licenses/

# Устанавливаем правильный JAVA_HOME
# Автоматически находим путь к Java
if [ -z "$JAVA_HOME" ]; then
    # Пытаемся найти Java
    JAVA_PATH=$(update-alternatives --list java 2>/dev/null | head -n 1)
    if [ -n "$JAVA_PATH" ]; then
        export JAVA_HOME=$(dirname $(dirname "$JAVA_PATH"))
    else
        # Проверяем стандартные пути
        if [ -d "/usr/lib/jvm/java-8-openjdk-amd64" ]; then
            export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"
        elif [ -d "/opt/jdk/jdk1.8.0_131" ]; then
            export JAVA_HOME="/opt/jdk/jdk1.8.0_131"
        else
            echo "ERROR: Java not found and JAVA_HOME not set!"
            exit 1
        fi
    fi
fi

for I in "platforms;android-10" \
         "build-tools;24.0.3"; do
    echo "Trying to update with tools/bin/sdkmanager: " $I
    yes | sdkmanager $I
done

sdkmanager --update
yes | sdkmanager --licenses
