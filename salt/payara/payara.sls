{% set PAYARA_INSTALL_DIR='/opt/payara' %}
{% set PAYARA_ARTIFACT='payara-4.1.1.171.1.zip' %}

download-payara:
  cmd.run:
    - name: curl -o /opt/{{ PAYARA_ARTIFACT }} -L http://info.payara.fish/cs/c/?cta_guid=b9609f35-f630-492f-b3c0-238fc55f489b&placement_guid=7cca6202-06a3-4c29-aee0-ca58af60528a&portal_id=334594&redirect_url=APefjpESGl8Izb5xKZC3c8JiNc6evRvUNNIGBiNbo6VlawYx3tVT_SuQCLAllFzQ8YOe8U55DnkEGFASXhH4eU_0PeJst44wg5IjdkD8bNTI2L85PCrYbVY&hsutk=&canon=http%3A%2F%2Fwww.payara.fish%2Fall_downloads
    - creates: /opt/{{ PAYARA_ARTIFACT }}

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
