{% set PAYARA_LIBS='/opt/payara/glassfish/domains/domain1/lib' %}
{% set MIC_DIR='/opt/microservices' %}
{% set DEPLOY_DIR='/opt/payara/glassfish/domains/domain1/autodeploy' %}

maven:
  pkg:
    - installed

copy-maven-libs:
  cmd.run:
    - name: mvn dependency:copy-dependencies -DoutputDirectory={{ PAYARA_LIBS }}
    - cwd: /srv/salt/files/maven
    - creates: {{ PAYARA_LIBS }}/jersey-media-json-jackson-2.26.jar

{{ MIC_DIR }}:
  file.directory:
    - user: root
    - group: root
    - dir_mode: 755

install-javalite-http:
  svn.export:
    - name: https://github.com/realulim/javalite-http/trunk
    - target: {{ MIC_DIR }}/javalite-http
    - unless: ls {{ MIC_DIR }}/javalite-http
  cmd.run:
    - name: bash -c 'mvn package install && cp {{ MIC_DIR }}/javalite-http/target/javalite-http.jar {{ PAYARA_LIBS }}'
    - cwd: {{ MIC_DIR }}/javalite-http
    - creates {{ MIC_DIR }}/javalite-http/target/javalite-http.jar

{% for LIB_NAME in ['LoadBalancer', 'ReST'] %}

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
      - install-javalite-http
      - copy-maven-libs
      - copy-LoadBalancer-lib
      - copy-ReST-lib

{% for MIC_SRV_NAME in ['Status', 'Users', 'Sessions', 'Maintenance'] %}

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
