{% set CFCMD='/srv/salt/files/basics/cloudflare.sh' %}
{% set CFAPI=pillar['CFAPI'] %}
{% set CFZONEID=pillar['CFZONEID'] %}
{% set CFEMAIL=pillar['CFEMAIL'] %}
{% set CFKEY=pillar['CFKEY'] %}
{% set DOMAIN=pillar['ssldomain'] %}
{% set HOST=pillar['hostname'] %}
{% set HOSTINTERNAL='internal.' + pillar['hostname'] %}
{% set ADATA='{"type":"A","name":"' + pillar['hostname'] + '","content":"' + pillar['ip'] + '","ttl":120,"proxied":false}' %}
{% set ADATAINTERNAL='{"type":"A","name":"internal.' + pillar['hostname'] + '","content":"' + pillar['privip'] + '","ttl":120,"proxied":false}' %}

update-A-record:
  cmd.run:
    - name: {{ CFCMD }} update-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOST }} '{{ ADATA }}'
    - onlyif: {{ CFCMD }} host-has-other-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOST }} {{ pillar['ip'] }}

create-A-record:
  cmd.run:
    - name: {{ CFCMD }} create-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} '{{ ADATA }}'
    - unless: {{ CFCMD }} host-has-this-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOST }} {{ pillar['ip'] }}

update-A-record-internal:
  cmd.run:
    - name: {{ CFCMD }} update-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOSTINTERNAL }} '{{ ADATAINTERNAL }}'
    - onlyif: {{ CFCMD }} host-has-other-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOSTINTERNAL }} {{ pillar['privip'] }}

create-A-record-internal:
  cmd.run:
    - name: {{ CFCMD }} create-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} '{{ ADATAINTERNAL }}'
    - unless: {{ CFCMD }} host-has-this-A-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOSTINTERNAL }} {{ pillar['privip'] }}
