#!/bin/bash

# $1 absolute path to asadmin binary
# $2 absolute path to password file

{%- set ASADMIN_CMD='$1 --interactive=false --user admin --passwordfile=$2' %}

# create alias for couchdb admin password
{{ ASADMIN_CMD }} delete-password-alias couchdb-admin-password
{{ ASADMIN_CMD }} create-password-alias couchdb-admin-password

# create custom JNDI resource for CouchDB _users database
{{ ASADMIN_CMD }} delete-custom-resource couchdb/Users
{{ ASADMIN_CMD }} create-custom-resource --property service=_db._tcp.bornemisza.de.:db=_users:username=admin:password='\$\{ALIAS\=couchdb-admin-password\}' --restype de.bornemisza.users.da.couchdb.ConnectionPool --factoryclass de.bornemisza.users.da.couchdb.ConnectionPoolFactory couchdb/Users
