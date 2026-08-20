# Build multi-étapes : compilation avec Maven, exécution avec un JRE minimal
# (l'image finale n'embarque pas Maven ni le JDK complet — plus légère, démarrage plus rapide sur Railway)

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Railway fournit la variable PORT dynamiquement — on la relaie vers SERVER_PORT
# attendu par application.yml (server.port: ${SERVER_PORT:8080}).
ENV SERVER_PORT=${PORT:-8080}
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]