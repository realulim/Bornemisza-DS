remove-couchdb-node-from-cluster:
  cmd.run:
    - name: /opt/scripts/remove_node.sh {{ pillar['privip'] }}
