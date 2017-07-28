{% set CFCMD='/srv/salt/files/basics/cloudflare.sh' %}
{% set CFAPI=pillar['CFAPI'] %}
{% set CFZONEID=pillar['CFZONEID'] %}
{% set CFEMAIL=pillar['CFEMAIL'] %}
{% set CFKEY=pillar['CFKEY'] %}
{% set DOMAIN=pillar['ssldomain'] %}
{% set HOST=pillar['hostname'] %}
{% set HOSTINTERNAL='internal.' + pillar['hostname'] %}
{% set SRVDATA='{"type":"SRV","name":"_db._tcp.' + pillar['ssldomain'] + '.","content":"SRV 1 0 443 ' + pillar['hostname'] + '.","data":{"priority":1,"weight":0,"port":443,"target":"' + pillar['hostname'] + '","service":"_' + pillar['service'] + '","proto":"_tcp","name":"' + pillar['ssldomain'] + '","ttl":"1","proxied":false}}' %}
{% set SERVICE='_' + pillar['service'] + '._tcp.' + pillar['ssldomain'] %}
{% set ADATA='{"type":"A","name":"'+ pillar['hostname'] + '","content":"' + pillar['ip'] + '","ttl":1,"proxied":false}' %}
{% set ADATAINTERNAL='{"type":"A","name":"internal.' + pillar['hostname'] + '","content":"' + pillar['privip'] + '","ttl":1,"proxied":false}' %}

create-SRV-record:
  cmd.run:
    - name: {{ CFCMD }} cmd POST "{{ CFAPI }}/{{ CFZONEID }}/dns_records" {{ CFEMAIL }} {{ CFKEY }} '{{ SRVDATA }}'
    - unless: {{ CFCMD }} exists-SRV-target-for-service {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ SERVICE }} {{ HOST }}

update-A-record:
  cmd.run:
    - name: {{ CFCMD }} update-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} '{{ ADATA }}'
    - onlyif: {{ CFCMD }} host-has-other-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOST }} {{ pillar['ip'] }}

create-A-record:
  cmd.run:
    - name: {{ CFCMD }} cmd POST "{{ CFAPI }}/{{ CFZONEID }}/dns_records" {{ CFEMAIL }} {{ CFKEY }} '{{ ADATA }}'
    - unless: {{ CFCMD }} host-has-this-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOST }} {{ pillar['ip'] }}

update-A-record-internal:
  cmd.run:
    - name: {{ CFCMD }} update-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} '{{ ADATAINTERNAL }}'
    - onlyif: {{ CFCMD }} host-has-other-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOSTINTERNAL }} {{ pillar['privip'] }}

create-A-record-internal:
  cmd.run:
    - name: {{ CFCMD }} cmd POST "{{ CFAPI }}/{{ CFZONEID }}/dns_records" {{ CFEMAIL }} {{ CFKEY }} '{{ ADATAINTERNAL }}'
    - unless: {{ CFCMD }} host-has-this-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOSTINTERNAL }} {{ pillar['privip'] }}
