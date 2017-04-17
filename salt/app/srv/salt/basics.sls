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

ntpd:
  service.running

Europe/Berlin:
  timezone.system

/etc/profile.d/bash.sh:
  file.managed:
    - source: salt://files/basics/bash.sh

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
