download-acme.sh-client:
  cmd.run:
    - name: git clone https://github.com/Neilpang/acme.sh.git
    - unless: ls .acme.sh

install-acme-client:
  cmd.run:
    - name: cd ./acme.sh/acme.sh --install
    - onchanges:
      - download-acme.sh-client
