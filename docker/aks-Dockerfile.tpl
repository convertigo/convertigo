ENV ENABLE_JDWP_DEBUG="true"

RUN KUBECTL=$(curl -L -s https://dl.k8s.io/release/stable.txt | cut -d. -f-2) \
  && (curl -sL https://aka.ms/InstallAzureCLIDeb | bash) \
  && (curl -fsSL https://pkgs.k8s.io/core:/stable:/$KUBECTL/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg) \
  && (echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/$KUBECTL/deb/ /" | sudo tee /etc/apt/sources.list.d/kubernetes.list) \
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
