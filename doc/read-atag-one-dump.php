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
      "device_id": "6808-1401-3109_15-30-001-xxx",
      "device_status": 16385,
      "connection_status": 23,
      "date_time": 563360139
    },
    "report": {
      "report_time": 563360139,
      "burning_hours": 3087.57,
      "device_errors": "",
      "boiler_errors": "",
      "room_temp": 19.0,
      "outside_temp": 1.5,
      "dbg_outside_temp": 21.7,
      "pcb_temp": 25.4,
      "ch_setpoint": 35.3,
      "dhw_water_temp": 40.0,
      "ch_water_temp": 38.3,
      "dhw_water_pres": 0.0,
      "ch_water_pres": 2.0,
      "ch_return_temp": 32.0,
      "boiler_status": 778,
      "boiler_config": 772,
      "ch_time_to_temp": 0,
      "shown_set_temp": 19.0,
      "power_cons": 1451,
      "tout_avg": 0.9,
      "rssi": 26,
      "current": 39,
      "voltage": 4023,
      "resets": 12,
      "memory_allocation": 3537,
      "details": {
        "boiler_temp": 38.3,
        "boiler_return_temp": 32.0,
        "dhw_flow_rate": 0.0,
        "min_mod_level": 22,
        "rel_mod_level": 2,
        "boiler_capacity": 0,
        "target_temp": 19.0,
        "overshoot": 0.000,
        "max_boiler_temp": 83.0,
        "alpha_used": 0.00236,
        "regulation_state": 2,
        "ch_m_dot_c": 1218.193,
        "c_house": 50042012,
        "r_rad": 0.0016,
        "r_env": 0.0033,
        "alpha": 0.00013,
        "alpha_max": 0.00236,
        "delay": 1131,
        "mu": 0.30,
        "threshold_offs": 15.0,
        "wd_k_factor": 2.0,
        "wd_exponent": 1.4,
        "lmuc_burner_starts": 0,
        "lmuc_burner_hours": 0,
        "lmuc_dhw_hours": 0,
        "KP": 36.100,
        "KI": 0.00568
      }
    },
    "control": {
      "ch_status": 45,
      "ch_control_mode": 0,
      "ch_mode": 1,
      "ch_mode_duration": 0,
      "ch_mode_temp": 19.0,
      "dhw_temp_setp": 60.0,
      "dhw_status": 45,
      "dhw_mode": 1,
      "dhw_mode_temp": 150.0,
      "weather_temp": 1.5,
      "weather_status": 0,
      "vacation_duration": 0,
      "extend_duration": 0,
      "fireplace_duration": 3600
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
      "download_url": "http://firmware.atag-one.com:80/R54",
      "boiler_id": "P154130071",
      "boiler_det_type": 1,
      "language": 0,
      "pressure_unit": 0,
      "temp_unit": 0,
      "time_format": 1,
      "time_zone": 0,
      "summer_eco_mode": 0,
      "summer_eco_temp": 20.0,
      "shower_time_mode": 0,
      "comfort_settings": 0,
      "room_temp_offs": 0.0,
      "outs_temp_offs": 0.0,
      "ch_temp_max": 83.0,
      "ch_vacation_temp": 12.0,
      "start_vacation": 0,
      "wd_k_factor": 2.0,
      "wd_exponent": 1.4,
      "climate_zone": -12.0,
      "wd_temp_offs": 0.0,
      "dhw_legion_day": 7,
      "dhw_legion_time": 195,
      "dhw_boiler_cap": 0,
      "ch_building_size": 2,
      "ch_heating_type": 3,
      "ch_isolation": 1,
      "installer_id": "102749",
      "disp_brightness": 80,
      "ch_mode_vacation": 604800,
      "ch_mode_extend": 3600,
      "support_contact": "Installatiebedrijf",
      "privacy_mode": 0,
      "ch_max_set": 85.0,
      "ch_min_set": 20.0,
      "dhw_max_set": 65.0,
      "dhw_min_set": 40.0,
      "mu": 0.30,
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
          "SSID": "devolo-bcf2af9f1c0d",
          "RSSI": 29,
          "SECURITY": 2
        },
        {
          "SSID": "Ziggo",
          "RSSI": 11,
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
echo "min_mod_level: {$dump->retrieve_reply->report->details->min_mod_level}\n";
