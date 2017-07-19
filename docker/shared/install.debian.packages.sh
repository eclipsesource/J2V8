
echo "Preparing Debian packages..."
apt-get -qq update && \
	DEBIAN_FRONTEND=noninteractive apt-get -qq install -y \
	git \
	unzip \
	gcc gcc-multilib \
	g++ g++-multilib \
	curl \
	file \
	python \
	make \
	wget \
	supervisor \
	execstack
