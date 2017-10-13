install_monit_pkgs:
  pkg.installed:
    - pkgs:
      - monit

/etc/monitrc:
  file.managed:
    - source: salt://files/monit/monitrc
    - template: jinja
    - user: root
    - group: root
    - mode: 600

create-monit.log-symlink:
  file.symlink:
    - name: /opt/logs/monit.log
    - target: /var/log/monit.log

monit-running:
  service.running:
    - name: monit
    - enable: true
    - listen: /etc/monitrc
