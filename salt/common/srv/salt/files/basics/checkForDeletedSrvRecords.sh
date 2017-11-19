#!/bin/bash

IFS=$'\n'
domain={{ pillar['ssldomain'] }}
service={{ pillar['service'] }}
cfns={{ pillar['cfns'] }}

records=$(host -t srv _${service}._tcp.$domain. $cfns|cut -d" " -f8)
lines=$(grep '-' /srv/pillar/"${service}"servers.sls|cut -d" " -f4)

highstate=false
for line in $lines ; do
	if [[ ! "$records" == *"$line"* ]] ; then
		printf "[$(date)] Gone: %s\n" "$line" >> /opt/logs/srvRecords.log
		sed -i "/$line/d" /srv/pillar/"${service}"servers.sls
		highstate=true
	fi
done

if [ "$highstate" = true ]; then
	/usr/bin/bash -c '/opt/bootstrap-bornemisza.sh >> /opt/logs/highstate-srvRecords.log'
fi
