# Build from repo root:
# docker build -t edtech/config-server -f platform/config-server/Dockerfile .

FROM eclipse-temurin:21-jre AS extractor
WORKDIR /workspace
ARG JAR_FILE=platform/config-server/build/libs/*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=extractor /workspace/dependencies/ ./
COPY --from=extractor /workspace/snapshot-dependencies/ ./
COPY --from=extractor /workspace/spring-boot-loader/ ./
COPY --from=extractor /workspace/application/ ./
EXPOSE 8888
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
# укажем дефолт для native repository; в compose смонтируем /config-repo
ENV SPRING_PROFILES_ACTIVE=docker \
    SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS=/config-repo
ENTRYPOINT ["sh","-c","java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
