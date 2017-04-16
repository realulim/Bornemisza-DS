ufw:
  pkg:
    - installed
  service.running:
    - listen:
      - pkg: ufw

disable_and_remove_firewalld:
  service.dead:
    - name: firewalld
  pkg.removed:
    - name: firewalld

firewall_rule_ssh:
  cmd.run:
    - name: ufw allow 922/tcp && ufw delete allow SSH
    - unless: ufw status|grep 922
