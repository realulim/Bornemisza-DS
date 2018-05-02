{%- set OFFSET=pillar['nodenumber'] -%}

/opt/scripts/checkForDeletedSrvRecords.sh:
  file.managed:
    - source: salt://files/basics/checkForDeletedSrvRecords.sh
    - template: jinja
    - user: root
    - group: root
    - mode: 700
  cron.present:
    - identifier: srvrecords
    - user: root
    - minute: '*'

/usr/bin/bash -c '/opt/bootstrap-bornemisza.sh >> /opt/logs/highstate.log':
  cron.present:
    - identifier: highstate
    - user: root
    - minute: '{{ OFFSET - 1 }}-59/15'
    - hour: '*'

/usr/bin/bash -c '/root/.acme.sh/acme.sh --cron --home /root/.acme.sh > /dev/null':
  cron.present:
    - identifier: letsencrypt
    - user: root
    - minute: random
    - hour: 0
