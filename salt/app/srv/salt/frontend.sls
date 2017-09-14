workaround-till-centos-7.4-fixes-deps:
  cmd.run:
    - name: rpm -ivh https://kojipkgs.fedoraproject.org/packages/http-parser/2.7.1/3.el7/x86_64/http-parser-2.7.1-3.el7.x86_64.rpm

install_nodejs_pkgs:
  pkg.installed:
    - pkgs:
      - epel-release
      - nodejs
      - npm

install_riot_pkgs:
  npm.installed:
    - riot
    - babel-core
    - babel-preset-es2015-riot
    - babel-plugin-external-helpers
