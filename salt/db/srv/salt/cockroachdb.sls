{% set CRDB_DIR='/opt/cockroachdb' %}
{% set CRDB_VERSION='1.0' %}
{% set CRDB_ARTIFACT='cockroach-v1.0.linux-amd64.tgz' %}

download-crdb:
  cmd.run:
    - name: curl -o /opt/{{ CRDB_ARTIFACT }} -L https://binaries.cockroachdb.com/{{ CRDB_ARTIFACT }}
    - unless: ls /opt/cockroachdb-{{ CRDB_VERSION }}

# If there is a new cockroachdb.tar.gz file:
#  remove old installation
#  extract new .tar.gz file and rename extracted directory sensibly
crdb-installed:
  cmd.run:
    - name: rm -rf {{ CRDB_DIR }}
    - onchanges:
      - download-crdb
  archive.extracted:
    - name: /opt
    - source: /opt/{{ CRDB_ARTIFACT }}
    - archive_format: tar
    - unless: ls {{ CRDB_DIR }}
  file.rename:
    - source: /opt/cockroach-v{{ CRDB_VERSION }}.linux-amd64
    - name: /opt/cockroachdb-{{ CRDB_VERSION }}
    - onlyif: ls /opt/cockroach-v{{ CRDB_VERSION }}.linux-amd64

create-crdb-dir-symlink:
  file.symlink:
    - name: {{ CRDB_DIR }}
    - target: cockroachdb-{{ CRDB_VERSION }}

create-crdb-binary-symlink:
  file.symlink:
    - name: /usr/local/bin/cockroach
    - target: {{ CRDB_DIR }}/cockroach

/opt/{{ CRDB_ARTIFACT }}:
  file.absent
