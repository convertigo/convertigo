ENV ENABLE_JDWP_DEBUG="true"

RUN KUBECTL_MINOR="$(curl -fsSL --retry 5 --retry-delay 2 --retry-connrefused --retry-all-errors https://dl.k8s.io/release/stable.txt | tr -d '\n' | cut -d. -f1,2)" \
  && case "$KUBECTL_MINOR" in v[0-9]*.[0-9]*) ;; *) echo "Unexpected kubectl minor: $KUBECTL_MINOR" >&2; exit 1 ;; esac \
  && apt-get update \
  && apt-get install -y --no-install-recommends apt-transport-https \
  && install -d -m 0755 /etc/apt/keyrings \
  && curl -fsSL https://pkgs.k8s.io/core:/stable:/${KUBECTL_MINOR}/deb/Release.key | gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg \
  && chmod 644 /etc/apt/keyrings/kubernetes-apt-keyring.gpg \
  && echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/${KUBECTL_MINOR}/deb/ /" > /etc/apt/sources.list.d/kubernetes.list \
  && chmod 644 /etc/apt/sources.list.d/kubernetes.list \
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
