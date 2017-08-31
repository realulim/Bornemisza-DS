{% set PAYARA_LIBS='/opt/payara/glassfish/domains/domain1/lib' %}
{% set MIC_DIR='/opt/microservices' %}
{% set DEPLOY_DIR='/opt/payara/glassfish/domains/domain1/autodeploy' %}

maven:
  pkg:
    - installed

download-ektorp-lib:
  cmd.run:
    - name: /usr/bin/mvn dependency:get -DrepoUrl=http://repo1.maven.org/maven2 -Dartifact=org.ektorp:org.ektorp:1.4.4:jar
    - unless:
      - ls /root/.m2/repository/org/ektorp/org.ektorp/1.4.4

download-javalite-lib:
  cmd.run:
    - name: /usr/bin/mvn dependency:get -DrepoUrl=http://repo1.maven.org/maven2 -Dartifact=org.javalite:javalite-common:1.4.13:jar
    - unless:
      - ls /root/.m2/repository/org/javalite/javalite-common/1.4.13

copy-thirdparty-libs:
  cmd.run:
    - name: mvn dependency:copy-dependencies -DoutputDirectory={{ PAYARA_LIBS }}
    - cwd: /srv/salt/files/maven
    - creates: {{ PAYARA_LIBS }}/org.ektorp-1.4.4.jar

{{ MIC_DIR }}:
  file.directory:
    - user: root
    - group: root
    - dir_mode: 755

{% for LIB_NAME in ['CouchDB', 'ReST'] %}

checkout-{{ LIB_NAME }}-lib:
  svn.export:
    - name: {{ pillar['svn'] }}/java/{{ LIB_NAME }}
    - target: {{ MIC_DIR }}/{{ LIB_NAME }}
    - unless: ls {{ MIC_DIR }}/{{ LIB_NAME }}

build-{{ LIB_NAME }}-lib:
  cmd.run:
    - name: mvn package install
    - cwd: {{ MIC_DIR }}/{{ LIB_NAME }}
    - creates: {{ MIC_DIR }}/{{ LIB_NAME }}/target/{{ LIB_NAME }}.jar

copy-{{ LIB_NAME }}-lib:
  cmd.run:
    - name: cp {{ MIC_DIR }}/{{ LIB_NAME }}/target/{{ LIB_NAME }}.jar {{ PAYARA_LIBS }}
    - onchanges:
      - build-{{ LIB_NAME }}-lib

{% endfor %}

restart-payara-on-new-libs:
  cmd.run:
    - name: systemctl restart payara
    - onchanges:
      - copy-thirdparty-libs
      - copy-CouchDB-lib
      - copy-ReST-lib

{% for MIC_SRV_NAME in ['Status', 'Users'] %}

checkout-{{ MIC_SRV_NAME }}-microservice:
  svn.export:
    - name: {{ pillar['svn'] }}/java/{{ MIC_SRV_NAME }}
    - target: {{ MIC_DIR }}/{{ MIC_SRV_NAME }}
    - unless: ls {{ MIC_DIR }}/{{ MIC_SRV_NAME }}

build-{{ MIC_SRV_NAME }}-microservice:
  cmd.run:
    - name: mvn package
    - cwd: {{ MIC_DIR }}/{{ MIC_SRV_NAME }}
    - creates: {{ MIC_DIR }}/{{ MIC_SRV_NAME }}/target/{{ MIC_SRV_NAME }}.war

deploy-{{ MIC_SRV_NAME }}-microservice:
  cmd.run:
    - name: cp {{ MIC_DIR }}/{{ MIC_SRV_NAME }}/target/{{ MIC_SRV_NAME }}.war {{ DEPLOY_DIR }}
    - onchanges:
      - build-{{ MIC_SRV_NAME }}-microservice

{% endfor %}
