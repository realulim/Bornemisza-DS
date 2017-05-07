#!/bin/bash

if [ $# -eq 0 ]; then
	echo "No arguments supplied, please use 'app' or 'db'"
	exit -1
fi

BORNEY=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt

cd /opt
curl -o ./config.sh -L $BORNEY/config.sh
source config.sh $1

# update system
yum -y update
yum install -y bind-utils
yum clean all

# create state tree
mkdir -p $SaltLocal/files/basics
mkdir -p $PillarLocal
for FILE in $SaltLocal/basics.sls $SaltLocal/files/basics/bash.sh $SaltLocal/letsencrypt.sls
do
        curl -o $FILE -L $SaltRemoteRoot/common/$FILE
done

# determine my hostname and ip
HOSTNAME=`domainname -f`
IP=`host $HOSTNAME | cut -d' ' -f4`
printf "hostname: $HOSTNAME\nip: $IP\n" | tee $PillarLocal/basics.sls

# download and install salt
curl -o bootstrap-salt.sh -L https://bootstrap.saltstack.com
sudo sh bootstrap-salt.sh git develop

# configure salt to run masterless
sed -i.bak 's/\#file_client: remote/file_client: local/g' /etc/salt/minion
sed -i.bak 's/\#master_type: str/master_type: disable/g' /etc/salt/minion
systemctl stop salt-minion

# download and process salt files
curl -o bootstrap-bornemisza.sh -L $BORNEY/$1/bootstrap-bornemisza.sh
chmod u+x bootstrap-bornemisza.sh
sh bootstrap-bornemisza.sh
