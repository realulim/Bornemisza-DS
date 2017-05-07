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
