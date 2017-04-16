firewall_zone_public:
  firewalld.present:
    - name: public
    - block_icmp:
      - echo-reply
      - echo-request
    - masquerade: True
    - ports:
      - 922/tcp
      - 25/tcp
      - 80/tcp
      - 4848/tcp
      - 9000/tcp

firewall_zone_failover:
  firewalld.present:
    - name: failover
    - block_icmp:
      - echo-reply
      - echo-request
    - masquerade: True
    - sources:
      - 94.237.28.181
      - 45.76.37.215
      - 108.61.177.129
    - ports:
      - 8080/tcp

firewalld:
  service.running:
    - watch:
      - firewalld: firewall_zone_public
      - firewalld: firewall_zone_failover
