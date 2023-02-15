RUN (curl -sL https://aka.ms/InstallAzureCLIDeb | bash) \
  && (curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -) \
  && (echo "deb https://apt.kubernetes.io/ kubernetes-xenial main" | tee -a /etc/apt/sources.list.d/kubernetes.list) \
  && apt-get update \
  && apt-get install -y --no-install-recommends kubectl \
       nano less \
       iptables net-tools iputils-ping \
  && rm -rf /var/lib/apt/lists/*
  
RUN curl -sL https://github.com/jpillora/chisel/releases/download/v1.7.6/chisel_1.7.6_linux_amd64.gz | gunzip > /usr/local/bin/chisel \
  && echo "58037ef897ec155a03ea193df4ec618a  /usr/local/bin/chisel" | md5sum -c \
  && chmod a+x /usr/local/bin/chisel \
  && touch /var/log/chisel \
  && chmod 777 /var/log/chisel
