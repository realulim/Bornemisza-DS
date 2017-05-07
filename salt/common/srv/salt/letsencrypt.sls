/opt/acme.sh:
  file.directory:
    - user: root
    - group: root
    - dir_mode: 755

download-acme.sh-client:
  cmd.run:
    - name: git clone https://github.com/Neilpang/acme.sh.git /opt/acme.sh
    - unless: ls /opt/acme.sh/acme.sh

install-acme-client:
  cmd.run:
    - name: bash -c 'cd /opt/acme.sh && ./acme.sh --install'
    - onchanges:
      - download-acme.sh-client
