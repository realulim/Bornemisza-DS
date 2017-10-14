{%- set FRONTEND_DIR='/opt/frontend' %}
{%- set DOCROOT=''.join(pillar['ssldomain']) %}

install_nodejs_pkgs:
  pkg.installed:
    - pkgs:
      - nodejs
      - npm

install_riot_pkgs:
  cmd.run:
    - name: npm install riot babel-core babel-preset-es2015-riot babel-plugin-external-helpers
    - cwd: {{ FRONTEND_DIR }}
    - onlyif: npm list riot babel-core babel-preset-es2015-riot babel-plugin-external-helpers|grep "UNMET DEPENDENCY"

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
