install_basics_pkgs:
  pkg.installed:
    - pkgs:
      - java-1.8.0-openjdk.x86_64
      - nano
      - net-tools
      - ntp
      - telnet

install_basics_groups:
  pkg.group_installed:
    - name: "Development Tools"

/etc/profile.d/bash.sh:
  file.managed:
    - source: salt://files/basics/bash.sh

Europe/Berlin:
  timezone.system

sshd:
  service.running:
    - enable: True
    - listen:
      - file: /etc/ssh/sshd_config

/etc/ssh/sshd_config:
  file.replace:
    - pattern: "#Port 22"
    - repl: "Port 922"

rsyslog:
  service.running:
    - enable: True
    - listen:
      - file: /etc/rsyslog.conf

/etc/rsyslog.conf:
  file.append:
    - text: |
        $ModLoad imudp
        $UDPServerRun 514
        $UDPServerAddress 127.0.0.1

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
    - name: external
    - block_icmp:
      - echo-reply
      - echo-request
    - masquerade: True
    - sources:
      - 94.237.28.181
      - 45.76.37.215
      - 108.61.177.129
    - ports:
      - 8080

firewalld:
  service.running:
    - watch:
      - firewalld: firewall_zone_public
      - firewalld: firewall_zone_failover
