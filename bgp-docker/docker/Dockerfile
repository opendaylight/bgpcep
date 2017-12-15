FROM ubuntu:17.04

MAINTAINER Claudio D. Gasparini <claudio.gasparini@pantheon.tech>

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install --assume-yes apt-utils

RUN apt-get update && apt-get install -y git sudo wget

RUN apt-get update && apt-get install -y --no-install-recommends apt-utils software-properties-common

RUN apt-get update && apt-get install -y iputils-ping

# Install Java.
RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list
RUN echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886 && apt-get update && apt-get install -y curl dnsutils oracle-java8-installer ca-certificates

# Define working directory.
WORKDIR /odl

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# Update dpkg repositories
RUN apt-get update 

# Clean Up APT when finished
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*


# Define default command.
CMD ["bash"]

# Get maven 3.3.9
RUN wget --no-verbose -O /tmp/apache-maven-3.3.9-bin.tar.gz http://www-eu.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz

# Install maven
RUN tar xzf /tmp/apache-maven-3.3.9-bin.tar.gz -C /opt/
RUN ln -s /opt/apache-maven-3.3.9 /opt/maven
RUN ln -s /opt/maven/bin/mvn /usr/local/bin
RUN rm -f /tmp/apache-maven-3.3.9-bin.tar.gz
ENV MAVEN_HOME /opt/maven

# Clone integration distribution
RUN git clone https://git.opendaylight.org/gerrit/integration/distribution

# Add ODL repositories
RUN mkdir /root/.m2

RUN wget --no-verbose -O /root/.m2/settings.xml https://raw.githubusercontent.com/opendaylight/odlparent/master/settings.xml

WORKDIR "/odl/distribution/karaf"
RUN ["mvn", "install"]

WORKDIR "/odl/distribution/karaf/target/assembly"

