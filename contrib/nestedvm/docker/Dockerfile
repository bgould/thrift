FROM bcg1/nestedvm:thrift
MAINTAINER BCG, bgould@users.noreply.github.com

RUN apt-get -y install \
    ant ant-optional maven flex bison pkg-config libtool automake wget \
    --no-install-recommends

ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64
ENV nestedvm /usr/local/nestedvm

VOLUME /usr/local/src/thrift
WORKDIR /usr/local/src/thrift
