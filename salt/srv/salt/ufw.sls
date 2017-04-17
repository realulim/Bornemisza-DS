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
    - onlyif: ufw status|grep -E "SSH\s+ALLOW\s+Anywhere"

{% for port in ['80', '922', '4848', '9000'] %}
firewall_rule_allow_{{ port }}:
  cmd.run:
    - name: ufw allow {{ port }}/tcp
    - unless: ufw status|grep -E "{{ port }}/tcp\s+ALLOW\s+Anywhere"
{% endfor %}

{% for ip in ['94.237.28.181', '45.76.37.215', '108.61.177.129'] %}
firewall_rule_hazelcast_from_{{ ip }}:
  cmd.run:
    - name: ufw allow from {{ ip }} to any port 5701
    - unless: ufw status|grep -E "5701\s+ALLOW\s+{{ ip }}"
{% endfor %}
