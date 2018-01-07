#!/bin/bash

# $1 - the ip address of the node to be removed from the cluster

# Example: ./remove_node.sh 10.4.7.37

AUTH=$(cat /srv/pillar/netrc)
IN=/tmp/shardsold.json
OUT=/tmp/shardsnew.json

# retrieve revision of node
REV=$(/usr/bin/curl -s -u "$AUTH" http://localhost:5986/_nodes/couchdb@$1 | jq -re '._rev')

# delete node
/usr/bin/curl -s -u "$AUTH" -X DELETE http://localhost:5986/_nodes/couchdb@$1?rev=$REV
