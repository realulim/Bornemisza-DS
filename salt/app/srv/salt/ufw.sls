{% for port in ['80', '4848', '9000'] %}
firewall_rule_allow_{{ port }}:
  cmd.run:
    - name: ufw allow {{ port }}/tcp
    - unless: ufw status|grep -E "{{ port }}/tcp\s+ALLOW\s+Anywhere"
{% endfor %}

{% for ip in [pillar['ip1'], pillar['ip2'], pillar['ip3']] %}
firewall_rule_hazelcast_from_{{ ip }}:
  cmd.run:
    - name: ufw allow from {{ ip }} to any port 5701
    - unless: ufw status|grep -E "5701\s+ALLOW\s+{{ ip }}"
{% endfor %}
