<?php
/**
 * Get JSON diagnostics and save values to database.
 */

error_reporting(E_ALL);
ini_set('display_errors', 1);

$email = 'address@gmail.com';
$password = 'p6ssw0rd';
$jar = '/usr/local/bin/atag-one.jar';

$json = '';
if (!empty($email)) {
    $json = `java -jar $jar --email $email --password $password`;
} else {
    $json = `java -jar $jar`;
}

// JSON looks like this.
/*
{
    "deviceId": "6808-1401-3109_15-30-001-123",
    "deviceAlias": "CV-ketel",
    "latestReportTime": "2015-12-16 12:42:12",
    "connectedTo": "BCU",
    "burningHours": 296.97,
    "boilerHeatingFor": "-",
    "flameStatus": false,
    "roomTemperature": 17.9,
    "outsideTemperature": 9.7,
    "dhwSetpoint": 60.0,
    "dhwWaterTemperature": 34.5,
    "chSetpoint": 0.0,
    "chWaterTemperature": 17.9,
    "chWaterPressure": 1.1,
    "chReturnTemperature": 17.9,
    "targetTemperature": 17.0,
    "currentMode": "manual",
    "vacationPlanned": false
}
*/


// Extract values from JSON.
$diagnostics = json_decode($json);
if (!$diagnostics) {
    die("Error getting diagnostics: $json");
}

echo "$diagnostics->roomTemperature\n";
echo "$diagnostics->targetTemperature\n";
echo "$diagnostics->chSetpoint\n";
$flameStatusInt = $diagnostics->flameStatus ? 1 : 0;
echo $flameStatusInt;
// etc.


// Create database connection.
$servername = "localhost";
$username = "username";
$password = "password";
$dbname = "myDB";


$conn = new mysqli($servername, $username, $password, $dbname);
// Check connection.
if ($conn->connect_error) {
    die("Database connection failed: " . $conn->connect_error);
}

// Insert into database;
$sql = "INSERT INTO Report (timestamp, roomTemperature, targetTemperature, chSetpoint, flameStatus)
    VALUES (now(), $diagnostics->roomTemperature, $diagnostics->targetTemperature, $diagnostics->chSetpoint, $flameStatusInt)";

if (!$conn->query($sql)) {
    echo "Database error: " . $sql . "<br>" . $conn->error;
}

$conn->close();
