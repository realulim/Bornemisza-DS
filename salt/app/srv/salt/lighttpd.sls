install_lighttpd_pkgs:
  pkg.installed:
    - pkgs:
      - lighttpd

/etc/lighttpd/lighttpd.conf:
  file.replace:
    - pattern: "server.port = 80"
    - repl: server.port = 4331

lighttpd-running:
  service.running:
    - name: lighttpd
    - enable: true
    - listen:
      - file: /etc/lighttpd/lighttpd.conf
