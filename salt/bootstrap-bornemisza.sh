#!/bin/bash
SALT=/srv/salt
PILLAR=/srv/pillar
BORNEY=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt

# create state tree
mkdir -p $SALT/files/basics
curl -o $SALT/top.sls -L $BORNEY/top.sls
curl -o $SALT/basics.sls -L $BORNEY/basics.sls
curl -o $SALT/files/basics/bash.sh -L $BORNEY/bash.sh

mkdir -p $SALT/files/haproxy
curl -o $SALT/haproxy.sls -L $BORNEY/haproxy/haproxy.sls
curl -o $SALT/files/haproxy/haproxy.cfg -L $BORNEY/haproxy/haproxy.cfg

mkdir -p $SALT/files/payara
curl -o $SALT/payara.sls -L $BORNEY/payara/payara.sls
curl -o $SALT/files/payara/payara.service -L $BORNEY/payara/payara.service

# pillars
mkdir -p $PILLAR
curl -o $PILLAR/top.sls -L $BORNEY/pillar/top.sls
curl -o $PILLAR/haproxy.sls -L $BORNEY/pillar/haproxy.sls
sed -ie s/password:/"password: `openssl rand -base64 15`"/ $PILLAR/haproxy.sls

# create server
salt-call state.highstate
