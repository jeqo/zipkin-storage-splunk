.PHONY: all
all: license-format clean-build

.PHONY: all-docker
all-docker: license-format clean-build dc-build dc-up

.PHONY: all-local
all-local: license-format clean-build dc-up-splunk run-local

OPEN := 'xdg-open'
MVN := './mvnw'
VERSION := '0.1.0-SNAPSHOT'

.PHONY: license-format
license-format:
	$(MVN) com.mycila:license-maven-plugin:3.0:format

.PHONY: clean-build
clean-build:
	$(MVN) clean install

.PHONY: dc-build
dc-build:
	docker-compose build

.PHONY: dc-up
dc-up:
	docker-compose up -d

.PHONY: dc-down
dc-down:
	docker-compose down

.PHONY: dc-up-splunk
dc-up-splunk:
	docker-compose up -d splunk

.PHONY: download-zipkin
download-zipkin:
	curl -sSL https://zipkin.apache.org/quickstart.sh | bash -s

.PHONY: run-local
run-local:
	SPLUNK_USERNAME=admin SPLUNK_PASSWORD=welcome1 STORAGE_TYPE=splunk \
	java \
    -Dloader.path='storage/target/zipkin-storage-splunk-${VERSION}.jar,autoconfigure/target/zipkin-autoconfigure-storage-splunk-${VERSION}-module.jar' \
    -Dspring.profiles.active=splunk \
    -cp zipkin.jar \
    org.springframework.boot.loader.PropertiesLauncher

.PHONY: zipkin-test
zipkin-test:
	curl -s https://raw.githubusercontent.com/openzipkin/zipkin/master/zipkin-ui/testdata/netflix.json | \
	curl -X POST -s localhost:9411/api/v2/spans -H'Content-Type: application/json' -d @- ; \
	${OPEN} 'http://localhost:9411/zipkin/?lookback=custom&startTs=1'
