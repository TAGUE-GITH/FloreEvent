# ---- Etape 1 : compilation avec le JDK et Maven ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Les dependances sont telechargees dans une couche separee :
# elles restent en cache tant que le pom.xml ne change pas
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q package -DskipTests

# ---- Etape 2 : image finale, legere, avec seulement le JRE ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# L'application ne tourne pas en root (bonne pratique de securite)
RUN useradd --system --create-home spring
USER spring

COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
