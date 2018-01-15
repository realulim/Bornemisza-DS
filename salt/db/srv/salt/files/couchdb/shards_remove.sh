#!/bin/bash

IP={{ pillar['privip'] }}

AUTH=$(cat /srv/pillar/netrc)
INORIG=/tmp/shardsorig.json

for DB in $(curl -s -u "$AUTH" http://localhost:5986/_dbs/_all_docs|jq -re '.rows|.[]|.id'|grep -v _)
do

IN="/tmp/shardsold-$DB.json"
OUT="/tmp/shardsnew-$DB.json"

BY_NODE="del(.by_node|.\"couchdb@$IP\")"
BY_RANGE="del(.by_range|.[]|.[]|select(.|contains(\"@$IP\")))"

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
          echo ",[ \"remove\", $SHARD, \"couchdb@$IP\" ]" >> $OUT
     done
echo "]", >> $OUT

echo "\"by_node\": $(jq -e '.by_node' $IN)," >> $OUT

echo "\"by_range\": $(jq -e '.by_range' $IN)" >> $OUT

echo "}" >> $OUT

# upload new shards document
/usr/bin/curl -s -u "$AUTH" -X PUT http://localhost:5986/_dbs/"$DB" -d "@$OUT"

done
