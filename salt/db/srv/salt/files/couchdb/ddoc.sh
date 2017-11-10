#!/bin/bash

# $1 base url to CouchDB

AUTH=$(cat /srv/pillar/netrc)
REV=$(/usr/bin/curl -s -u "$AUTH" "$1"/_users/_design/User|jq -re '._rev')

if [ "$REV" = 'null' ]; then
	/usr/bin/curl -s -u "$AUTH" -X PUT "$1"/_users/_design/User -d '@/home/couchpotato/ddoc/User.json'
else
	DDOC=$(> /home/couchpotato/ddoc/User.json sed "s/_design\/User\",/_design\/User\",\"_rev\": \"$REV\",/g")
	/usr/bin/curl -s -u "$AUTH" -X PUT "$1"/_users/_design/User -d "$DDOC"
fi
