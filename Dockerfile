FROM frolvlad/alpine-oraclejdk8:slim
VOLUME /tmp
ADD target/REISystems-USCIS-ODOS2-user-0.0.1.jar app.jar
ENV JAVA_OPTS=""
RUN wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.jar
ADD newrelic/newrelic.yml .
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -javaagent:newrelic.jar -jar /app.jar" ]
EXPOSE 8080
USER 1001
