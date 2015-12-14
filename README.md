# ATAG One API
## Get diagnostic data from your ATAG One

Get all diagnostic data from your ATAG One thermostat with just one simple command.

A typical message looks like this:

    {
        "deviceId": "6808-1401-3109_15-30-001-123",
        "deviceAlias": "CV-ketel",
        "latestReportTime": "2015-12-14 12:10:04",
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

Connect to the ATAG One thermostat on your local network and get diagnostic data: 

    $ java -jar atag-one.jar

Get diagnostic data from the ATAG One internet portal:

    $ java -jar atag-one.jar --email user@gmail.com --password p6ssw0rd

Set the target room temperature to 20.5 degrees celsius via the ATAG One internet portal:

    $ java -jar atag-one.jar --email user@gmail.com --password p6ssw0rd --set 20.5

## Disclaimer

All of the trademarks used are the property of their respective owners. 

This project is not affiliated with Atag, the manufacturer of the ATAG One thermostat.
 
The software is provided "as is", without warranty of any kind.

## Website

Project website http://atag.one
 
ATAG One manufacturer product website https://www.atag-one.com  

