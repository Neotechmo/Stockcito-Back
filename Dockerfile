# syntax=docker/dockerfile:1

FROM tomcat:11.0-jdk21-temurin AS build

RUN sed -i 's|http://ports.ubuntu.com|https://ports.ubuntu.com|g; s|http://archive.ubuntu.com|https://archive.ubuntu.com|g' /etc/apt/sources.list /etc/apt/sources.list.d/* 2>/dev/null || true \
    && apt-get update \
    && apt-get install -y --no-install-recommends ant curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN mkdir -p /opt/stockcito-libs && cd /opt/stockcito-libs \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/hk2/external/aopalliance-repackaged/4.0.0-M3/aopalliance-repackaged-4.0.0-M3.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-api/4.0.0-M3/hk2-api-4.0.0-M3.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-locator/4.0.0-M3/hk2-locator-4.0.0-M3.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-utils/4.0.0-M3/hk2-utils-4.0.0-M3.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.19.1/jackson-annotations-2.19.1.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.19.1/jackson-core-2.19.1.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.19.1/jackson-databind-2.19.1.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/com/fasterxml/jackson/module/jackson-module-jakarta-xmlbind-annotations/2.19.1/jackson-module-jakarta-xmlbind-annotations-2.19.1.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/jakarta/activation/jakarta.activation-api/2.1.3/jakarta.activation-api-2.1.3.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/jakarta/annotation/jakarta.annotation-api/3.0.0/jakarta.annotation-api-3.0.0.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/jakarta/validation/jakarta.validation-api/3.1.0/jakarta.validation-api-3.1.0.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/jakarta/ws/rs/jakarta.ws.rs-api/4.0.0/jakarta.ws.rs-api-4.0.0.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/jakarta/xml/bind/jakarta.xml.bind-api/4.0.2/jakarta.xml.bind-api-4.0.2.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/javassist/javassist/3.30.2-GA/javassist-3.30.2-GA.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/jersey/core/jersey-client/4.0.0/jersey-client-4.0.0.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/jersey/core/jersey-common/4.0.0/jersey-common-4.0.0.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/jersey/containers/jersey-container-servlet/4.0.0/jersey-container-servlet-4.0.0.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/jersey/ext/jersey-entity-filtering/4.0.0/jersey-entity-filtering-4.0.0.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/jersey/inject/jersey-hk2/4.0.0/jersey-hk2-4.0.0.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/jersey/media/jersey-media-json-jackson/4.0.0/jersey-media-json-jackson-4.0.0.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/jersey/core/jersey-server/4.0.0/jersey-server-4.0.0.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.31/mysql-connector-j-8.0.31.jar \
    && curl -fsSLO https://repo1.maven.org/maven2/org/glassfish/hk2/osgi-resource-locator/3.0.0/osgi-resource-locator-3.0.0.jar

COPY . .
RUN ant docker-war -Ddocker.libs.dir=/opt/stockcito-libs

FROM tomcat:11.0-jdk21-temurin AS runtime

RUN sed -i 's|http://ports.ubuntu.com|https://ports.ubuntu.com|g; s|http://archive.ubuntu.com|https://archive.ubuntu.com|g' /etc/apt/sources.list /etc/apt/sources.list.d/* 2>/dev/null || true \
    && apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/dist/Stockcito.war /usr/local/tomcat/webapps/Stockcito.war

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/Stockcito/api/v1/health || exit 1

CMD ["catalina.sh", "run"]
