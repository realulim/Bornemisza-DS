remove-couchdb-node-from-cluster:
  cmd.run:
    - name: /opt/scripts/node_remove.sh {{ pillar['privip'] }}
