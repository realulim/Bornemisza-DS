{% set CFCMD='/srv/salt/files/basics/cloudflare.sh' %}
{% set CFAPI=pillar['CFAPI'] %}
{% set CFZONEID=pillar['CFZONEID'] %}
{% set CFEMAIL=pillar['CFEMAIL'] %}
{% set CFKEY=pillar['CFKEY'] %}
{% set HOST=pillar['hostname'] %}
{% set HOSTINTERNAL='internal.' + pillar['hostname'] %}

delete-A-record:
  cmd.run:
    - name: {{ CFCMD }} delete-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} A {{ HOST }}

delete-A-record-internal:
  cmd.run:
    - name: {{ CFCMD }} delete-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} A {{ HOSTINTERNAL }}

delete-SRV-record:
  cmd.run:
    - name: {{ CFCMD }} delete-SRV-record {{ CFAPI }} {{ CFEMAIL }} {{ CFKEY }} {{ CFZONEID }} {{ HOST }}
