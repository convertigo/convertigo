# Copyright (c) 2001-2023 Convertigo SA.
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

FROM tomcat:9-jdk17-temurin-focal


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
    gosu \
    sudo \
    tini \
    unzip \
  && apt-get remove -y --purge wget libfreetype6 \
  && apt-get autoremove -y \
  && rm -rf /var/lib/apt/lists/*

%BEGIN%

## create a 'convertigo' user and fix some rights

RUN useradd -s /bin/false -m convertigo \
    && mkdir -p /workspace \
    && chown -R convertigo:convertigo /workspace \
    && chmod -R 777 /workspace \
    && echo "convertigo ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/convertigo \
    && chmod 0440 /etc/sudoers.d/convertigo

## disable unused AJP and Jasper features
## change HTTP port the historic Convertigo port 28080

RUN sed -i.bak \
        -e '/protocol="AJP/d' \
        -e '/JasperListener/d' \
        -e 's/port="8080"/port="28080" maxThreads="64000" relaxedQueryChars="{}[]|"/' \
        -e 's,</Host>,  <Valve className="org.apache.catalina.valves.RemoteIpValve" />\n\
        <Valve className="org.apache.catalina.valves.ErrorReportValve" \
 errorCode.404="webapps/convertigo/404.html"\
 errorCode.0="webapps/convertigo/error.html"\
 showReport="false" showServerInfo="false" />\n\
      </Host>,' \
        -e 's,</Service>,<!--SSL<Connector port="28443" protocol="org.apache.coyote.http11.Http11AprProtocol" SSLEnabled="true" maxThreads="64000" relaxedQueryChars="{}[]|">\n\
      <UpgradeProtocol className="org.apache.coyote.http2.Http2Protocol" />\n\
      <SSLHostConfig>\n\
        <Certificate certificateKeyFile="/certs/key.pem"\n\
                     certificateFile="/certs/cert.pem"\n\
                     certificateChainFile="/certs/chain.pem"\n\
                     type="RSA" />\n\
      </SSLHostConfig>\n\
    </Connector>SSL-->\n  </Service>,' \
        conf/server.xml \
    && sed -i.bak \
        -e 's,<Context>,<Context sessionCookiePath="/">,' \
        -e 's,</Context>,<Manager pathname="" /><CookieProcessor sameSiteCookies="unset" /></Context>,' \
        conf/context.xml \
    && rm -rf webapps/* bin/*.bat conf/server.xml.bak /tmp/* \
    && mkdir webapps/ROOT \
    && chown -R convertigo:convertigo conf temp work logs \
    && chmod -w conf/* \
    && chmod 777 conf/context.xml conf/server.xml

ENV CONVERTIGO_VERSION %VERSION%

ENV CONVERTIGO_WAR_URL https://github.com/convertigo/convertigo/releases/download/$CONVERTIGO_VERSION/convertigo-$CONVERTIGO_VERSION.war

ENV CONVERTIGO_GPG_KEYS 6A7779BB78FE368DF74B708FD4DA8FBEB64BF75F


## download and extract the convertigo webapps
## and remove unnecessary components for the mbaas version

RUN export GNUPGHOME="$(mktemp -d)" \
    && ( gpg --batch --keyserver keyserver.ubuntu.com --recv-keys "$CONVERTIGO_GPG_KEYS" \
    || gpg --batch --keyserver keyserver.pgp.com --recv-keys "$CONVERTIGO_GPG_KEYS" ) \
    && curl -fSL -o /tmp/convertigo.war $CONVERTIGO_WAR_URL \
    && curl -fSL -o /tmp/convertigo.war.asc $CONVERTIGO_WAR_URL.asc \
    && gpg --batch --verify /tmp/convertigo.war.asc /tmp/convertigo.war \
    && mkdir -p webapps/ROOT webapps/convertigo \
    && mkdir /certs && chmod 777 /certs \
    && (cd webapps/convertigo \
        && unzip -q /tmp/convertigo.war \
        && chmod 777 WEB-INF/web.xml WEB-INF/lib WEB-INF/classes \
        && rm -rf /tmp/*)

## copy the ROOT index that redirect to the 'convertigo' webapp

COPY ./root-index.html webapps/ROOT/index.html
COPY ./docker-entrypoint.sh /


WORKDIR /workspace
VOLUME ["/workspace"]
EXPOSE 28080


ENTRYPOINT ["tini", "--", "/docker-entrypoint.sh"]
CMD ["convertigo"]
