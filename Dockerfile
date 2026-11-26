# Builds the two images a deployment runs, from source, so anyone can build them from a clone:
#   docker build --target app -t naturalist-app .
#   docker build --target db-init -t naturalist-db-init .

FROM maven:3.9-eclipse-temurin-26-noble AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -DskipTests package -pl apps/management-console,apps/test-db-seeder -am \
    && mkdir /out \
    && cp apps/management-console/target/management-console-*.jar /out/app.jar \
    && cp apps/test-db-seeder/target/test-db-seeder-*-jar-with-dependencies.jar /out/db-init.jar

# Runtime JDK is pinned by full Temurin version, so a JDK change arrives as a visible Dependabot PR.
# 25.0.5.1_2 is the build Adoptium names as fixing CVE-2026-114938 (Java4Shell):
# https://adoptium.net/news/2026/11/temurin-november-2026-cspu-java4shell

# Creates the schema and loads the public catalog. See DeploymentDbInitializer.
FROM eclipse-temurin:26.0.2.1_1-jre-noble AS db-init
RUN useradd --system --uid 10001 --create-home naturalist \
    && mkdir -p /data/images && chown -R naturalist /data
COPY --from=build /out/db-init.jar /opt/naturalist/db-init.jar
USER naturalist
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -cp /opt/naturalist/db-init.jar com.naturalist.seeder.DeploymentDbInitializer"]

# The packaged web application. Listed last so a plain `docker build .` produces it.
FROM eclipse-temurin:26.0.2.1_1-jre-noble AS app
RUN useradd --system --uid 10001 --create-home naturalist \
    && mkdir -p /data/images && chown -R naturalist /data
COPY --from=build /out/app.jar /opt/naturalist/app.jar
USER naturalist
WORKDIR /home/naturalist
ENV NATURALIST_IMAGES_ROOT=/data/images
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75 $JAVA_OPTS -jar /opt/naturalist/app.jar"]
