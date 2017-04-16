ufw:
  pkg:
    - installed
  service.running:
    - listen:
      - pkg: ufw

remove_unneeded_pkgs:
  pkg.removed:
    - pkgs:
      - firewalld
