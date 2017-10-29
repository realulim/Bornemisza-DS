#!/bin/bash

IFS=$'\n'
domain={{ pillar['ssldomain'] }}
service={{ pillar['service'] }}

records=`host -t srv _$service._tcp.$domain.|cut -d" " -f8`

lines=`grep hostname /srv/pillar/haproxy.sls|cut -d" " -f2`

highstate=false
for line in $lines ; do
	if [[ ! "$records" =~ "$line" ]] ; then
		printf "[`date`] Gone: $line\n" >> /opt/logs/srvRecords.log
		sed -i "/$line/d" /srv/pillar/haproxy.sls
		highstate=true
	fi
done

if [ "$highstate" = true ]; then
	/usr/bin/bash -c '/usr/bin/salt-call -l error state.highstate >> /opt/logs/highstate.log'
fi
