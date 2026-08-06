FROM eclipse-temurin:25-jdk

WORKDIR /workspace
# DockerCodeExecutor also passes --user 1000:1000 when starting the container.
# The base image already has a group with GID 1000, so no named user is needed.
USER 1000:1000
