RUN (curl -sL https://aka.ms/InstallAzureCLIDeb | bash) \
  && (curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -) \
  && (echo "deb https://apt.kubernetes.io/ kubernetes-xenial main" | tee -a /etc/apt/sources.list.d/kubernetes.list) \
  && apt-get update \
  && apt-get install -y kubectl \
  && rm -rf /var/lib/apt/lists/*
  
RUN curl -sL https://github.com/jpillora/chisel/releases/download/1.3.1/chisel_linux_amd64.gz | gunzip > /usr/local/bin/chisel \
  && echo "53095fa70c73beda8d8cdec18d9aa9d5  /usr/local/bin/chisel" | md5sum -c \
  && chmod a+x /usr/local/bin/chisel
