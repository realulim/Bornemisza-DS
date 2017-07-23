#!/bin/bash

if [ $# -eq 0 ]; then
	echo "No arguments supplied, please use 'app' or 'db'"
	exit -1
fi

BORNEY=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt

mkdir /opt/scripts
cd /opt/scripts
curl -o ./config.sh -L $BORNEY/common/config.sh
source config.sh $1

# update system
yum -y update
yum install -y bind-utils jq
yum clean all

# create state tree
mkdir -p $SaltLocal/files/basics
mkdir -p $PillarLocal
for FILE in $SaltLocal/basics.sls $SaltLocal/files/basics/bash.sh $SaltLocal/files/basics/cloudflarecmd.sh $SaltLocal/letsencrypt.sls $SaltLocal/haproxy.sls
do
        curl -o $FILE -L $SaltRemoteRoot/common/$FILE
done

# download and install salt
curl -o bootstrap-salt.sh -L https://bootstrap.saltstack.com
sudo sh bootstrap-salt.sh git develop

# configure salt to run masterless
sed -i.bak 's/\#file_client: remote/file_client: local/g' /etc/salt/minion
sed -i.bak 's/\#master_type: str/master_type: disable/g' /etc/salt/minion
systemctl stop salt-minion

# download and process salt files
curl -o bootstrap-common.sh -L $BORNEY/common/bootstrap-common.sh
cd /opt
curl -o bootstrap-bornemisza.sh -L $BORNEY/$1/bootstrap-bornemisza.sh
chmod u+x bootstrap-bornemisza.sh /srv/salt/files/basics/cloudflarecmd.sh
sh bootstrap-bornemisza.sh
