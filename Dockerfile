#
# Copyright 2019 The OpenZipkin Authors
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
# in compliance with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License
# is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
# or implied. See the License for the specific language governing permissions and limitations under
# the License.
#

FROM openjdk:8

ARG SPLUNK_STORAGE_VERSION=0.1.0-SNAPSHOT

ENV ZIPKIN_REPO https://repo1.maven.org/maven2
ENV ZIPKIN_VERSION 2.14.0
ENV ZIPKIN_LOGGING_LEVEL INFO

# Use to set heap, trust store or other system properties.
ENV JAVA_OPTS -Djava.security.egd=file:/dev/./urandom
# Add environment settings for supported storage types
ENV STORAGE_TYPE=splunk

WORKDIR /zipkin

RUN curl -SL $ZIPKIN_REPO/org/apache/zipkin/zipkin-server/$ZIPKIN_VERSION/zipkin-server-${ZIPKIN_VERSION}-exec.jar > zipkin.jar

ADD storage/target/zipkin-storage-splunk-${SPLUNK_STORAGE_VERSION}.jar zipkin-storage-splunk.jar
ADD autoconfigure/target/zipkin-autoconfigure-storage-splunk-${SPLUNK_STORAGE_VERSION}-module.jar zipkin-autoconfigure-storage-splunk.jar

EXPOSE 9410 9411

CMD exec java \
    ${JAVA_OPTS} \
    -Dloader.path='zipkin-storage-splunk.jar,zipkin-autoconfigure-storage-splunk.jar' \
    -Dspring.profiles.active=${STORAGE_TYPE} \
    -cp zipkin.jar \
    org.springframework.boot.loader.PropertiesLauncher