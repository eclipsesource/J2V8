
echo "Preparing Debian packages..."
apt-get -qq update && \
	DEBIAN_FRONTEND=noninteractive apt-get -qq install -y \
	git \
	unzip \
	gcc gcc-multilib \
	g++ g++-multilib \
	python3 \
	python3-venv \
	python3-distutils \
	curl \
	file \
	make \
	wget \
	supervisor \

# Ensure legacy scripts find python
ln -sf /usr/bin/python3 /usr/bin/python
