#!/bin/bash
#
# - Concatenate the stub and the jar into a new executable:
#
# cat stub.sh Main.jar > main.sh
# 3- Make the new file executable:
#
# chmod +x main.sh
#
#
# https://coderwall.com/p/ssuaxa/how-to-make-a-jar-file-linux-executable
# https://mesosphere.com/blog/2013/12/07/executable-jars/
#

set -o errexit -o pipefail -o nounset

MYSELF=`which "$0" 2>/dev/null`
[ $? -gt 0 -a -f "$0" ] && MYSELF="./$0"

java=java
if [ "${JAVA_HOME:=xx}" != "xx" ] ; then
    java="$JAVA_HOME/bin/java"
fi

exec "${java}" ${java_args:=} -jar ${MYSELF} "$@"

exit $?
