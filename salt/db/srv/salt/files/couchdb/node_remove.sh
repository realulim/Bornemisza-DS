#!/bin/bash

IP={{ pillar['privip'] }}

AUTH=$(cat /srv/pillar/netrc)

# retrieve revision of node
REV=$(/usr/bin/curl -s -u "$AUTH" http://localhost:5986/_nodes/couchdb@$IP | jq -re '._rev')

# delete node
/usr/bin/curl -s -u "$AUTH" -X DELETE http://localhost:5986/_nodes/couchdb@$IP?rev=$REV
