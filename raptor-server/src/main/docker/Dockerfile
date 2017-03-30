FROM openjdk:8-jdk-alpine

LABEL maintainer "wcai@maxleap.com"

ENV RAPTOR_HOME="/usr/local/lib/raptor" RAPTOR_LOG_DIR="/mnt/sandbox"

WORKDIR ${RAPTOR_HOME}

EXPOSE 8080 1935

ENTRYPOINT [\
"java",\
"-XX:+UseG1GC",\
"-Djava.net.preferIPv4Stack=true",\
"-Dlog4j.configurationFile=file:log4j2.xml",\
"-Draptor.server.www=www",\
"-jar",\
"raptor-server-jar-with-dependencies.jar"\
]

ADD ./ ./