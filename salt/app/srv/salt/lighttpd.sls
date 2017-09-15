install_lighttpd_pkgs:
  pkg.installed:
    - pkgs:
      - lighttpd

/etc/lighttpd/lighttpd.conf:
  file.replace:
    - pattern: "server.port = 80"
    - repl: server.port = 4331
