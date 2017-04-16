ufw:
  pkg:
    - installed
  service.running:
    - listen:
      - pkg: ufw

firewalld:
  service.dead
  pkg.removed
