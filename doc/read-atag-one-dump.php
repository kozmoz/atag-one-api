<?php
/**
 * Get JSON dump.
 */

error_reporting(E_ALL);
ini_set('display_errors', 1);

$jar = '/usr/local/bin/atag-one.jar';

//$json = `java -jar $jar --dump`;

$json = '{
  "retrieve_reply": {
    "seqnr": 0,
    "status": {
      "device_id": "6808-1401-3109_15-30-001-123",
      "device_status": 16385,
      "connection_status": 23,
      "date_time": 504189887
    },
    "report": {
      "report_time": 504189887,
      "burning_hours": 334.08,
      "device_errors": "",
      "boiler_errors": "",
      "room_temp": 19.4,
      "outside_temp": 11.9,
      "dbg_outside_temp": 22.2,
      "pcb_temp": 25.8,
      "ch_setpoint": 49.6,
      "dhw_water_temp": 47.0,
      "ch_water_temp": 55.5,
      "dhw_water_pres": 0.0,
      "ch_water_pres": 1.7,
      "ch_return_temp": 38.0,
      "boiler_status": 778,
      "boiler_config": 772,
      "ch_time_to_temp": 0,
      "shown_set_temp": 20.0,
      "power_cons": 7123,
      "rssi": 24,
      "current": 48,
      "voltage": 3726,
      "resets": 11,
      "memory_allocation": 2800
    },
    "control": {
      "ch_status": 13,
      "ch_control_mode": 0,
      "ch_mode": 1,
      "ch_mode_duration": 0,
      "ch_mode_temp": 20.0,
      "dhw_temp_setp": 60.0,
      "dhw_status": 5,
      "dhw_mode": 1,
      "dhw_mode_temp": 60.0,
      "weather_temp": 11.9,
      "weather_status": 0,
      "vacation_duration": 0,
      "extend_duration": 0,
      "fireplace_duration": 10800
    },
    "schedules": {
      "ch_schedule": {
        "base_temp": 15.0,
        "entries": [
          [
            [
              420,
              540,
              20.0
            ],
            [
              1020,
              1380,
              20.0
            ]
          ],
          [
            [
              420,
              540,
              20.0
            ],
            [
              1020,
              1380,
              20.0
            ]
          ],
          [
            [
              420,
              540,
              20.0
            ],
            [
              1020,
              1380,
              20.0
            ]
          ],
          [
            [
              420,
              540,
              20.0
            ],
            [
              1020,
              1380,
              20.0
            ]
          ],
          [
            [
              420,
              540,
              20.0
            ],
            [
              1020,
              1380,
              20.0
            ]
          ],
          [
            [
              420,
              1380,
              20.0
            ]
          ],
          [
            [
              420,
              1380,
              20.0
            ]
          ]
        ]
      },
      "dhw_schedule": {
        "base_temp": 60.0,
        "entries": [
          [],
          [],
          [],
          [],
          [],
          [],
          []
        ]
      }
    },
    "configuration": {
      "report_url": "https://reportprd.atag-one.com:443/api/message",
      "download_url": "http://firmware.atag-one.com:80/R42",
      "boiler_id": "P154130071",
      "boiler_det_type": 1,
      "language": 0,
      "pressure_unit": 0,
      "temp_unit": 0,
      "time_format": 1,
      "time_zone": 0,
      "summer_eco_mode": 0,
      "summer_eco_temp": 10.0,
      "shower_time_mode": 0,
      "comfort_settings": 0,
      "room_temp_offs": 0.0,
      "outs_temp_offs": 0.0,
      "ch_temp_max": 55.0,
      "ch_vacation_temp": 12.0,
      "start_vacation": 0,
      "wd_k_factor": 2.0,
      "wd_exponent": 1.4,
      "wd_control_temp": 0.0,
      "climate_zone": -12.0,
      "wd_temp_offs": 0.0,
      "dhw_legion_day": 3,
      "dhw_legion_time": 3,
      "dhw_boiler_cap": 0,
      "ch_building_size": 2,
      "ch_heating_type": 3,
      "ch_isolation": 1,
      "installer_id": "102749",
      "disp_brightness": 75,
      "ch_mode_vacation": 604800,
      "ch_mode_extend": 3600,
      "support_contact": "Installatiebedrijf\r\nTijhaar-Vilsteren BV\r\n\r\n0529458288",
      "privacy_mode": 0,
      "ch_max_set": 85.0,
      "ch_min_set": 20.0,
      "dhw_max_set": 10.0,
      "dhw_min_set": 10.0,
      "mu": 0.00,
      "dhw_legion_enabled": 1,
      "frost_prot_enabled": 0,
      "frost_prot_temp_outs": 0.0,
      "frost_prot_temp_room": 4.0,
      "wdr_temps_influence": 0,
      "max_preheat": 1440
    },
    "acc_status": 2,
    "wifi_scan": {
      "ssid_list": [
        {
          "SSID": "Ziggo",
          "RSSI": 3,
          "SECURITY": 2
        },
        {
          "SSID": "devolo-bcf2af9f1c0d",
          "RSSI": 1,
          "SECURITY": 2
        }
      ]
    }
  }
}';


// Extract values from JSON.
$dump = json_decode($json);
if (!$dump) {
    die("Error getting diagnostics: $json");
}

echo "base_temp: {$dump->retrieve_reply->schedules->ch_schedule->base_temp}\n";
