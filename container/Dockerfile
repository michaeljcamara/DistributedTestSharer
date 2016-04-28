# FROM ubuntu
# MAINTAINER Colton McCurdy
# ADD tlb /tlb
# RUN apt-get update && \
#     apt-get upgrade -y && \
#     apt-get install -y  software-properties-common && \
#     add-apt-repository ppa:webupd8team/java -y && \
#     apt-get update && \
#     echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
#     apt-get install -y oracle-java7-installer && \
#     apt-get clean
# RUN wget http://mirror.sdunix.com/apache//ant/binaries/apache-ant-1.9.4-bin.zip && \
#     unzip apache-ant-1.9.4-bin.zip && \
#     mv apache-ant-1.9.4/ /opt/ant && \
#     ln -s /opt/ant/bin/ant /usr/bin/ant && \
#     echo "#!/bin/bash
#     ANT_HOME=/opt/ant
#     PATH=$ANT_HOME/bin:$PATH
#     export PATH ANT_HOME
#     export CLASSPATH=." > /etc/profile.d/ant.sh && \
#     chmod +x /etc/profile.d/ant.sh && \
#     source /etc/profile.d/ant.sh
# RUN ant -version

FROM webratio/java:8
MAINTAINER Colton McCurdy
ADD tlb /tlb

# Installs vim
RUN ["apt-get", "update"]
RUN ["apt-get", "install", "-y", "vim"]

# Installs Ant
ENV ANT_VERSION 1.9.4
RUN cd && \
    wget -q http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xzf apache-ant-${ANT_VERSION}-bin.tar.gz && \
    mv apache-ant-${ANT_VERSION} /opt/ant && \
    rm apache-ant-${ANT_VERSION}-bin.tar.gz
ENV ANT_HOME /opt/ant
ENV PATH ${PATH}:/opt/ant/bin
