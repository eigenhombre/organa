.PHONY: all docker test uberjar

BINPATH = ${HOME}/bin
JARPATH = target/uberjar/organa.jar

${JARPATH}: src/organa/*.clj project.clj resources/*
	lein uberjar

uberjar:
	make ${JARPATH}

clean:
	rm -rf target/*

install:
	mkdir -p ${BINPATH}
	cp ${JARPATH} ${BINPATH}

all: test uberjar doc install

test:
	lein do kaocha, bikeshed, kibit, eastwood
