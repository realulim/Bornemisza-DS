#!/bin/bash
SALT=/srv/salt
BORNEY=https://github.com/realulim/Bornemisza/salt
cd /opt

# download and install salt
curl -o bootstrap-salt.sh -L https://bootstrap.saltstack.com
sudo sh bootstrap-salt.sh git develop

# configure salt to run masterless
sed -i.bak 's/\#file_client: remote/file_client: local/g' /etc/salt/minion

# create state tree
mkdir -p $SALT/files/basics
curl -o $SALT/top.sls -L $BORNEY/top.sls
curl -o $SALT/basics.sls -L $BORNEY/basics.sls
curl -o $SALT/files/basics/bash.sh -L $BORNEY/bash.sh

salt-call state.highstate
