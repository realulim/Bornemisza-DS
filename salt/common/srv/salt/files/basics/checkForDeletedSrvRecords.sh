#!/bin/bash

IFS=$'\n'
domain={{ pillar['ssldomain'] }}
service={{ pillar['service'] }}

records=`host -t srv _$service._tcp.$domain.|cut -d" " -f8`

lines=`grep hostname /opt/test.sls|cut -d" " -f2`

for line in $lines ; do
	if [[ ! "$records" =~ "$line" ]] ; then
		printf "[`date`] Gone: $line\n" >> /opt/logs/srvRecords.log
		sed -i "/$line/d" /opt/test.sls
	fi
done
