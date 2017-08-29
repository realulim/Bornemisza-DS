{% set COUCHDB_DIR='apache-couchdb-2.1.0' %}
{% set COUCHDB_TARGZ='apache-couchdb-2.1.0.tar.gz' %}
{% set COUCHDB_BINARY='/home/couchpotato/couchdb/bin/couchdb' %}
{% set AUTH='-u `cat /srv/pillar/netrc`' %}
{% set URL='http://' + pillar['privip'] + ':5984' %}
{% set BACKDOORURL='http://localhost:5986' %}
{% set VIEWS='/home/couchpotato/ddoc' %}

install_couchdb_pkgs:
  pkg.installed:
    - pkgs:
      - autoconf
      - autoconf-archive
      - automake
      - erlang-asn1
      - erlang-erl_interface
      - erlang-erts
      - erlang-eunit
      - erlang-jiffy
      - erlang-os_mon
      - erlang-reltool
      - erlang-snappy
      - erlang-xmerl
      - gcc-c++
      - help2man
      - jq
      - js-devel
      - libcurl-devel
      - libicu-devel
      - libtool
      - perl-Test-Harness

couchpotato:
  user.present:
    - fullname: CouchDB Administrator
    - shell: /bin/bash
    - home: /home/couchpotato

download-couchdb:
  cmd.run:
    - name: bash -c 'mkdir tmp && cd tmp && wget http://mirror.synyx.de/apache/couchdb/source/2.1.0/{{ COUCHDB_TARGZ }}'
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

set-permissions-couchdb:
  cmd.run:
    - name: find /home/couchpotato/couchdb -type d -exec chmod 0770 {} \;
    - onchanges:
      - install-couchdb

install-systemctl-unitfile:
   file.managed:
    - name: /usr/lib/systemd/system/couchdb.service
    - source: salt://files/couchdb/couchdb.service

/home/couchpotato/couchdb/etc/local.d/admins.ini:
  file.copy:
    - source: /srv/salt/files/couchdb/admins.ini
    - user: couchpotato
    - group: couchpotato
    - mode: 644

configure-admin-password:
  file.replace:
    - name: /home/couchpotato/couchdb/etc/local.d/admins.ini
    - pattern: |
        ;admin = mysecretpassword
    - repl: |
        admin = {{ pillar['couchdb-admin-password'] }}
    - show_changes: False

configure-couchdb:
  file.managed:
    - name: /home/couchpotato/couchdb/etc/local.ini
    - source: salt://files/couchdb/local.ini
    - user: couchpotato
    - group: couchpotato
    - mode: 644
    - template: jinja

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

/etc/logrotate.d/couchdb:
  file.managed:
    - source: salt://files/couchdb/couchdb.logrotate.conf

run-couchdb:
  service.running:
    - name: couchdb
    - enable: True
    - init_delay: 5
    - listen:
      - file: /usr/lib/systemd/system/couchdb.service
      - file: /home/couchpotato/couchdb/etc/local.d/admins.ini
      - file: /home/couchpotato/couchdb/etc/local.ini
      - file: /home/couchpotato/couchdb/etc/vm.args

join-couchdb-cluster:
  cmd.run:
     - name: curl -s {{ AUTH }} -X PUT "{{ BACKDOORURL }}/_nodes/couchdb@{{ pillar['clusterip'] }}" -d {}; sleep 5
     - onlyif:
       - test '{{ pillar['clusterip'] }}' != '{{ pillar['privip'] }}'
       - curl -s {{ AUTH }} {{ URL }}/_membership|grep -F '{"all_nodes":["couchdb@{{ pillar['privip'] }}"],"cluster_nodes":["couchdb@{{ pillar['privip'] }}"]}'
       - </dev/tcp/{{ pillar['clusterip'] }}/4369

{% for db in ['_users', '_replicator', '_global_changes'] %}
create-database-{{ db }}:
  cmd.run:
    - name: curl -s -X PUT {{ AUTH }} {{ URL }}/{{ db }}
    - onlyif:
      - curl -s {{ AUTH }} {{ URL }}/{{ db }} | grep "Database does not exist"
      - curl -s {{ AUTH }} {{ URL }}/_membership|jq -re ".all_nodes|length" | grep {{ pillar['clustersize'] }}
      - curl -s {{ AUTH }} {{ URL }}/_membership|jq -re ".cluster_nodes|length" | grep {{ pillar['clustersize'] }}
{% endfor %}

{{ VIEWS }}:
  file.directory:
    - user: couchpotato
    - group: couchpotato
    - dir_mode: 755

{{ VIEWS }}/User.json:
  file.managed:
    - source: salt://files/couchdb/ddocUser.json
    - user: couchpotato
    - group: couchpotato

/opt/scripts/ddoc.sh:
  file.managed:
    - source: salt://files/couchdb/ddoc.sh
    - user: root
    - group: root
    - file_mode: 700

create-or-update-design-doc-User:
  cmd.run:
    - name: /opt/scripts/ddoc.sh {{ URL }}
    - onchanges:
      - {{ VIEWS }}/User.json
