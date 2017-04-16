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

firewall_rule_remove_ssh:
  cmd.run:
    - name: ufw delete allow SSH
    - onlyif: ufw status|grep "22/tcp\s+(SSH)\s+ALLOW\s+IN\s+Anywhere"

{% for port in ['80', '922', '4848', '9000'] %}
firewall_rule_allow_{{ port }}:
  cmd.run:
    - name: ufw allow {{ port }}/tcp
    - unless: ufw status|grep "{{ port }}/tcp\s+ALLOW\s+Anywhere"
{% endfor %}

{% for ip in ['94.237.28.181', '45.76.37.215', '108.61.177.129'] %}
firewall_rule_failover_from_{{ ip }}:
  cmd.run:
    - name: ufw allow from {{ ip }} to any port 8080
    - unless: ufw status|grep "8080\s+ALLOW\s+{{ ip }}"
{% endfor %}
