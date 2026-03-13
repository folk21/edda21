# Build from repo root:
# docker build -t edda21/api-gateway -f platform/api-gateway/Dockerfile .

FROM eclipse-temurin:21-jre AS extractor
WORKDIR /workspace
ARG JAR_FILE=platform/api-gateway/build/libs/*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=extractor /workspace/dependencies/ ./
COPY --from=extractor /workspace/snapshot-dependencies/ ./
COPY --from=extractor /workspace/spring-boot-loader/ ./
COPY --from=extractor /workspace/application/ ./
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
