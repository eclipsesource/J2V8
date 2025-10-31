
#!/bin/bash
set -e  # Exit immediately if any command fails

echo "Preparing Debian packages..."
apt-get -qq update && \
	DEBIAN_FRONTEND=noninteractive apt-get -qq install -y \
	git \
	unzip \
	gcc \
	g++ \
	python3 \
	python3-venv \
	python3-distutils \
	curl \
	file \
	make \
	wget \
	supervisor

# Ensure legacy scripts find python
ln -sf /usr/bin/python3 /usr/bin/python

# Verify critical tools were installed
echo "Verifying package installation..."
which wget || (echo "ERROR: wget not installed" && exit 1)
which git || (echo "ERROR: git not installed" && exit 1)
which python3 || (echo "ERROR: python3 not installed" && exit 1)
echo "Package installation verified successfully."
