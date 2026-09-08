# ============================================================
# Etape 1 : build - compile le projet avec Maven dans un conteneur
# jetable (le JDK complet n'est jamais present dans l'image finale).
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copie du pom.xml seul d'abord : permet a Docker de mettre en cache la
# resolution des dependances Maven tant que le pom.xml ne change pas,
# meme si le code source change (build bien plus rapide en iteration).
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ============================================================
# Etape 2 : runtime - image finale minimale, uniquement le JRE et le jar.
# ============================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

# Utilisateur non-root : bonne pratique de securite pour ne jamais faire
# tourner l'application avec les privileges root dans le conteneur.
RUN addgroup -S xaamxaam && adduser -S xaamxaam -G xaamxaam

WORKDIR /app
COPY --from=build /app/target/xaamxaam-backend.jar app.jar

USER xaamxaam

# Railway/Render injectent la variable PORT ; l'application l'ecoute via
# server.port: ${PORT:...} (voir application.yml). Le port par defaut ici
# ne sert qu'en local sans variable PORT definie.
EXPOSE 8080

# Verifie periodiquement que l'application repond, via le endpoint public
# /actuator/health (voir SecurityConfig et dependance spring-boot-starter-actuator).
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health | grep -q '"status":"UP"' || exit 1

# -XX:MaxRAMPercentage adapte automatiquement le heap a la memoire allouee
# au conteneur, plus fiable que de fixer -Xmx en dur sur des plateformes
# ou la memoire disponible depend du plan choisi (Railway/Render).
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -jar app.jar"]
