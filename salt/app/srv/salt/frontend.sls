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

# skip compilation if tags.js is the newest file in the directory tree
compile-frontend:
  cmd.run:
    - name: npm run-script compile
    - cwd: {{ FRONTEND_DIR }}
    - unless: find . -type f -printf '%T@ %p\n' | sort -n | tail -1 | cut -f2- -d" " | grep tags.js

copy-frontend-files:
  cmd.run:
    - name: bash -c 'cp -r *.html *.ico *.png *.txt *.xml bin css img js /var/www/{{ DOCROOT }} && chown -R lighttpd:lighttpd /var/www/{{ DOCROOT }}'
    - cwd: {{ FRONTEND_DIR }}
    - onchanges:
      - compile-frontend

/var/www/{{ DOCROOT }}/js/config.js:
  file.managed:
    - source: salt://files/frontend/config.js
    - template: jinja
