/etc/sysconfig/network-scripts/ifcfg-eth1:
  file.managed:
    - source: salt://files/network/ifcfg-eth1
    - template: jinja

restart-network-interface:
  cmd.run:
    - name: ifdown eth1 && ifup eth1
    - onchanges: 
      - /etc/sysconfig/network-scripts/ifcfg-eth1

create-dummy-interface:
  cmd.run:
    - name: ip link add dev dummy1 type dummy && ip link set dummy1 up && ip addr add dev dummy1 {{ pillar['floatip'] }}/32
    - unless: ip addr show dev dummy1

/etc/bird.conf:
  file.managed:
    - source: salt://files/network/bird.conf
    - template: jinja

bird:
  service.running:
    - listen:
      - file: /etc/bird.conf
