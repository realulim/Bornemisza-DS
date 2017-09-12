install_nodejs_pkgs:
  pkg.installed:
    - pkgs:
      - epel-release
      - nodejs
      - npm

riot:
  npm.installed

babel-core:
  npm.installed

babel-preset-es2015-riot:
  npm.installed

babel-plugin-external-helpers:
  npm.installed
