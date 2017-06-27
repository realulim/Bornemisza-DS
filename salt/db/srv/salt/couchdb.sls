{% set COUCHDB_TARGZ='apache-couchdb-2.0.0.tar.gz' %}

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
    - name: bash -c 'mkdir ~/tmp && cd ~/tmp && wget http://mirror.synyx.de/apache/couchdb/source/2.0.0/{{ COUCHDB_TARGZ }}'
    - user: couchpotato
    - unless: ls ~/tmp/{{ COUCHDB_TARGZ }}
