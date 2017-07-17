{% set MVN_NAME='apache-maven' %}
{% set MVN_VERSION='3.5.0' %}
{% set MVN_BIN='/opt/maven/bin/mvn' %}
{% set MVN_REPO='http://repo.maven.apache.org/maven2/' %}
{% set PAYARA_LIBS='/opt/payara/glassfish/domains/domain1/lib' %}

download-and-extract-maven:
  cmd.run:
    - name: curl -o /opt/{{ MVN_NAME }}-{{ MVN_VERSION }}-bin.tar.gz -L http://www.us.apache.org/dist/maven/maven-3/{{ MVN_VERSION }}/binaries/{{ MVN_NAME }}-{{ MVN_VERSION }}-bin.tar.gz
    - creates: /opt/{{ MVN_NAME }}-{{ MVN_VERSION }}-bin.tar.gz
  archive.extracted:
    - name: /opt
    - source: /opt/{{ MVN_NAME }}-{{ MVN_VERSION }}-bin.tar.gz
    - archive_format: tar
    - creates: /opt/{{ MVN_NAME }}-{{ MVN_VERSION }}
  file.symlink:
    - name: /opt/maven
    - target: /opt/{{ MVN_NAME }}-{{ MVN_VERSION }}

download-shared-libs:
  cmd.run:
    - name: mvn dependency:get -DrepoUrl={{ MVN_REPO }} -Dartifact=org.ektorp:org.ektorp:1.4.4.RELEASE:jar -Dtransitive=true -Ddest={{ PAYARA_LIBS }}
    - creates: {{ PAYARA_LIBS }}/org.ektorp-1.4.4.jar
