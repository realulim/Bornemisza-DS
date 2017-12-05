{%- set IP1=''.join(salt.dnsutil.A(pillar['cfns'], pillar['cfns2'])) -%}
{%- set IP2=''.join(salt.dnsutil.A(pillar['cfns2'], pillar['cfns'])) -%}

install_basics_pkgs:
  pkg.installed:
    - pkgs:
      - htop
      - nano
      - net-tools
      - nmap-ncat
      - ntp
      - telnet
      - ufw

install_basics_groups:
  pkg.group_installed:
    - name: "Development Tools"

configure_resolvers:
  file.append:
    - name: /etc/sysconfig/network-scripts/ifcfg-eth0
    - text: |
        DNS1={{ IP1 }}
	DNS2={{ IP2 }}

restart_network:
  cmd.run:
    - name: systemctl restart network
    - onchanges:
      - configure_resolvers

/root/download:
  file.directory:
    - user: root
    - group: root
    - dir_mode: 755

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

create-swapfile:
  cmd.run:
    - name: bash -c 'dd if=/dev/zero of=/swap bs=1M count=2048 && mkswap /swap && chmod 600 /swap'
    - creates: /swap

create-swap-mountpoint:
  file.append:
    - name: /etc/fstab
    - text: /swap   swap  swap  defaults  0 0

mount-swapfile:
  cmd.run:
    - name: swapon -a
    - unless: swapon -s | grep /swap

/opt/logs:
  file.directory:
    - user: root
    - group: root
    - dir_mode: 755
