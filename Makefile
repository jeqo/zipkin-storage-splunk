.PHONY: all
all: license-format clean-build dc-build dc-up

MVN := ./mvnw

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

.PHONY: run-local
run-local:
	java \
    -Dloader.path='storage/target/zipkin-storage-splunk-0.1.0-SNAPSHOT.jar,autoconfigure/target/zipkin-autoconfigure-storage-splunk-0.1.0-SNAPSHOT.jar' \
    -Dspring.profiles.active=splunk \
    -cp zipkin.jar \
    org.springframework.boot.loader.PropertiesLauncher \
    -Dzipkin.storage.splunk.host=localhost\
    -Dzipkin.storage.splunk.port=8089
