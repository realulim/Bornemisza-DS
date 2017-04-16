#!/bin/bash
SaltLocal=/srv/salt
PillarLocal=/srv/pillar
SaltRemote=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt/srv/salt
PillarRemote=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt/srv/pillar

# create state tree
mkdir -p $SaltLocal/files/basics
mkdir -p $SaltLocal/files/haproxy
mkdir -p $SaltLocal/files/payara
for FILE in top.sls basics.sls files/basics/bash.sh haproxy.sls files/haproxy/haproxy.cfg payara.sls files/payara/payara.service ufw.sls
do
	curl -o $SaltLocal/$FILE -L $SaltRemote/$FILE
done

# pillars
mkdir -p $PillarLocal
for FILE in top.sls basics.sls haproxy.sls payara.sls
do
	curl -o $PillarLocal/$FILE -L $PillarRemote/$FILE
done

function generatepw {
	openssl rand -hex 12
}

# create passwords
sed -ie s/stats-password:/"stats-password: `generatepw`"/ $PillarLocal/haproxy.sls
sed -ie s/asadmin-password:/"asadmin-password: `generatepw`"/ $PillarLocal/payara.sls
sed -ie s/asadmin-master-password:/"asadmin-master-password: `generatepw`"/ $PillarLocal/payara.sls
chmod 400 $PillarLocal/haproxy.sls $PillarLocal/payara.sls

# determine hostname
sed -ie s/hostname:/"hostname: `uname -n`"/ $PillarLocal/basics.sls

# create server
salt-call -l info state.highstate
