#!/bin/bash
BORNEY=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt

# update system
yum -y update
yum install -y bind-utils
yum clean all

# download and install salt
cd /opt
curl -o bootstrap-salt.sh -L https://bootstrap.saltstack.com
sudo sh bootstrap-salt.sh git develop

# configure salt to run masterless
sed -i.bak 's/\#file_client: remote/file_client: local/g' /etc/salt/minion
sed -i.bak 's/\#master_type: str/master_type: disable/g' /etc/salt/minion
systemctl stop salt-minion

# download and process salt files
curl -o bootstrap-bornemisza.sh -L $BORNEY/bootstrap-bornemisza.sh
chmod u+x bootstrap-bornemisza.sh
sudo sh bootstrap-bornemisza.sh
