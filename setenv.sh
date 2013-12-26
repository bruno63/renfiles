#!/bin/sh
#export JAVA_HOME=/usr/lib/jvm/java-6-openjdk-amd64
export JAVA_HOME=`/usr/libexec/java_home -v 1.7`
echo "JAVA_HOME=<$JAVA_HOME>"
which java
java -version

export ANT_HOME=/opt/apache-ant-1.8.2
echo "ANT_HOME=$ANT_HOME"
which ant
ant -version

export JRE_16=$JAVA_HOME/jre
echo "JRE_16=<$JRE_16>"

export PATH=$JAVA_HOME/bin:$ANT_HOME/bin:$PATH
which git
git --version
