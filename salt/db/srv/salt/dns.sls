{% set CFCMD='/srv/salt/files/basics/cloudflarecmd.sh' %}
{% set CFAPI=pillar['CFAPI'] %}
{% set CFZONEID=pillar['CFZONEID'] %}
{% set CFEMAIL=pillar['CFEMAIL'] %}
{% set CFKEY=pillar['CFKEY'] %}
{% set DOMAIN=pillar['ssldomain'] %}
{% set HOST=pillar['sslhost'] %}
{% set DATA='{"type":"SRV","name":"_db._tcp.' + pillar['ssldomain'] + '.","content":"SRV 1 0 443 ' + pillar['sslhost'] + '.","data":{"priority":1,"weight":0,"port":443,"target":"' + pillar['sslhost'] + '","service":"_db","proto":"_tcp","name":"' + pillar['ssldomain'] + '","ttl":"1","proxied":false}}' %}

create-srv-record:
  cmd.run:
    - name: {{ CFCMD }} POST "{{ CFAPI }}/{{ CFZONEID }}/dns_records" {{ CFEMAIL }} {{ CFKEY }} {{ DOMAIN }} {{ HOST }} {{ DATA }}
    - unless: {{ CFCMD }} GET "{{ CFAPI }}/{{ CFZONEID }}/dns_records" {{ CFEMAIL }} {{ CFKEY }} | jq '.result|.[]|select(.type=="SRV")|select(.data.target=="'{{ HOST }}'")' | grep SRV
