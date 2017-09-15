install_lighttpd_pkgs:
  pkg.installed:
    - pkgs:
      - lighttpd

configure-lighttpd-port:
  file.replace:
    - name: /etc/lighttpd/lighttpd.conf
    - pattern: "server.port = 80"
    - repl: server.port = 4331

configure-lighttpd-docroot:
  file.replace:
    - name: /etc/lighttpd/lighttpd.conf
    - pattern: "server.document-root = server_root + \"/lighttpd\""
    - repl: server.document-root = server_root + "/{{ pillar['ssldomain'] }}"

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
