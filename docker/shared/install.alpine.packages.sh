
echo "Preparing Alpine packages..."
apk add --update --no-cache \
	git \
	unzip \
	gcc \
	g++ \
	curl \
	file \
	python \
	make \
	cmake \
	wget \
	supervisor \
	bash \
	linux-headers
