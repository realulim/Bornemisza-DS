/etc/hosts:
  file.managed:
    - source: salt://files/hosts
    - template: jinja
