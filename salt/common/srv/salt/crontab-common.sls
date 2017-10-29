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

/usr/bin/salt-call -l info state.highstate >> /opt/logs/highstate.log
  cron.present:
    - user: root
    - minute: '*/15'
