#!/bin/bash
set -x
# $1 - the ip address of the node to be removed from the cluster

# Example: ./remove_node.sh 10.4.7.37

AUTH=$(cat /srv/pillar/netrc)
INORIG=/tmp/shardsorig.json

for DB in $(curl -s http://"$1":5984/_all_dbs|jq -re '.[]')
do

IN="/tmp/shardsold-$DB.json"
OUT="/tmp/shardsnew-$DB.json"

BY_NODE="del(.by_node|.\"couchdb@$1\")"
BY_RANGE="del(.by_range|.[]|.[]|select(.|contains(\"@$1\")))"

# retrieve shards document
curl -s -u "$AUTH" http://localhost:5986/_dbs/"$DB" > $INORIG
curl -s -u "$AUTH" http://localhost:5986/_dbs/"$DB"|jq "$BY_NODE"|jq "$BY_RANGE" > $IN

CHANGELOG=$(jq -e '.changelog' $IN | head -n -1 | tail -n +2)
SHARDS=$(jq -e '.by_node' "$INORIG" | tail -n +3 | sed '/],/,$d' | tr -d ,)

echo { > $OUT

echo "\"_id\":" $(jq -e '._id' $IN), >> $OUT
echo "\"_rev\":" $(jq -e '._rev' $IN), >> $OUT
echo "\"shard_suffix\":" $(jq -e '.shard_suffix' $IN), >> $OUT

echo "\"changelog\": [" >> $OUT
echo $CHANGELOG >> $OUT

     for SHARD in $SHARDS
     do
          echo ",[ \"remove\", $SHARD, \"couchdb@$1\" ]" >> $OUT
     done
echo "]", >> $OUT

echo "\"by_node\": $(jq -e '.by_node' $IN)," >> $OUT

echo "\"by_range\": $(jq -e '.by_range' $IN)" >> $OUT

echo "}" >> $OUT

# upload new shards document
/usr/bin/curl -s -u "$AUTH" -X PUT http://localhost:5986/_dbs/"$DB" -d "@$OUT"

done

# retrieve revision of node
REV=$(/usr/bin/curl -s -u "$AUTH" http://localhost:5986/_nodes/couchdb@$1 | jq -re '._rev')

# delete node
/usr/bin/curl -s -u "$AUTH" -X DELETE http://localhost:5986/_nodes/couchdb@$1?rev=$REV
