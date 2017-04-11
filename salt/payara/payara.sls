{% set PAYARA_INSTALL_DIR='/opt/payara' %}
{% set PAYARA_VERSION='4.1.1.171.1' %}
{% set PAYARA_ARTIFACT='payara-{{ PAYARA_VERSION }}.zip' %}
{% set ASADMIN='/opt/payara/bin/asadmin' %}
{% set ADMIN_PWD='/root/.payara' %}
{% set TMP_ADMIN_PWD='/tmp/payara-pwd' %}

download-payara:
  cmd.run:
    - name: curl -o /opt/{{ PAYARA_ARTIFACT }} -L https://search.maven.org/remotecontent?filepath=fish/payara/distributions/payara/{{ PAYARA_VERSION }}/{{ PAYARA_ARTIFACT }}
    - creates: /opt/{{ PAYARA_ARTIFACT }}

install-systemctl-unitfile:
   file.managed:
    - name: /usr/lib/systemd/system/payara.service
    - source: salt://files/payara/payara.service
   module.run:
    - name: service.systemctl_reload   
    - onchanges:
      - file: /usr/lib/systemd/system/payara.service

# If there is a new Payara zip file:
#  stop Payara
#  remove old installation
#  extract new zip file and symlink it
payara-installed:
  service.dead:
    - name: payara
    - onchanges:
      - download-payara
  cmd.run:
    - name: rm -rf {{ PAYARA_INSTALL_DIR }}
    - onchanges:
      - download-payara
  archive.extracted:
    - name: /opt
    - source: /opt/{{ PAYARA_ARTIFACT }}
    - archive_format: zip
    - if_missing: {{ PAYARA_INSTALL_DIR }}
  file.symlink:
    - name: /opt/payara
    - target: {{ PAYARA_INSTALL_DIR }}

make-asadmin-executable:
  cmd.run:
    - name: chmod 755 {{ ASADMIN }}
    - require:
      - payara-installed
    - onchanges:
      - payara-installed

payara-running:
  service.running:
    - name: payara
    - require: 
       - make-asadmin-executable

create-admin-password:
  cmd.run:
    - printf "AS_ADMIN_PASSWORD=`openssl rand -base64 15`\n" > {{ ADMIN_PWD }} && chmod 400 {{ ADMIN_PWD }}
    - onchanges:
      - payara-installed

create-tmp-admin-password:
  cmd.run:
    - sed -e $'s/AS_ADMIN_PASSWORD=/AS_ADMIN_PASSWORD=\\\nAS_ADMIN_NEWPASSWORD=/g' {{ ADMIN_PWD }} > {{ TMP_ADMIN_PWD }}
    - onchanges:
      - create-admin-password

set-admin-password:
  cmd.run:
    - name: {{ ASADMIN }} --interactive=false --user admin --passwordfile={{ TMP_ADMIN_PWD }} change-admin-password && rm -f {{ TMP_ADMIN_PWD }}
    - require:
      - make-asadmin-executable
      - payara-running
    - onchanges:
      - create-tmp-admin-password

# SecureAdmin must be activated for access to the admin console
enable-secure-admin:
  cmd.run:
    - name: {{ ASADMIN }} --interactive=false --user admin --passwordfile={{ ADMIN_PWD }} enable-secure-admin
    - require:
      - payara-running
      - set-admin-password
    - onchanges:
      - payara-installed

restart-payara-if-secure-admin-was-enabled:
  cmd.run:
    - name: systemctl restart payara
    - require:
      - enable-secure-admin
    - onchanges:
      - enable-secure-admin

output-admin-password:
  cmd.run:
    - printf "Payara Admin Password: `cat {{ ADMIN_PWD }} | sed 's/.*\=//'`"
    - onlyif:
      - ls {{ TMP_ADMIN_PWD }}
    - onchanges:
      - restart-payara-if-secure-admin-was-enabled
