haproxy:
  pkg:
    - installed
  service.running:
    - watch:
      - pkg: haproxy
      - file: /etc/haproxy/haproxy.cfg

/etc/haproxy/haproxy.cfg:
  file.managed:
    - source: salt://files/haproxy/haproxy.cfg
    - template: jinja
    - user: root
    - group: root
    - mode: 644

configure_haproxy_logging:
  file.append:
    - name: /etc/rsyslog.conf
    - text: #local2.=info     /var/log/haproxy-info.log
    - text: local2.notice    /var/log/haproxy-allbutinfo.log

restart_rsyslog_if_haproxy_configured:
  cmd.run:
    - name: systemctl restart rsyslog
    - onchanges:
      - configure_haproxy_logging
