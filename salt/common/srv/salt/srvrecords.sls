{% set CFCMD='/srv/salt/files/basics/cloudflare.sh' %}
{% set CFAPI=pillar['CFAPI'] %}
{% set CFZONEID=pillar['CFZONEID'] %}
{% set CFEMAIL=pillar['CFEMAIL'] %}
{% set CFKEY=pillar['CFKEY'] %}
{% set HOST=pillar['hostname'] %}
{% set SRVDATA='{"type":"SRV","name":"_db._tcp.' + pillar['ssldomain'] + '.","content":"SRV 1 0 443 ' + pillar['hostname'] + '.","data":{"priority":1,"weight":0,"port":443,"target":"' + pillar['hostname'] + '","service":"_' + pillar['service'] + '","proto":"_tcp","name":"' + pillar['ssldomain'] + '","ttl":"120","proxied":false}}' %}
{% set SERVICE='_' + pillar['service'] + '._tcp.' + pillar['ssldomain'] %}

create-SRV-record:
  cmd.run:
    - name: {{ CFCMD }} cmd POST "{{ CFAPI }}/{{ CFZONEID }}/dns_records" {{ CFEMAIL }} {{ CFKEY }} '{{ SRVDATA }}'
    - unless: {{ CFCMD }} exists-SRV-target-for-service {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ SERVICE }} {{ HOST }}
