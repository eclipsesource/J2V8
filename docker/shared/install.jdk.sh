
if java_loc="$(type -p javac)" || [ -z "$java_loc" ]; then
    echo "JDK already installed, skipping installation..."
    echo "Existing JDK location: "$java_loc
    exit 0
fi

# sources:
# - https://www.mkyong.com/java/how-to-install-oracle-jdk-8-on-debian/

echo "Preparing JDK..."
curl -L -C - -b "oraclelicense=accept-securebackup-cookie" -O http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-linux-x64.tar.gz
mkdir -p /opt/jdk
tar x -C /opt/jdk -f jdk-8u131-linux-x64.tar.gz

update-alternatives --install /usr/bin/java java /opt/jdk/jdk1.8.0_131/bin/java 100
update-alternatives --install /usr/bin/javac javac /opt/jdk/jdk1.8.0_131/bin/javac 100
update-alternatives --install /usr/bin/javah javah /opt/jdk/jdk1.8.0_131/bin/javah 100
