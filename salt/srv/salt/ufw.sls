ufw:
  pkg:
    - installed
  service:
    - running

firewalld:
  service:
    - dead
  pkg:
    - removed

firewall_rule_remove_ssh:
  cmd.run:
    - name: ufw delete allow SSH
    - onlyif: ufw status|grep -E "22/tcp\s+(SSH)\s+ALLOW\s+IN\s+Anywhere"

{% for port in ['80', '922', '4848', '9000'] %}
firewall_rule_allow_{{ port }}:
  cmd.run:
    - name: ufw allow {{ port }}/tcp
    - unless: ufw status|grep -E "{{ port }}/tcp\s+ALLOW\s+Anywhere"
{% endfor %}
