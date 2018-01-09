#!/bin/bash

# $1 - the ip address of the node to be removed from the cluster

# Example: ./remove_node.sh 10.4.7.37

AUTH=$(cat /srv/pillar/netrc)
IN=/tmp/shardsold.json
OUT=/tmp/shardsnew.json

for DB in $(curl -s http://"$1":5984/_all_dbs|jq -re '.[]'|grep -v _)
do

# retrieve shards document
curl -s -u "$AUTH" http://localhost:5986/_dbs/"$DB"|jq 'del(.by_node|."couchdb@$1")'|jq 'del(.by_range|.[]|.[]|select(.|contains("@$1")))' > $IN

SHARDS=$(jq -e '.by_node' "$IN" | tail -n +3 | sed '/],/,$d')
CHANGELOG=$(jq -e '.changelog' $IN | head -n -1 | tail -n +2)

echo { > $OUT

echo "\"_id\":" $(jq -e '._id' $IN), >> $OUT
echo "\"_rev\":" $(jq -e '._rev' $IN), >> $OUT
echo "\"shard_suffix\":" $(jq -e '.shard_suffix' $IN), >> $OUT

echo "\"changelog\": [" >> $OUT
echo $CHANGELOG , >> $OUT

     for SHARD in $SHARDS
     do
          echo ", ["
          echo "    \"remove\","
          echo "    $SHARD"
          echo "    \"couchdb@$1\""
          echo "  ]"
     done

done

# retrieve revision of node
REV=$(/usr/bin/curl -s -u "$AUTH" http://localhost:5986/_nodes/couchdb@$1 | jq -re '._rev')

# delete node
/usr/bin/curl -s -u "$AUTH" -X DELETE http://localhost:5986/_nodes/couchdb@$1?rev=$REV
