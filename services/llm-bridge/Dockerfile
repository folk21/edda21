# Build from repo root:
# docker build -t edda21/llm-bridge -f services/llm-bridge/Dockerfile .

FROM eclipse-temurin:21-jre AS extractor
WORKDIR /workspace
ARG JAR_FILE=services/llm-bridge/build/libs/*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=extractor /workspace/dependencies/ ./
COPY --from=extractor /workspace/snapshot-dependencies/ ./
COPY --from=extractor /workspace/spring-boot-loader/ ./
COPY --from=extractor /workspace/application/ ./
EXPOSE 8098
# По умолчанию используем stub-режим, ключи не нужны
ENV GENERATOR_MODE=stub \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
# Для OpenAI раскомментируй/переопредели в compose:
# ENV SPRING_AI_OPENAI_API_KEY=...
# ENV SPRING_AI_MODEL=gpt-4o-mini
ENTRYPOINT ["sh","-c","java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
