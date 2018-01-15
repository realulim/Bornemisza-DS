{% for port in ['80', '179', '443', '4848', '9000'] %}
firewall_rule_allow_{{ port }}:
  cmd.run:
    - name: ufw allow {{ port }}/tcp
    - unless: ufw status|grep -E "{{ port }}/tcp\s+ALLOW\s+Anywhere"
{% endfor %}

firewall_rule_hazelcast_on_private_network:
  cmd.run:
    - name: ufw allow in on {{ pillar['private-if'] }}
    - unless: ufw status|grep -E "Anywhere on {{ pillar['private-if'] }}\s+ALLOW\s+Anywhere"
