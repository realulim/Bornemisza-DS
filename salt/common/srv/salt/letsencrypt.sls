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

CF_Key:
  environ.setenv:
    - value: {{ pillar['CFKEY'] }}
    - unless: grep 'SAVED_CF_Key' ~/.acme.sh/account.conf

CF_Email:
  environ.setenv:
    - value: {{ pillar['CFEMAIL'] }}
    - unless: grep 'SAVED_CF_Email' ~/.acme.sh/account.conf

issue-certificate:
  cmd.run:
    - name: ~/.acme.sh/acme.sh --issue --dns dns_cf -d {{ pillar['ssldomain'] }} -d {{ pillar['sslhost'] }} -d {{ pillar['hostname'] }}
    - unless: ls ~/.acme.sh/{{ pillar['ssldomain'] }}/*.cer

~/.acme.sh/{{ pillar['ssldomain'] }}:
  file.directory:
    - user: root
    - group: root
    - dir_mode: 400
    - file_mode: 400
    - recurse:
      - user
      - group
      - mode
    - onlyif: ls ~/.acme.sh/{{ pillar['ssldomain'] }}

create-strong-dh-group:
  cmd.run:
    - name: openssl dhparam -out /etc/pki/tls/private/dhparams.pem 2048
    - unless: ls /etc/pki/tls/private/dhparams.pem

create-pem-file:
  cmd.run:
    - name: cat /root/.acme.sh/{{ pillar['ssldomain'] }}/fullchain.cer /etc/pki/tls/private/dhparams.pem /root/.acme.sh/{{ pillar['ssldomain'] }}/{{ pillar['ssldomain'] }}.key > /etc/pki/tls/private/{{ pillar['ssldomain'] }}.pem
    - onlyif: test /etc/pki/tls/private/{{ pillar['ssldomain'] }}.pem -ot /root/.acme.sh/{{ pillar['ssldomain'] }}/fullchain.cer

protect-pem-file:
  cmd.run:
    - name: chmod -R 400 /etc/pki/tls/private
    - onchanges:
      - create-pem-file
