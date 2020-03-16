#!make
SHELL ?= /bin/bash
JAR_VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
JAR_FILE := mn2pdf-$(JAR_VERSION).jar

all: target/$(JAR_FILE)

target/$(JAR_FILE):
	mvn -DskipTests clean package shade:shade

# src/test/resources/pdf_fonts_config.xml
test: target/$(JAR_FILE)
	mvn test surefire-report:report

#src/test/resources/pdf_fonts_config.xml: src/test/resources/pdf_fonts_config.xml.in
#	envsubst < src/test/resources/pdf_fonts_config.xml.in > src/test/resources/pdf_fonts_config.xml

clean:
	rm -rf target
	rm -rf tests/*.pdf
#src/test/resources/pdf_fonts_config.xml

.PHONY: all clean test
