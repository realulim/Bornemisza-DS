install_lighttpd_pkgs:
  pkg.installed:
    - pkgs:
      - lighttpd

/etc/lighttpd/lighttpd.conf:
  file.managed:
    - source: salt://files/lighttpd/lighttpd.conf
    - template: jinja

/etc/lighttpd/conf.d/access_log.conf:
  file.managed:
    - source: salt://files/lighttpd/access_log.conf

/etc/lighttpd/modules.conf:
  file.replace:
    - pattern: "#  \"mod_rewrite\","
    - repl: "\"mod_rewrite\","

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
      - file: /etc/lighttpd/modules.conf
