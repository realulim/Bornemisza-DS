#!/bin/bash
SaltLocal=/srv/salt
PillarLocal=/srv/pillar
SaltRemote=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt/app/srv/salt
PillarRemote=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt/app/srv/pillar

# create state tree
mkdir -p $SaltLocal/files/basics
mkdir -p $SaltLocal/files/haproxy
mkdir -p $SaltLocal/files/payara
for FILE in top.sls basics.sls files/basics/bash.sh haproxy.sls files/haproxy/haproxy.cfg payara.sls files/payara/payara.service files/payara/hazelcast.xml ufw.sls
do
	curl -o $SaltLocal/$FILE -L $SaltRemote/$FILE
done

# static pillars
mkdir -p $PillarLocal
for FILE in top.sls basics.sls
do
	curl -o $PillarLocal/$FILE -L $PillarRemote/$FILE
done

function generatepw {
	openssl rand -hex 12
}

# dynamic pillar: haproxy
if [[ ! -e $PillarLocal/haproxy.sls ]]; then
	curl -o $PillarLocal/haproxy.sls -L $PillarRemote/haproxy.sls
	sed -ie s/stats-password:/"stats-password: `generatepw`"/ $PillarLocal/haproxy.sls
	chmod 400 $PillarLocal/haproxy.sls
fi

# dynamic pillar: payara
if [[ ! -e $PillarLocal/payara.sls ]]; then
	curl -o $PillarLocal/payara.sls -L $PillarRemote/payara.sls
	sed -ie s/asadmin-password:/"asadmin-password: `generatepw`"/ $PillarLocal/payara.sls
	sed -ie s/asadmin-master-password:/"asadmin-master-password: `generatepw`"/ $PillarLocal/payara.sls
	chmod 400 $PillarLocal/payara.sls
fi

function getip {
	host $1 | cut -d' ' -f4
}

# determine my hostname and ip
HOSTNAME=`uname -n`
IP=`getip $HOSTNAME`
sed -ie s/hostname:/"hostname: $HOSTNAME"/ $PillarLocal/basics.sls
sed -ie s/ip:/"ip: $IP"/ $PillarLocal/basics.sls

# determine cluster members hostnames and ips
COUNTER=0
for HOSTNAME in db.fra.bornemisza.de app.ams.bornemisza.de app.par.bornemisza.de
do
	let COUNTER++
	printf "hostname$COUNTER: $HOSTNAME\n" >> $PillarLocal/basics.sls
	printf "ip$COUNTER: `getip $HOSTNAME`\n" >> $PillarLocal/basics.sls
done

# create server
salt-call state.highstate
