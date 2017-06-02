#!/bin/bash

cd /opt
source ./config.sh db

# create state tree
for FILE in top.sls hosts.sls files/hosts cockroachdb.sls
do
	curl -o $SaltLocal/$FILE -L $SaltRemote/$FILE
done

# static pillars
for FILE in top.sls
do
	curl -o $PillarLocal/$FILE -L $PillarRemote/$FILE
done

function getip {
	host $1 | cut -d' ' -f4
}

# determine cluster members hostnames and ips
if [ `grep hostname1 /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	COUNTER=1
	for LOCATION in ${db_HostLocation[@]}
	do
		HOSTNAME=$db_HostPrefix.$LOCATION.$db_Domain
		printf "hostname$COUNTER: $HOSTNAME\n" | tee -a $PillarLocal/basics.sls
		printf "ip$COUNTER: `getip $HOSTNAME`\n" | tee -a $PillarLocal/basics.sls
		let "COUNTER++"
	done
fi

# determine my private IP
if [ `grep privip: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	PRIVIP=`ip addr show $db_privateIpInterface|grep "inet "|cut -d"/" -f1|cut -d" " -f6`
	printf "privip: $PRIVIP\n" | tee -a $PillarLocal/basics.sls
fi

# create server
salt-call -l info state.highstate
