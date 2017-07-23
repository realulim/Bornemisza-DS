#!/bin/bash

# $1 http method (GET, POST or PUT)
# $2 url
# $3 cfemail
# $4 cfkey
# $5 data to send (only with POST or PUT)

if [ -n "$5" ]; then
	/usr/bin/curl -s -X $1 "$2" -H "X-Auth-Email: $3" -H "X-Auth-Key: $4" -H "Content-Type: application/json" --data "$5"
else
	/usr/bin/curl -s -X $1 "$2" -H "X-Auth-Email: $3" -H "X-Auth-Key: $4" -H "Content-Type: application/json"
fi
