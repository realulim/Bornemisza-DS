remove-shards:
  cmd.run:
    - name: /opt/scripts/shards_remove.sh

remove-couchdb-node-from-cluster:
  cmd.run:
    - name: /opt/scripts/node_remove.sh
