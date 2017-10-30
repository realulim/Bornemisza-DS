/opt/scripts/checkForDeletedAppSrvRecords.sh:
  file.managed:
    - source: salt://files/checkForDeletedAppSrvRecords.sh
    - template: jinja
    - user: root
    - group: root
    - mode: 700
#  cron.present:
#    - user: root
#    - minute: '*'
