/etc/sysconfig/network-scripts/ifcfg-eth1:
  file.managed:
    - source: salt://files/network/ifcfg-eth1
    - template: jinja

restart-network-interface:
  cmd.run:
    - name: ifdown eth1 && ifup eth1
    - onchanges: 
      - /etc/sysconfig/network-scripts/ifcfg-eth1
