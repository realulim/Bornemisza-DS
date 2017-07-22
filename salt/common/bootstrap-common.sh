#!/bin/bash

cd /opt/scripts
source ./config.sh $1

# determine my hostname, domain and public ip
HOSTNAME=`domainname -f`
SSLDOMAIN=`printf $HOSTNAME | rev | awk -F. '{ print $1"."$2 }' | rev`
if [ `grep hostname: $PillarLocal/basics.sls | wc -l` -eq 0 ]; then
	VARNAME=$1_publicIpInterface
	IP=`ip addr show ${!VARNAME}|grep "inet "|cut -d"/" -f1|cut -d" " -f6`
	printf "hostname: $HOSTNAME\nip: $IP\n" | tee $PillarLocal/basics.sls
        printf "ssldomain: $SSLDOMAIN\n" | tee -a $PillarLocal/basics.sls
fi

# determine my private IP
if [ `grep privip: $PillarLocal/basics.sls | wc -l` -eq 0 ]; then
	printf "privip: `getprivip $1`\n" | tee -a $PillarLocal/basics.sls
fi

# ask for Cloudflare API key
if [ `grep CFKEY: $PillarLocal/basics.sls | wc -l` -eq 0 ]; then
	read -p 'Cloudflare API Key: ' CFKEY
	printf "CFKEY: $CFKEY\n" >> $PillarLocal/basics.sls
	CFK=$CFKEY
fi

# ask for Cloudflare email (username of Cloudflare account)
if [ `grep CFEMAIL: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	read -p 'Cloudflare Email: ' CFEMAIL
	printf "CFEMAIL: $CFEMAIL\n" >> $PillarLocal/basics.sls
	CFE=$CFEMAIL
fi

CF_AUTH_PARAMS="-H \"X-Auth-Email: $CFEMAIL\" -H \"X-Auth-Key: $CFKEY\" -H \"Content-Type: application/json\""

echo "DEBUG"
echo "$CF_AUTH_PARAMS"

# determine zone id of domain
if [ `grep CFZONEID: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	CFZONEID=`curl -s "$CFAPI" "$CF_AUTH_PARAMS" | jq '.result|.[]|.id' | tr -d "\""`
echo "$CFZONEID"
	printf "CFZONEID: $CFZONEID\n" | tee -a $PillarLocal/basics.sls

	# write SRV records for domain
	for LOCATION in ${db_HostLocation[@]}
	do
		DBHOSTNAME=$db_HostPrefix.$LOCATION.$db_Domain
		SRVID=`curl -s "$CFAPI/$CFZONEID/dns_records" "$CF_AUTH_PARAMS" | jq '.result|.[]|select(.type=="SRV")|select(.data.target=="$DBHOSTNAME")|.id'| tr -d "\"`
		SRVDATA=`echo '{"type":"SRV","name":"_db._tcp.$SSLDOMAIN.","content":"SRV 1 0 443 $DBHOSTNAME.","data":{"priority":1,"weight":0,"port":443,"target":"$DBHOSTNAME","service":"_db","proto":"_tcp","name":"$SSLDOMAIN","ttl":"1","proxied":false}}'`

		if [ -n "$SRVID" ]; then
			# record exist, so let's update it
			curl -s -X PUT "$CFAPI/$CFZONEID/dns_records/$SRVID" "$CF_AUTH_PARAMS" --data '$SRVDATA'
		else
			# record does not exist, so let's create it
			curl -s -X POST "$CFAPI/$CFZONEID/dns_records" "$CF_AUTH_PARAMS" --data '$SRVDATA'
		fi
	done
fi

if [[ -e /opt/bootstrap.sh ]]; then
	mv /opt/bootstrap.sh /opt/scripts
fi
