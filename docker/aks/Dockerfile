# Copyright (c) 2001-2020 Convertigo SA.
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU Affero General Public License
# as published by the Free Software Foundation; either version 3
# of the License, or (at your option) any later version.

# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.

# You should have received a copy of the GNU General Public License
# along with this program; if not, see<http://www.gnu.org/licenses/>.

FROM tomcat:9-jdk11-openjdk


MAINTAINER Nicolas Albert nicolasa@convertigo.com

## force SWT to use GTK2 instead of GTK3 (no Xulrunner support)
ENV SWT_GTK3 0

ENV CATALINA_HOME /usr/local/tomcat
RUN mkdir -p "$CATALINA_HOME"
WORKDIR $CATALINA_HOME

RUN apt-get update -y \
  && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    dirmngr \
    gnupg \
    unzip \
  && rm -rf /var/lib/apt/lists/*

RUN (curl -sL https://aka.ms/InstallAzureCLIDeb | bash) \
  && (curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -) \
  && (echo "deb https://apt.kubernetes.io/ kubernetes-xenial main" | tee -a /etc/apt/sources.list.d/kubernetes.list) \
  && apt-get update \
  && apt-get install -y kubectl \
  && rm -rf /var/lib/apt/lists/*
  
RUN curl -sL https://github.com/jpillora/chisel/releases/download/1.3.1/chisel_linux_amd64.gz | gunzip > /usr/local/bin/chisel \
  && echo "53095fa70c73beda8d8cdec18d9aa9d5  /usr/local/bin/chisel" | md5sum -c \
  && chmod a+x /usr/local/bin/chisel

## grab gosu for easy step-down from root and tini for signal handling

ENV GOSU_VERSION 1.11
ENV GOSU_GPG_KEYS B42F6819007F00F88E364FD4036A9C25BF357DD4
ENV TINI_VERSION 0.18.0
ENV TINI_GPG_KEYS 6380DC428747F6C393FEACA59A84159D7001A4E5

RUN export GNUPGHOME="$(mktemp -d)" \
  && ( gpg --batch --keyserver ha.pool.sks-keyservers.net --recv-keys "$GOSU_GPG_KEYS" \
  || gpg --batch --keyserver pgp.mit.edu --recv-keys "$GOSU_GPG_KEYS" \
  || gpg --batch --keyserver keyserver.ubuntu.com --recv-keys "$GOSU_GPG_KEYS" \
  || gpg --batch --keyserver keyserver.pgp.com --recv-keys "$GOSU_GPG_KEYS" ) \
  && curl -o /usr/local/bin/gosu -fSL "https://github.com/tianon/gosu/releases/download/${GOSU_VERSION}/gosu-$(dpkg --print-architecture)" \
  && curl -o /usr/local/bin/gosu.asc -fSL "https://github.com/tianon/gosu/releases/download/${GOSU_VERSION}/gosu-$(dpkg --print-architecture).asc" \
  && gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
  && rm /usr/local/bin/gosu.asc \
  && chmod +x /usr/local/bin/gosu \
  && ( gpg --batch --keyserver ha.pool.sks-keyservers.net --recv-keys "$TINI_GPG_KEYS" \
  || gpg --batch --keyserver pgp.mit.edu --recv-keys "$TINI_GPG_KEYS" \
  || gpg --batch --keyserver keyserver.ubuntu.com --recv-keys "$TINI_GPG_KEYS" \
  || gpg --batch --keyserver keyserver.pgp.com --recv-keys "$TINI_GPG_KEYS" ) \
  && curl -o /usr/local/bin/tini -fSL "https://github.com/krallin/tini/releases/download/v${TINI_VERSION}/tini-$(dpkg --print-architecture)" \
  && curl -o /usr/local/bin/tini.asc -fSL "https://github.com/krallin/tini/releases/download/v${TINI_VERSION}/tini-$(dpkg --print-architecture).asc" \
  && gpg --batch --verify /usr/local/bin/tini.asc /usr/local/bin/tini \
  && rm /usr/local/bin/tini.asc \
  && chmod +x /usr/local/bin/tini \
  && rm -rf /tmp/*


## create a 'convertigo' user and fix some rights

RUN useradd -s /bin/false -m convertigo \
    && mkdir -p /workspace/lib /workspace/classes \
    && chown -R convertigo:convertigo /workspace

## disable unused AJP, APR and Jasper features
## change HTTP port the historic Convertigo port 28080

RUN sed -i.bak \
        -e '/protocol="AJP/d' \
        -e '/AprLifecycleListener/d' \
        -e '/JasperListener/d' \
        -e 's/port="8080"/port="28080" maxThreads="64000" relaxedQueryChars="{}[]|"/' \
        -e 's,</Host>,  <Valve className="org.apache.catalina.valves.RemoteIpValve" />\n      </Host>,' \
        conf/server.xml \
    && sed -i.bak \
        -e 's,<Context>,<Context sessionCookiePath="/">,' \
        conf/context.xml \
    && rm -rf webapps/* bin/*.bat conf/server.xml.bak /tmp/* \
    && mkdir webapps/ROOT \
    && chown -R convertigo:convertigo conf temp work logs \
    && chmod -w conf/*

ENV CONVERTIGO_VERSION 7.7.1

ENV CONVERTIGO_WAR_URL https://github.com/convertigo/convertigo/releases/download/$CONVERTIGO_VERSION/convertigo-$CONVERTIGO_VERSION.war

ENV CONVERTIGO_GPG_KEYS 6A7779BB78FE368DF74B708FD4DA8FBEB64BF75F


## download and extract the convertigo webapps
## and remove unnecessary components for the mbaas version

RUN export GNUPGHOME="$(mktemp -d)" \
    && ( gpg --batch --keyserver ha.pool.sks-keyservers.net --recv-keys "$CONVERTIGO_GPG_KEYS" \
    || gpg --batch --keyserver pgp.mit.edu --recv-keys "$CONVERTIGO_GPG_KEYS" \
    || gpg --batch --keyserver keyserver.ubuntu.com --recv-keys "$CONVERTIGO_GPG_KEYS" \
    || gpg --batch --keyserver keyserver.pgp.com --recv-keys "$CONVERTIGO_GPG_KEYS" ) \
    && curl -fSL -o /tmp/convertigo.war $CONVERTIGO_WAR_URL \
    && curl -fSL -o /tmp/convertigo.war.asc $CONVERTIGO_WAR_URL.asc \
    && gpg --batch --verify /tmp/convertigo.war.asc /tmp/convertigo.war \
    && mkdir -p webapps/ROOT webapps/convertigo \
    && (cd webapps/convertigo \
        && unzip -q /tmp/convertigo.war \
        && (chmod -f a+x WEB-INF/xvnc/* || true) \
        && (test "$(dpkg --print-architecture)" != "i386" && rm -rf WEB-INF/xulrunner WEB-INF/xvnc WEB-INF/lib/swt_* || true) \
        && rm -rf /tmp/*)

## copy the ROOT index that redirect to the 'convertigo' webapp

COPY ./root-index.html webapps/ROOT/index.html
COPY ./docker-entrypoint.sh /


WORKDIR /workspace
VOLUME ["/workspace"]
EXPOSE 28080


ENTRYPOINT ["tini", "--", "/docker-entrypoint.sh"]
CMD ["convertigo"]
