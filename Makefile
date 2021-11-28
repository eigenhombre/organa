.PHONY: all docker test uberjar lint ancient-advisory

BINPATH = ${HOME}/bin
JARPATH = target/uberjar/organa.jar

all: test lint ancient-advisory uberjar doc install

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

lint:
	lein do bikeshed, kibit, eastwood

ancient-advisory:
	lein ancient && echo 'up to date!' || echo 'WARNING: updates needed!'

test:
	lein kaocha

docker:
	docker build --progress tty -t organa .

docker-quiet:
	docker build --quiet -t organa .
