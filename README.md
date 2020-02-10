# ATAG One API
## Get diagnostic data from your ATAG One

Get diagnostic data from your ATAG One thermostat with just one simple command.

A typical response message looks like this:

    {
        "deviceId": "6808-1401-3109_15-30-001-123",
        "latestReportTime": "2019-01-26 13:20:04",
        "connectedTo": "BCU",
        "burningHours": 41.4,
        "boilerHeatingFor": "-",
        "flameStatus": false,
        "roomTemperature": 18.1,
        "outsideTemperature": 13,
        "dhwSetpoint": 60,
        "dhwWaterTemperature": 32.3,
        "chSetpoint": 1.4,
        "chWaterTemperature": 18.1,
        "chWaterPressure": 1.5,
        "chReturnTemperature": 18.1
        "targetTemperature" : 18
    }

## Usage
### Modes of Operation

atag-one.jar has two modes of operation; _Local_ and _Remote_.  

* In local mode; it connects directly to the thermostat within the local network. 
* In remote mode; it connects to the ATAG One portal.

### Local Mode

Some examples. To execute these examples, it expects the _java_ executable to be available in the classpath.   

Find the ATAG One thermostat within your local network, connect to it and output brief diagnostic data: 

    $ java -jar atag-one.jar

Set the target room temperature to 20.5 degrees celsius:

    $ java -jar atag-one.jar --set 20.5

Connect to thermostat at IP 10.0.1.12, skip authorization proces and set target room temperature to 20.5 degrees celsius:

    $ java -jar atag-one.jar --set 20.5 --skip-auth-request 10.0.1.12

Connect to thermostat and dump all the available info:

    $ java -jar atag-one.jar --dump

### Remote Mode

Get diagnostic data via the ATAG One internet portal:

    $ java -jar atag-one.jar --email user@gmail.com --password p6ssw0rd

Set the target room temperature to 20.5 degrees celsius via the ATAG One internet portal:

    $ java -jar atag-one.jar --email user@gmail.com --password p6ssw0rd --set 20.5

## Disclaimer

All the trademarks used are the property of their respective owners. 

This project is not affiliated with Atag, the manufacturer of the ATAG One thermostat, in any way.
 
The software is provided "as is", without warranty of any kind.

## Website

ATAG One API project **website** - http://atag.one

ATAG One API project **wiki** - https://github.com/kozmoz/atag-one-api/wiki
 
ATAG One manufacturer **product** website - https://www.atag-one.com  

