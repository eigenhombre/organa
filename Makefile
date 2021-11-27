.PHONY: all docker test uberjar

BINPATH = ${HOME}/bin
JARPATH = target/uberjar/organa.jar

all: test uberjar doc install

${JARPATH}: src/organa/*.clj project.clj resources/*
	lein uberjar

uberjar:
	make ${JARPATH}

clean:
	rm -rf target/*

install:
	mkdir -p ${BINPATH}
	cp ${JARPATH} ${BINPATH}

doc:
	lein codox

test:
	lein do kaocha, bikeshed, kibit, eastwood

docker:
	docker build --progress tty -t organa .

docker-quiet:
	docker build --quiet -t organa .
