{% set PAYARA_LIBS='/opt/payara/glassfish/domains/domain1/lib' %}

maven:
  pkg:
    - installed

download-shared-libs:
  cmd.run:
    - name: /usr/bin/mvn dependency:get -DrepoUrl=http://repo1.maven.org/maven2 -Dartifact=org.ektorp:org.ektorp:1.4.4:jar
    - unless:
      - ls /root/.m2/repository/org/ektorp/org.ektorp/1.4.4

copy-shared-libs:
  cmd.run:
    - name: mvn dependency:copy-dependencies -DoutputDirectory={{ PAYARA_LIBS }}
    - cwd: /srv/salt/files/maven
    - creates: {{ PAYARA_LIBS }}/org.ektorp-1.4.4.jar

checkout-status-microservice:
  file.directory:
    - name: /opt/microservices
    - user: root
    - group: root
    - dir_mode: 755
  svn.export:
    - name: {{ pillar['svn'] }}/java/Status
    - target: /opt/microservices/status
    - unless: ls /opt/microservices/status

restart-payara-on-new-libs:
  cmd.run:
    - name: systemctl restart payara
    - onchanges:
      - copy-shared-libs
