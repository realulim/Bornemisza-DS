#!/bin/bash

# $1 - the ip address of the node wanting to add the shards of the cluster

# Example: ./shards.sh 10.4.7.37

AUTH=$(cat /srv/pillar/netrc)
IN=/tmp/shardsold.json
OUT=/tmp/shardsnew.json

for DB in $(curl -s http://"$1":5984/_all_dbs|jq -re '.[]'|grep -v _)
do

# retrieve current shards file
/usr/bin/curl -s -u "$AUTH" http://localhost:5986/_dbs/"$DB" | jq . > $IN

if ! grep -q $1 $IN ; then

     # create new shards file only if this IP isn't already in there
     SHARDS=$(jq -e '.by_node' "$IN" | tail -n +3 | sed '/],/,$d')

     CHANGELOG=$(jq -e '.changelog' $IN | head -n -1 | tail -n +2)

     echo { > $OUT

     echo "\"_id\":" $(jq -e '._id' $IN), >> $OUT
     echo "\"_rev\":" $(jq -e '._rev' $IN), >> $OUT
     echo "\"shard_suffix\":" $(jq -e '.shard_suffix' $IN), >> $OUT

     echo "\"changelog\": [" >> $OUT
     echo $CHANGELOG , >> $OUT
     echo $CHANGELOG | sed "s/@[^\"]*\"/@$1\"/g" >> $OUT
     echo ], >> $OUT

     echo "\"by_node\":" $(jq -e '.by_node' $IN | sed "/]$/a , \"couchdb@$1\": [" | head -n -1) $SHARDS , >> $OUT

     echo "\"by_range\":" $(jq -e '.by_range' $IN | sed "/\[/a \    \"couchdb@$1\",") >> $OUT

     echo } >> $OUT

     # upload new shards file
     /usr/bin/curl -s -u "$AUTH" -X PUT http://localhost:5986/_dbs/"$DB" -d "@$OUT"

fi

done
