{% set CFCMD='/srv/salt/files/basics/cloudflare.sh' %}
{% set CFAPI=pillar['CFAPI'] %}
{% set CFZONEID=pillar['CFZONEID'] %}
{% set CFEMAIL=pillar['CFEMAIL'] %}
{% set CFKEY=pillar['CFKEY'] %}
{% set DOMAIN=pillar['ssldomain'] %}
{% set HOST=pillar['hostname'] %}
{% set SRVDATA='{"type":"SRV","name":"_db._tcp.' + pillar['ssldomain'] + '.","content":"SRV 1 0 443 ' + pillar['hostname'] + '.","data":{"priority":1,"weight":0,"port":443,"target":"' + pillar['hostname'] + '","service":"_db","proto":"_tcp","name":"' + pillar['ssldomain'] + '","ttl":"1","proxied":false}}' %}
{% set SERVICE='_db._tcp.' + pillar['ssldomain'] %}

create-srv-record:
  cmd.run:
    - name: {{ CFCMD }} cmd POST "{{ CFAPI }}/{{ CFZONEID }}/dns_records" {{ CFEMAIL }} {{ CFKEY }} '{{ SRVDATA }}'
    - unless: {{ CFCMD }} exists-srv-target-for-service {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ SERVICE }} {{ HOST }}
