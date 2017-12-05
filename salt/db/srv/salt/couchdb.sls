{% set AUTH='-u `cat /srv/pillar/netrc`' %}
{% set URL='http://' + pillar['privip'] + ':5984' %}
{% set BACKDOORURL='http://localhost:5986' %}
{% set VIEWS='/home/couchdb/ddoc' %}

couchdb:
  user.present:
    - fullname: CouchDB Administrator
    - shell: /bin/bash
    - home: /home/couchdb

/etc/yum.repos.d/bintray-apache-couchdb-rpm.repo:
  file.managed:
    - source: salt://files/couchdb/bintray-apache-couchdb-rpm.repo
    - user: root
    - group: root
    - mode: 644

install_couchdb_pkgs:
  pkg.installed:
    - pkgs:
      - epel-release
      - couchdb

set-permissions-couchdb:
  cmd.run:
    - name: find /opt/couchdb -type d -exec chmod 0770 {} \;
    - onchanges:
      - install_couchdb_pkgs

/var/lib/couchdb:
  file.directory:
    - user: couchdb
    - group: couchdb
    - recurse:
      - user
      - group

install-couchdb-systemd-unitfile:
   file.managed:
    - name: /usr/lib/systemd/system/couchdb.service
    - source: salt://files/couchdb/couchdb.service

/opt/couchdb/etc/local.d/admins.ini:
  file.copy:
    - source: /srv/salt/files/couchdb/admins.ini
    - user: couchdb
    - group: couchdb
    - mode: 644

configure-admin-password:
  file.replace:
    - name: /opt/couchdb/etc/local.d/admins.ini
    - pattern: |
        ;admin = mysecretpassword
    - repl: |
        admin = {{ pillar['couchdb-admin-password'] }}
    - show_changes: False

configure-couchdb:
  file.managed:
    - name: /opt/couchdb/etc/local.ini
    - source: salt://files/couchdb/local.ini
    - user: couchdb
    - group: couchdb
    - mode: 644
    - template: jinja

/opt/couchdb/etc/vm.args:
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

create-couchdb-log-symlink:
  file.symlink:
    - name: /opt/logs/couchdb.log
    - target: /home/couchdb/couchdb.log

run-couchdb:
  service.running:
    - name: couchdb
    - enable: True
    - init_delay: 5
    - listen:
      - file: /usr/lib/systemd/system/couchdb.service
      - file: /opt/couchdb/etc/local.d/admins.ini
      - file: /opt/couchdb/etc/local.ini
      - file: /opt/couchdb/etc/vm.args

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
      - curl -s {{ AUTH }} {{ URL }}/_membership|jq -re ".all_nodes|length" | grep {{ pillar['dbclustersize'] }}
      - curl -s {{ AUTH }} {{ URL }}/_membership|jq -re ".cluster_nodes|length" | grep {{ pillar['dbclustersize'] }}
{% endfor %}

{{ VIEWS }}:
  file.directory:
    - user: couchdb
    - group: couchdb
    - dir_mode: 755

{{ VIEWS }}/User.json:
  file.managed:
    - source: salt://files/couchdb/ddocUser.json
    - user: couchdb
    - group: couchdb

/opt/scripts/ddoc.sh:
  file.managed:
    - source: salt://files/couchdb/ddoc.sh
    - user: root
    - group: root
    - mode: 744

create-or-update-design-doc-User:
  cmd.run:
    - name: /opt/scripts/ddoc.sh {{ URL }} | grep -v error
    - onchanges:
      - {{ VIEWS }}/User.json
