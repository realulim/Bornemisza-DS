#!/bin/bash

# ask for floating IP and determine its hostname and domain
read -p 'floating IP: ' FLOATIP
if [ `grep floatip: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	printf "floatip: $FLOATIP\n" >> $PillarLocal/basics.sls
	FLOATHOST=`host $FLOATIP | cut -d' ' -f5 | sed -r 's/(.*)\..*/\1/'`
	printf "floathost: $FLOATHOST\n" | tee -a $PillarLocal/basics.sls
	FLOATDOMAIN=`printf $FLOATHOST | rev | awk -F. '{ print $1"."$2 }' | rev`
	printf "floatdomain: $FLOATDOMAIN\n" | tee -a $PillarLocal/basics.sls
fi

# ask for autonomous system number
read -p 'ASN: ' ASN
if [ `grep ASN: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	printf "ASN: $ASN\n" >> $PillarLocal/basics.sls
fi

# ask for BGP password
read -p 'BGP: ' BGP
if [ `grep BGP: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	printf "BGP: $BGP\n" >> $PillarLocal/basics.sls
fi

# ask for Cloudflare API key
read -p 'Cloudflare API Key: ' CFKEY
if [ `grep CFKEY: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	printf "CFKEY: $CFKEY\n" >> $PillarLocal/basics.sls
fi

# ask for Cloudflare email (username of Cloudflare account)
read -p 'Cloudflare Email: ' CFEMAIL
if [ `grep CFEMAIL: /srv/pillar/basics.sls | wc -l` -eq 0 ]; then
	printf "CFEMAIL: $CFEMAIL\n" >> $PillarLocal/basics.sls
fi

chmod -R 400 $PillarLocal
