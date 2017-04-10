{% set PAYARA_INSTALL_DIR='/opt/payara' %}
{% set PAYARA_ARTIFACT='payara-4.1.1.171.1.zip' %}

download-payara:
  cmd.run:
    - name: curl -o /opt/{{ PAYARA_ARTIFACT }} -L http://info.payara.fish/cs/c/?cta_guid=b9609f35-f630-492f-b3c0-238fc55f489b&placement_guid=7cca6202-06a3-4c29-aee0-ca58af60528a&portal_id=334594&redirect_url=APefjpESGl8Izb5xKZC3c8JiNc6evRvUNNIGBiNbo6VlawYx3tVT_SuQCLAllFzQ8YOe8U55DnkEGFASXhH4eU_0PeJst44wg5IjdkD8bNTI2L85PCrYbVY&hsutk=&canon=http%3A%2F%2Fwww.payara.fish%2Fall_downloads
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
#  extract new zip file
payara-installed:
  service.dead:
    - name: payara
    - onchanges:
      - download-payara
    - require:
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
    - name: chmod 755 /opt/payara/bin/asadmin
    - require:
      - payara-installed
    - onchanges:
      - payara-installed

payara-running:
  service.running:
    - name: payara
    - require: 
       - make-asadmin-executable

set-admin-password:
  file.managed:
    - name: /opt/payara-change-admin-pwd
    - source: salt://cys/files/payara/payara-change-admin-pwd
    - template: jinja
  cmd.run:
    - name: /opt/payara/bin/asadmin --interactive=false --user admin --passwordfile=/opt/payara-change-admin-pwd change-admin-password
    - require:
      - make-asadmin-executable
    - onchanges:
      - payara-installed
      - file: /opt/payara-change-admin-pwd

# SecureAdmin must be activated for access to the admin console
enable-secure-admin:
  file.managed:
    - name: /opt/payara-admin-pwd
    - source: salt://cys/files/payara/payara-admin-pwd
    - template: jinja
  cmd.run:
    - name: /opt/payara/bin/asadmin --interactive=false --user admin --passwordfile=/opt/payara-admin-pwd  enable-secure-admin
    - require:
      - payara-running
    - onchanges:
      - payara-installed
      - file: /opt/payara-admin-pwd

restart-payara-if-secure-admin-was-enabled:
  cmd.run:
    - name: systemctl restart payara
    - require:
      - enable-secure-admin
    - onchanges:
      - enable-secure-admin
