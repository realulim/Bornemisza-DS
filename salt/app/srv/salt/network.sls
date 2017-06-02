create-dummy-interface:
  cmd.run:
    - name: ip link add dev dummy1 type dummy && ip link set dummy1 up && ip addr add dev dummy1 {{ pillar['floatip'] }}/32
    - unless: ip addr show dev dummy1

/etc/bird.conf:
  file.managed:
    - source: salt://files/network/bird.conf
    - template: jinja

bird:
  pkg:
    - installed
  service.running:
    - enable: true
    - listen:
      - file: /etc/bird.conf
