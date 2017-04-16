ufw:
  pkg:
    - installed
  service.running:
    - listen:
      - pkg: ufw

disable_and_remove_firewalld:
  service.dead:
    - name: firewalld
  pkg.removed:
    - name: firewalld
