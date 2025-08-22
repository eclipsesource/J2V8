if command -v javac >/dev/null 2>&1; then
    echo "JDK already installed: $(javac -version 2>&1)"
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
    exit 0
fi

echo "Installing AdoptOpenJDK 8..."

# Устанавливаем yes и wget
apt-get update && apt-get install -y yes wget

# Скачиваем AdoptOpenJDK 8
wget -q https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u392-b08/OpenJDK8U-jdk_x64_linux_hotspot_8u392b08.tar.gz
tar -xzf OpenJDK8U-jdk_x64_linux_hotspot_8u392b08.tar.gz -C /opt
mv /opt/jdk8u392-b08 /opt/jdk8

export JAVA_HOME=/opt/jdk8
export PATH="$JAVA_HOME/bin:$PATH"

# Используем yes для автоматического подтверждения
yes "" | update-alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 100
yes "" | update-alternatives --install /usr/bin/javac javac $JAVA_HOME/bin/javac 100
yes "" | update-alternatives --install /usr/bin/javah javah $JAVA_HOME/bin/javah 100

rm OpenJDK8U-jdk_x64_linux_hotspot_8u392b08.tar.gz

echo "AdoptOpenJDK 8 installation completed"
java -version
javac -version