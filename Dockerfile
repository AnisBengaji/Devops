FROM eclipse-temurin:17-jre-jammy

# Mise à jour des paquets OS (corrige des CVE de l'image de base) + curl pour le healthcheck
RUN apt-get update && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Utilisateur non-root (Checkov CKV_DOCKER_3)
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app
COPY target/*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8089

# Healthcheck (Checkov CKV_DOCKER_2)
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8089/ || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
