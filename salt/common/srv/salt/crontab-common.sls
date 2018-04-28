{%- set OFFSET=pillar['nodenumber'] -%}
{%- set RANDOM=shuf -i0-59 -n1 -%}

/opt/scripts/checkForDeletedSrvRecords.sh:
  file.managed:
    - source: salt://files/basics/checkForDeletedSrvRecords.sh
    - template: jinja
    - user: root
    - group: root
    - mode: 700
  cron.present:
    - user: root
    - minute: '*'

/usr/bin/bash -c '/opt/bootstrap-bornemisza.sh >> /opt/logs/highstate.log':
  cron.present:
    - user: root
    - minute: '{{ OFFSET - 1 }}-59/15'
    - hour: '*'

/usr/bin/bash -c '/root/.acme.sh/acme.sh --cron --home /root/.acme.sh':
  cron.present:
    - user: root
    - minute: random
    - hour: 0
