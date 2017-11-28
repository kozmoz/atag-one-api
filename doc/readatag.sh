#!/bin/bash
#
# Cron script to read ATAG ONE thermostat.
#

#set -o errexit -o nounset -o xtrace
set -o nounset -o xtrace

MYSELF=`which "$0" 2>/dev/null`
MYSELF=`dirname $MYSELF`
[ $? -gt 0 -a -f "$0" ] && MYSELF="."

java=java
if [ "${JAVA_HOME:=xx}" != "xx" ] ; then
    java="$JAVA_HOME/bin/java"
fi

result=`$java -jar /usr/local/bin/atag-one.jar -o csv`

if [ $? -eq 0 ] ; then

room_temp=`echo ${result} | cut --delimiter=\  -f 1`
outside_temp=`echo ${result} | cut --delimiter=\  -f 2`
pressure=`echo ${result} | cut --delimiter=\  -f 3`
water_temp=`echo ${result} | cut --delimiter=\  -f 4`
water_return_temp=`echo ${result} | cut --delimiter=\  -f 5`
target_temp=`echo ${result} | cut --delimiter=\  -f 6`
ch_setpoint=`echo ${result} | cut --delimiter=\  -f 7`
flame_status=`echo ${result} | cut --delimiter=\  -f 8`
boiler_heating_for=`echo ${result} | cut --delimiter=\  -f 9`

# Insert values in database.
#
#CREATE TABLE IF NOT EXISTS CV
#(
#  id               INT(11)  NOT NULL AUTO_INCREMENT,
#  date             DATETIME NOT NULL,
#  room_temp        LONG     NOT NULL,
#  outside_temp     LONG     NOT NULL,
#  pressure         LONG     NOT NULL,
#  water_temp       LONG     NOT NULL,
#  water_return_temp LONG     NOT NULL,
#  PRIMARY KEY (id)
#);
#
# ALTER TABLE CV CHANGE room_temp room_temp DOUBLE NOT NULL default 0;
# ALTER TABLE CV CHANGE outside_temp outside_temp DOUBLE NOT NULL default 0;
# ALTER TABLE CV CHANGE pressure pressure  DOUBLE NOT NULL default 0;
# ALTER TABLE CV CHANGE water_temp water_temp  DOUBLE NOT NULL default 0;
# ALTER TABLE CV CHANGE water_return_temp water_return_temp  DOUBLE NOT NULL default 0;
# ALTER TABLE CV ADD target_temp  DOUBLE NOT NULL default 0;
# ALTER TABLE CV ADD ch_setpoint  DOUBLE NOT NULL default 0;
# ALTER TABLE CV ADD flame_status  INT NOT NULL default 0;
# ALTER TABLE CV ADD boiler_heating_for VARCHAR(10);

echo "use electricity; INSERT INTO CV (date, room_temp, outside_temp, pressure, water_temp, water_return_temp, target_temp, ch_setpoint, flame_status, boiler_heating_for) VALUES (now(), $room_temp, $outside_temp, $pressure, $water_temp, $water_return_temp, $target_temp, $ch_setpoint, $flame_status, '$boiler_heating_for');" | mysql -u electricity -pel8761
 
fi

exit 0
