install_couchdb_pkgs:
  pkg.installed:
    - pkgs:
      - autoconf
      - autoconf-archive
      - automake
      - curl-devel
      - erlang-asn1
      - erlang-erts
      - erlang-eunit
      - erlang-os_mon
      - erlang-xmerl
      - help2man
      - js-devel-1.8.5
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
