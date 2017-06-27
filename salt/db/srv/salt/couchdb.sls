{% set COUCHDB_DIR='apache-couchdb-2.0.0' %}
{% set COUCHDB_TARGZ='apache-couchdb-2.0.0.tar.gz' %}
{% set COUCHDB_BINARY='/home/couchpotato/couchdb/bin/couchdb' %}

install_couchdb_pkgs:
  pkg.installed:
    - pkgs:
      - autoconf
      - autoconf-archive
      - automake
      - libcurl-devel
      - erlang-asn1
      - erlang-erts
      - erlang-eunit
      - erlang-os_mon
      - erlang-xmerl
      - help2man
      - js-devel
      - libicu-devel
      - libtool
      - perl-Test-Harness
      - gcc-c++
      - erlang-reltool
      - erlang-erl_interface
      - erlang-jiffy
      - erlang-snappy

install-systemctl-unitfile:
   file.managed:
    - name: /usr/lib/systemd/system/couchdb.service
    - source: salt://files/couchdb/couchdb.service

couchpotato:
  user.present:
    - fullname: CouchDB Administrator
    - shell: /bin/bash
    - home: /home/couchpotato

download-couchdb:
  cmd.run:
    - name: bash -c 'mkdir tmp && cd tmp && wget http://mirror.synyx.de/apache/couchdb/source/2.0.0/{{ COUCHDB_TARGZ }}'
    - cwd: /home/couchpotato
    - runas: couchpotato
    - unless: ls ./tmp/{{ COUCHDB_TARGZ }}

build-couchdb:
  cmd.run:
    - name: bash -c 'tar -xzf {{ COUCHDB_TARGZ }} && cd {{ COUCHDB_DIR }} && ./configure && make release'
    - cwd: /home/couchpotato/tmp
    - runas: couchpotato
    - unless: ls ./{{ COUCHDB_DIR }}/rel/couchdb

install-couchdb:
  cmd.run:
    - name: bash -c 'cp -R rel/couchdb ~/ && find ~/couchdb -type d -exec chmod 0770 {} \; && chmod 0644 ~/couchdb/etc/*'
    - cwd: /home/couchpotato/tmp/{{ COUCHDB_DIR }}
    - runas: couchpotato
    - unless: ls {{ COUCHDB_BINARY }}

run-couchdb:
  service.running:
    - name: couchdb
    - enable: true
    - listen:
      - file: /usr/lib/systemd/system/couchdb.service
