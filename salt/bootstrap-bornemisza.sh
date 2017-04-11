#!/bin/bash
SALT=/srv/salt
BORNEY=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt

# update system
yum -y update
yum clean all

# download and install salt
cd /opt
curl -o bootstrap-salt.sh -L https://bootstrap.saltstack.com
sudo sh bootstrap-salt.sh git develop

# configure salt to run masterless
sed -i.bak 's/\#file_client: remote/file_client: local/g' /etc/salt/minion

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

# create server
salt-call state.highstate
