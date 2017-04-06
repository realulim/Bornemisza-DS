install_basics:
  pkg.installed:
    - pkgs:
      - java-1.8.0-openjdk.x86_64
      - nano

/etc/profile.d/bash.sh:
  file.managed:
    - source: salt://files/basics/bash.sh
