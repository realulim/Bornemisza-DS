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

monit-running:
  service.running:
    - name: monit
    - enable: true
    - listen: /etc/monitrc
