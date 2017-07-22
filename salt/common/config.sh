#!/bin/bash

SaltLocal=/srv/salt
PillarLocal=/srv/pillar
SaltRemoteRoot=https://raw.githubusercontent.com/realulim/Bornemisza/master/salt/
SaltRemote=$SaltRemoteRoot/$1/srv/salt
PillarRemote=$SaltRemoteRoot/$1/srv/pillar
CFAPI=https://api.cloudflare.com/client/v4/zones/

# you have to change these
app_HostCount=3
app_HostPrefix=app
app_Domain=fra.bornemisza.de
app_publicIpInterface=eth0
app_privateIpInterface=eth1

db_HostLocation=(fra ams lon)
db_HostPrefix=db
db_Domain=bornemisza.de
db_publicIpInterface=eth0
db_privateIpInterface=eth1

function getip {
	host $1 | cut -d' ' -f4
}

function generatepw {
	openssl rand -hex 12
}

function getprivip {
	VARNAME=$1_privateIpInterface
	ip addr show ${!VARNAME}|grep "inet "|cut -d"/" -f1|cut -d" " -f6
}

function getinternalip {
	dig -t txt $1 +short | tr -d '"'
}

function cloudflareget {
	curl -s "$1" -H "X-Auth-Email: $2" -H "X-Auth-Key: $3" -H "Content-Type: application/json"
}

function cloudflarepost {
	$DATA=getsrvrecorddata $4 $5
	curl -s -X POST "$1" -H "X-Auth-Email: $2" -H "X-Auth-Key: $3" -H "Content-Type: application/json" --data "$data"
}

function cloudflareput {
	$DATA=getsrvrecorddata $4 $5
	curl -s -X PUT "$1" -H "X-Auth-Email: $2" -H "X-Auth-Key: $3" -H "Content-Type: application/json" --data "$data"
}

function getsrvrecorddata {
	echo "{\"type\":\"SRV\",\"name\":\"_db._tcp."$1".\",\"content\":\"SRV 1 0 443 "$2".\",\"data\":{\"priority\":1,\"weight\":0,\"port\":443,\"target\":\""$2"\",\"service\":\"_db\",\"proto\":\"_tcp\",\"name\":\""$1"\",\"ttl\":\"1\",\"proxied\":false}}
}
