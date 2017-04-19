install_basics_pkgs:
  pkg.installed:
    - pkgs:
      - java-1.8.0-openjdk.x86_64
      - nano
      - net-tools
      - ntp
      - telnet
      - ufw

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

firewalld:
  service:
    - dead
  pkg:
    - removed

ufw:
  service.running

firewall_rule_remove_ssh:
  cmd.run:
    - name: ufw delete allow SSH
    - onlyif: ufw status|grep -E "SSH\s+ALLOW\s+Anywhere"

{% for port in ['922'] %}
firewall_rule_allow_{{ port }}:
  cmd.run:
    - name: ufw allow {{ port }}/tcp
    - unless: ufw status|grep -E "{{ port }}/tcp\s+ALLOW\s+Anywhere"
{% endfor %}

sshd:
  service.running:
    - enable: True
    - listen:
      - file: /etc/ssh/sshd_config

/etc/ssh/sshd_config:
  file.replace:
    - pattern: "#Port 22"
    - repl: "Port 922"
    - require:
      - firewall_rule_allow_922
