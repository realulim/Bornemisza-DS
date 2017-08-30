#!/bin/bash

# $1 absolute path to asadmin binary
# $2 absolute path to password file
# $3 the domain serviced by this Payara instance
# $4 the fully qualified domain name of this Payara instance

{%- set ASADMIN_CMD='$1 --interactive=false --user admin --passwordfile=$2' %}

# create alias for couchdb admin password
{{ ASADMIN_CMD }} delete-password-alias couchdb-admin-password
{{ ASADMIN_CMD }} create-password-alias couchdb-admin-password

# create custom JNDI resource for CouchDB _users database
{{ ASADMIN_CMD }} delete-custom-resource couchdb/Users
{{ ASADMIN_CMD }} create-custom-resource --property service=_db._tcp.$3.:db=_users --restype de.bornemisza.couchdb.da.ConnectionPool --factoryclass de.bornemisza.couchdb.da.ConnectionPoolFactory couchdb/Users
{{ ASADMIN_CMD }} delete-custom-resource couchdb/admin
{{ ASADMIN_CMD }} create-custom-resource --property service=_db._tcp.$3.:db=_users:username=admin:password='\$\{ALIAS\=couchdb-admin-password\}' --restype de.bornemisza.couchdb.da.ConnectionPool --factoryclass de.bornemisza.couchdb.da.ConnectionPoolFactory couchdb/admin

# create SMTP resource
{{ ASADMIN_CMD }} delete-javamail-resource mail/Outgoing
{{ ASADMIN_CMD }} create-javamail-resource --mailuser root --mailhost localhost --fromaddress noreply@$3 mail/Outgoing

# create FQDN system property
{{ ASADMIN_CMD }} delete-system-property FQDN
{{ ASADMIN_CMD }} create-system-properties FQDN=$4
