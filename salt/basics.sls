install_basics:
  pkg.installed:
    - pkgs:
      - nano

/etc/profile.d/bash.sh:
  file.managed:
    - source: salt://files/basics/bash.sh
