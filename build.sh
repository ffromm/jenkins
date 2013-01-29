#!/bin/bash
export MAVEN_OPTS=" -Xmx1576m -Xms1024m -XX:MaxPermSize=1024m"
mvn clean install -Plight-test
