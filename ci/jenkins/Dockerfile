# Only used when eventpro-jenkins:lts is absent locally.
# Reproduces the cached image: same base, plugins baked, config mounted at runtime.
FROM jenkins/jenkins:lts-jdk21

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

USER jenkins