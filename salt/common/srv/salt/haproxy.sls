haproxy:
  pkg:
    - installed
  service.running:
    - enable: true
    - listen:
      - file: /etc/haproxy/haproxy.cfg

irqbalance:
  service:
    - dead
  pkg:
    - removed

/etc/haproxy/haproxy.cfg:
  file.managed:
    - source: salt://files/haproxy/haproxy.cfg
    - template: jinja
    - user: root
    - group: root
    - mode: 644
    - show_changes: false

configure_haproxy_logging:
  file.append:
    - name: /etc/rsyslog.conf
    - text: |
        #local2.=info     /var/log/haproxy-info.log
        local2.notice    /var/log/haproxy-allbutinfo.log

create-haproxy-log-symlink:
  file.symlink:
    - name: /opt/logs/haproxy-allbutinfo.log
    - target: /var/log/haproxy-allbutinfo.log

