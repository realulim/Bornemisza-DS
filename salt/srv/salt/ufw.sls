ufw:
  pkg:
    - installed
  service.running:
    - listen:
      - pkg: ufw

firewalld:
  pkg.removed
