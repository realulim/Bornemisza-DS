#!/bin/bash

IP={{ pillar['privip'] }}

AUTH=$(cat /srv/pillar/netrc)

for DB in $(curl -s -u "$AUTH" http://localhost:5986/_dbs/_all_docs|jq -re '.rows|.[]|.id'|grep -v _)
do

IN="/tmp/shardsold-$DB.json"
OUT="/tmp/shardsnew-$DB.json"

# retrieve current shards file
/usr/bin/curl -s -u "$AUTH" http://localhost:5986/_dbs/"$DB" | jq . > $IN

if ! cat $IN|jq .by_node|grep $IP\": ; then

     # create new shards file only if this IP isn't already in there
     SHARDS=$(jq -e '.by_node' "$IN" | tail -n +3 | sed '/.*\(]\|}\).*/d')

     SHARDCOUNT=$(echo "$SHARDS" | wc -l)
     CHANGELOGOLD=$(jq -e '.changelog' $IN | head -n -1 | tail -n +2)
     CHANGELOGNEW=$(jq -e '.changelog|.[:'$SHARDCOUNT']' $IN | head -n -1 | tail -n +2)

     echo { > $OUT

     echo "\"_id\":" $(jq -e '._id' $IN), >> $OUT
     echo "\"_rev\":" $(jq -e '._rev' $IN), >> $OUT
     echo "\"shard_suffix\":" $(jq -e '.shard_suffix' $IN), >> $OUT

     echo "\"changelog\": [" >> $OUT
     echo $CHANGELOGOLD , >> $OUT
     echo $CHANGELOGNEW | sed "s/@[^\"]*\"/@$IP\"/g" >> $OUT
     echo ], >> $OUT

     echo "\"by_node\":" $(jq -e '.by_node' $IN | sed "/]$/a , \"couchdb@$IP\": [" | head -n -1) $SHARDS ]}, >> $OUT

     echo "\"by_range\":" $(jq -e '.by_range' $IN | sed "/\[/a \    \"couchdb@$IP\",") >> $OUT

     echo } >> $OUT

     # upload new shards file
     /usr/bin/curl -s -u "$AUTH" -X PUT http://localhost:5986/_dbs/"$DB" -d "@$OUT"

fi

done
