{% set PASSWORD=salt['cmd.run']('openssl rand -base64 15') %}

haproxy:
  pkg:
    - installed
  service.running:
    - watch:
      - pkg: haproxy
      - file: /etc/haproxy/haproxy.cfg

configure-stats-password:
  file.replace:
    - name: /srv/pillar/haproxy.sls
    - pattern: placeholder
    - repl: {{ PASSWORD }}
    - onlyif: grep placeholder /srv/pillar/haproxy.sls

/etc/haproxy/haproxy.cfg:
  file.managed:
    - source: salt://files/haproxy/haproxy.cfg
    - template: jinja
    - user: root
    - group: root
    - mode: 644
    - require:
      - configure-stats-password
