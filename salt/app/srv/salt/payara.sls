{% set PAYARA_DIR='/opt/payara' %}
{% set DOMAIN_DIR='/opt/payara/glassfish/domains/domain1' %}
{% set PAYARA_VERSION='4.1.2.173' %}
{% set PAYARA_ARTIFACT='payara-4.1.2.173.zip' %}
{% set PWD_FILE='/root/.payara' %}
{% set INITIAL_PWD_FILE='/root/.payara-initial' %}
{% set ASADMIN='/opt/payara/bin/asadmin' %}

install_payara_pkgs:
  pkg.installed:
    - pkgs:
      - java-1.8.0-openjdk.x86_64

install-systemctl-unitfile:
   file.managed:
    - name: /usr/lib/systemd/system/payara.service
    - source: salt://files/payara/payara.service

download-payara:
  cmd.run:
    - name: curl -o /root/download/{{ PAYARA_ARTIFACT }} -L https://search.maven.org/remotecontent?filepath=fish/payara/distributions/payara/{{ PAYARA_VERSION }}/{{ PAYARA_ARTIFACT }}
    - creates: /root/download/{{ PAYARA_ARTIFACT }}

# If there is a new Payara zip file:
#  stop Payara, remove old installation, extract new zip file, rename extracted directory sensibly
payara-installed:
  service.dead:
    - name: payara
    - onchanges:
      - download-payara
  cmd.run:
    - name: rm -rf {{ PAYARA_DIR }}
    - onchanges:
      - download-payara
  archive.extracted:
    - name: /opt
    - source: /root/download/{{ PAYARA_ARTIFACT }}
    - archive_format: zip
    - unless: ls {{ PAYARA_DIR }}
  file.rename:
    - source: /opt/payara41
    - name: /opt/payara-{{ PAYARA_VERSION }}
    - onlyif: ls /opt/payara41

create-payara-symlink:
  file.symlink:
    - name: {{ PAYARA_DIR }}
    - target: payara-{{ PAYARA_VERSION }}

create-payara-server.log-symlink:
  file.symlink:
    - name: /opt/logs/server.log
    - target: /opt/payara/glassfish/domains/domain1/logs/server.log

payara-running:
  service.running:
    - name: payara
    - enable: true
    - require:
       - payara-installed
    - listen:
      - file: /usr/lib/systemd/system/payara.service

make-asadmin-executable:
  cmd.run:
    - name: chmod 755 {{ ASADMIN }}
    - onchanges:
      - payara-installed

create-initial-admin-password-file:
  cmd.run:
    - name: printf 'AS_ADMIN_PASSWORD=\nAS_ADMIN_NEWPASSWORD={{ pillar['asadmin-password'] }}\n' > {{ INITIAL_PWD_FILE }}
    - unless: grep {{ pillar['asadmin-password'] }} {{ INITIAL_PWD_FILE }}

set-admin-password:
  cmd.run:
    - name: {{ ASADMIN }} --interactive=false --user admin --passwordfile={{ INITIAL_PWD_FILE }} change-admin-password
    - require:
      - make-asadmin-executable
      - payara-running
    - onchanges:
      - create-initial-admin-password-file
      - payara-installed

create-admin-password-file:
  cmd.run:
    - name: printf 'AS_ADMIN_PASSWORD={{ pillar['asadmin-password'] }}\nAS_ADMIN_ALIASPASSWORD={{ pillar['couchdb-admin-password'] }}\n' > {{ PWD_FILE }}
    - onchanges:
      - set-admin-password

# SecureAdmin must be activated for access to the admin console
enable-secure-admin:
  cmd.run:
    - name: {{ ASADMIN }} --interactive=false --user admin --passwordfile={{ PWD_FILE }} enable-secure-admin
    - onchanges:
      - create-admin-password-file

payara-configured:
  file.managed:
    - name: /opt/payara/bin/domain-config.sh
    - source: salt://files/payara/domain-config.sh
    - template: jinja
    - mode: 755
  cmd.run:
    - name: /opt/payara/bin/domain-config.sh {{ ASADMIN }} {{ PWD_FILE }} {{ pillar['ssldomain'] }} {{ pillar['sslhost'] }} {{ pillar['cfns'] }}
    - shell: /bin/bash
    - onchanges:
      - enable-secure-admin
      - file: /opt/payara/bin/domain-config.sh

/opt/payara/bin/jks_import_pem.sh:
  file.managed:
    - source: salt://files/payara/jks_import_pem.sh
    - mode: 755

import-certs-to-truststore:
  cmd.run:
    - name: /opt/payara/bin/jks_import_pem.sh /root/.acme.sh/{{ pillar['ssldomain'] }}/fullchain.cer changeit {{ DOMAIN_DIR }}/config/cacerts.jks
    - onchanges:
      - enable-secure-admin
      - file: /opt/payara/bin/jks_import_pem.sh

{{ DOMAIN_DIR }}/docroot/index.html:
  file.replace:
    - pattern: "<h1>Hello from Payara - your server is now running!</h1>"
    - repl: |
        <h1>{{ pillar['hostname'] }}</h1>

{{ DOMAIN_DIR }}/config/domain.xml:
  file.replace:
    - pattern: "<hazelcast-runtime-configuration.*</hazelcast-runtime-configuration>"
    - repl: <hazelcast-runtime-configuration hazelcast-configuration-file="hazelcast.xml" start-port="5701" enabled="true"></hazelcast-runtime-configuration>

{{ DOMAIN_DIR }}/config/hazelcast.xml:
  file.managed:
    - source: salt://files/payara/hazelcast.xml
    - template: jinja

restart-payara-on-config-changes:
  cmd.run:
    - name: systemctl restart payara
    - onchanges:
      - set-admin-password
      - file: {{ DOMAIN_DIR }}/config/domain.xml
      - file: {{ DOMAIN_DIR }}/config/hazelcast.xml
    - require:
      - enable-secure-admin
