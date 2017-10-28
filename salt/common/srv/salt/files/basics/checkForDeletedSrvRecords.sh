#!/bin/bash

IFS=$'\n'
domain={{ pillar['ssldomain'] }}
service={{ pillar['service'] }}

records=`host -t srv _$service._tcp.$domain.|cut -d" " -f8`

lines=`grep hostname /srv/pillar/haproxy.sls|cut -d" " -f2`

for line in $lines ; do
	if [[ ! "$records" =~ "$line" ]] ; then
		printf "[`date`] Gone: $line\n" >> /opt/logs/srvRecords.log
		sed -i "/$line/d" /srv/pillar/haproxy.sls
	fi
done
