remove-couchdb-node-from-cluster:
  cmd.run:
    - name: /opt/script/remove_node.sh {{ pillar['privip'] }}
