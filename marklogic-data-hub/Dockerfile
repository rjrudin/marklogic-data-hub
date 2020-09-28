FROM adoptopenjdk/openjdk11:alpine-jre
ARG JAR_FILE=build/libs/marklogic-data-hub-5.3-SNAPSHOT-client.jar
WORKDIR /usr/local/runme
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","app.jar"]
