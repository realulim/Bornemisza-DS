install_lighttpd_pkgs:
  pkg.installed:
    - pkgs:
      - lighttpd

/etc/lighttpd/lighttpd.conf:
  file.managed:
    - source: salt://files/lighttpd/lighttpd.conf
    - template: jinja

/var/www/{{ pillar['ssldomain'] }}:
  file.directory:
    - user: lighttpd
    - group: lighttpd
    - dir_mode: 700

lighttpd-running:
  service.running:
    - name: lighttpd
    - enable: true
    - listen:
      - file: /etc/lighttpd/lighttpd.conf
