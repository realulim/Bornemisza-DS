#!/bin/bash
SaltLocal=/srv/salt
PillarLocal=/srv/pillar
SaltRemote=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt/srv/salt
PillarRemote=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt/srv/pillar

# create state tree
mkdir -p $SaltLocal/files/basics
cd $SaltLocal
curl -L $SaltRemote/top.sls
curl -L $SaltRemote/basics.sls
curl -L $SaltRemote/files/basics/bash.sh

mkdir -p $SaltLocal/files/haproxy
curl -L $SaltRemote/haproxy.sls
curl -L $SaltRemote/files/haproxy/haproxy.cfg

mkdir -p $SaltLocal/files/payara
curl -L $SaltRemote/payara.sls
curl -L $SaltRemote/files/payara/payara.service

# pillars
mkdir -p $PillarLocal
cd $PillarLocal
curl -L $PillarRemote/top.sls
curl -L $PillarRemote/haproxy.sls
sed -ie s/password:/"password: `openssl rand -base64 15`"/ $PillarLocal/haproxy.sls

# create server
salt-call state.highstate
