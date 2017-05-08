{% for port in ['80', '179', '443', '4848', '9000'] %}
firewall_rule_allow_{{ port }}:
  cmd.run:
    - name: ufw allow {{ port }}/tcp
    - unless: ufw status|grep -E "{{ port }}/tcp\s+ALLOW\s+Anywhere"
{% endfor %}

firewall_rule_hazelcast_on_private_network:
  cmd.run:
    - name: ufw allow in on eth1
    - unless: ufw status|grep -E "Anywhere on eth1\s+ALLOW\s+Anywhere"
