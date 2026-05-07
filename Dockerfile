# Etapa 1: Construcción (build)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cachear dependencias antes de copiar el código
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar fuentes y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Runtime (sólo JRE + el .jar)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Encoding UTF-8 para tildes/eñes en outputs y logs
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

# Correr como usuario no-root (best practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
