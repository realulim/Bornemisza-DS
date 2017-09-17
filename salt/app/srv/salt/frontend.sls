{%- set FRONTEND_DIR='/opt/frontend' %}
{%- set DOCROOT=''.join(pillar['ssldomain']) %}

workaround-till-centos-7.4-fixes-deps:
  cmd.run:
    - name: rpm -ivh https://kojipkgs.fedoraproject.org/packages/http-parser/2.7.1/3.el7/x86_64/http-parser-2.7.1-3.el7.x86_64.rpm
    - unless: rpm -q http-parser

install_nodejs_pkgs:
  pkg.installed:
    - pkgs:
      - epel-release
      - nodejs
      - npm

install_riot_pkgs:
  npm.installed:
    - pkgs:
      - riot

install_babel_pkgs:
  cmd.run:
    - name: npm install babel-core babel-preset-es2015-riot babel-plugin-external-helpers
    - cwd: {{ FRONTEND_DIR }}
    - unless: npm list babel-core

compile-frontend:
  cmd.run:
    - name: npm run-script compile
    - cwd: {{ FRONTEND_DIR }}
    - creates: {{ FRONTEND_DIR }}/bin/tags.js

/var/www/{{ DOCROOT }}/config.js:
  file.managed:
    - source: salt://files/frontend/config.js
    - template: jinja
