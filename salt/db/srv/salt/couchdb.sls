{% set COUCHDB_DIR='apache-couchdb-2.0.0' %}
{% set COUCHDB_TARGZ='apache-couchdb-2.0.0.tar.gz' %}
{% set COUCHDB_BINARY='/home/couchpotato/couchdb/bin/couchdb' %}
{% set COUCHDB_CONFIG='/home/couchpotato/couchdb/etc/local.ini' %}

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

install-systemctl-unitfile:
   file.managed:
    - name: /usr/lib/systemd/system/couchdb.service
    - source: salt://files/couchdb/couchdb.service

create-couchdb-admin-user:
  file.replace:
    - name: {{ COUCHDB_CONFIG }}
    - pattern: ";admin = mysecretpassword"
    - repl: "admin = {{ pillar['couchdb-admin-password'] }}"
    - show_changes: False

configure-couchdb-logging:
  file.append:
    - name: {{ COUCHDB_CONFIG }}
    - text: |
        [log]
        writer = file
        file = /home/couchpotato/couchdb.log
        level = info
    - show_changes: False

/home/couchpotato/couchdb/etc/vm.args:
  file.managed:
    - source: salt://files/couchdb/vm.args
    - template: jinja
    - show_changes: False

/srv/pillar/netrc:
  file.managed:
    - source: salt://files/couchdb/netrc
    - template: jinja
    - mode: 400
    - show_changes: False

run-couchdb:
  service.running:
    - name: couchdb
    - enable: True
    - init_delay: 5
    - listen:
      - file: /usr/lib/systemd/system/couchdb.service
      - file: {{ COUCHDB_CONFIG }}
      - file: /home/couchpotato/couchdb/etc/vm.args

{% for db in ['_users', '_replicator', '_global_changes', 'nodes'] %}
create-database-{{ db }}:
  cmd.run:
    - name: curl -s -X PUT --netrc-file /srv/pillar/netrc http://localhost:5984/{{ db }}
    - onlyif: curl -s --netrc-file /srv/pillar/netrc http://localhost:5984/{{ db }} | grep "Database does not exist"
{% endfor %}

#{% for node in [pillar['ip1'], pillar['ip2'], pillar['ip3']] %}
#add-{{ node }}-to-cluster:
#  cmd.run:
#    - name: curl -s --netrc-file /srv/pillar/netrc -X PUT "http://localhost:5986/_nodes/couchdb@{{ node }}" -d {}
#    - onlyif: </dev/tcp/{{ node }}/4369
#    - unless: curl -s --netrc-file /srv/pillar/netrc http://localhost:5984/_membership|grep {{ node }}'
#{% endfor %}
