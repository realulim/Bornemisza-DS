#!/bin/bash

IFS=$'\n'
domain={{ pillar['ssldomain'] }}
service={{ pillar['service'] }}

records=`host -t srv _$service._tcp.$domain.|cut -d" " -f8`

lines=`grep '-' /srv/pillar/appservers.sls|cut -d" " -f4`

highstate=false
for line in $lines ; do
	if [[ ! "$records" =~ "$line" ]] ; then
		printf "[`date`] Gone: $line\n" >> /opt/logs/srvRecords.log
		sed -i "/$line/d" /srv/pillar/haproxy.sls
		highstate=true
	fi
done

if [ "$highstate" = true ]; then
	/usr/bin/bash -c '/opt/bootstrap-bornemisza.sh >> /opt/logs/highstate-srvRecords.log'
fi
