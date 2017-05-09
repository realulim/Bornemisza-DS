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
    - name: ~/.acme.sh/acme.sh --issue --dns dns_cf -d {{ pillar['floatdomain'] }} -d {{ pillar['floathost'] }} -d {{ pillar['hostname'] }}
    - unless: ls ~/.acme.sh/{{ pillar['floatdomain'] }}/*.cer

~/.acme.sh/{{ pillar['floatdomain'] }}:
  file.directory:
    - user: root
    - group: root
    - dir_mode: 400
    - file_mode: 400
    - recurse:
      - user
      - group
      - mode
    - onlyif: ls ~/.acme.sh/{{ pillar['floatdomain'] }}

create-pem-file:
  cmd.run:
    - name: cat /root/.acme.sh/{{ pillar['floatdomain'] }}/fullchain.cer /root/.acme.sh/{{ pillar['floatdomain'] }}/{{ pillar['floatdomain'] }}.key > /etc/pki/tls/private/{{ pillar['floatdomain'] }}.pem
    - onlyif: test /etc/pki/tls/private/{{ pillar['floatdomain'] }}.pem -ot /root/.acme.sh/{{ pillar['floatdomain'] }}/fullchain.cer

protect-pem-file:
  cmd.run:
    - name: chmod -R 400 /etc/pki/tls/private
    - onchanges:
      - create-pem-file
