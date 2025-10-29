
if java_loc="$(type -p javac)" || [ -z "$java_loc" ]; then
    echo "JDK already installed, skipping installation..."
    echo "Existing JDK location: "$java_loc
    exit 0
fi

# sources:
# - https://www.mkyong.com/java/how-to-install-oracle-jdk-8-on-debian/

echo "Preparing JDK..."
apt-get -qq update &&   DEBIAN_FRONTEND=noninteractive apt-get -qq install -y --no-install-recommends     openjdk-17-jdk     openjdk-17-jre  && rm -rf /var/lib/apt/lists/*

update-alternatives --set java $(update-alternatives --list java | grep java-17 | head -n 1)
update-alternatives --set javac $(update-alternatives --list javac | grep java-17 | head -n 1)
