# Build from repo root:
# docker build -t edda21/service-registry -f platform/service-registry/Dockerfile .

FROM eclipse-temurin:21-jre AS extractor
WORKDIR /workspace
ARG JAR_FILE=platform/service-registry/build/libs/*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre
WORKDIR /app
# Spring Boot layered jar layout
COPY --from=extractor /workspace/dependencies/ ./
COPY --from=extractor /workspace/snapshot-dependencies/ ./
COPY --from=extractor /workspace/spring-boot-loader/ ./
COPY --from=extractor /workspace/application/ ./
EXPOSE 8761
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
