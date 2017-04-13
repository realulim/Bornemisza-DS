#!/bin/bash
SaltLocal=/srv/salt
PillarLocal=/srv/pillar
SaltRemote=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt/srv/salt
PillarRemote=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt/srv/pillar

# create state tree
mkdir -p $SaltLocal/files/basics
mkdir -p $SaltLocal/files/haproxy
mkdir -p $SaltLocal/files/payara
for FILE in top.sls basics.sls files/basics/bash.sh haproxy.sls files/haproxy/haproxy.cfg payara.sls files/payara/payara.service
do
	curl -o $SaltLocal/$FILE -L $SaltRemote/$FILE
done

# pillars
mkdir -p $PillarLocal
for FILE in top.sls haproxy.sls
do
	curl -o $PillarLocal/$FILE -L $PillarRemote/$FILE
done

# create passwords
sed -ie s/password:/"password: `openssl rand -base64 15`"/ $PillarLocal/haproxy.sls
chmod 400 $PillarLocal/haproxy.sls

# create server
salt-call state.highstate
