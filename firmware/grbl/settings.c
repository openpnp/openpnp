/*
  settings.c - eeprom configuration handling 
  Part of Grbl

  Copyright (c) 2009-2011 Simen Svale Skogsrud

  Grbl is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Grbl is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Grbl.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <avr/io.h>
#include <math.h>
#include "nuts_bolts.h"
#include "settings.h"
#include "eeprom.h"
#include "wiring_serial.h"
#include <avr/pgmspace.h>

settings_t settings;

// Version 1 outdated settings record
typedef struct {
  double steps_per_mm[3];
  uint8_t microsteps;
  uint8_t pulse_microseconds;
  double default_feed_rate;
  double default_seek_rate;
  uint8_t invert_mask;
  double mm_per_arc_segment;
} settings_v1_t;


void settings_reset() {
  settings.steps_per_mm[X_AXIS] = DEFAULT_X_STEPS_PER_MM;
  settings.steps_per_mm[Y_AXIS] = DEFAULT_Y_STEPS_PER_MM;
  settings.steps_per_mm[Z_AXIS] = DEFAULT_Z_STEPS_PER_MM;
  settings.steps_per_mm[C_AXIS] = DEFAULT_C_STEPS_PER_MM;
  settings.pulse_microseconds = DEFAULT_STEP_PULSE_MICROSECONDS;
  settings.default_feed_rate = DEFAULT_FEEDRATE;
  settings.default_seek_rate = DEFAULT_RAPID_FEEDRATE;
  settings.acceleration = DEFAULT_ACCELERATION;
  settings.mm_per_arc_segment = DEFAULT_MM_PER_ARC_SEGMENT;
  settings.invert_mask = DEFAULT_STEPPING_INVERT_MASK;
  settings.max_jerk = DEFAULT_MAX_JERK;
  
  settings.baud_rate = 9600;
  
  settings.steppers_enable_port = SETTINGS_PORT_DISABLED;
  settings.steppers_enable_bit = 0;
  
  settings.stepping_port = SETTINGS_PORT_DISABLED;
  settings.step_bits[X_AXIS] = 0;
  settings.step_bits[Y_AXIS] = 0;
  settings.step_bits[Z_AXIS] = 0;
  settings.step_bits[C_AXIS] = 0;
  
  settings.direction_port = SETTINGS_PORT_DISABLED;
  settings.direction_bits[X_AXIS] = 0;
  settings.direction_bits[Y_AXIS] = 0;
  settings.direction_bits[Z_AXIS] = 0;
  settings.direction_bits[C_AXIS] = 0;
  
  settings.spindle_enable_port = SETTINGS_PORT_DISABLED;
  settings.spindle_enable_bit = 0;

  settings.spindle_direction_port = SETTINGS_PORT_DISABLED;
  settings.spindle_direction_bit = 0;

  settings.flood_coolant_port = SETTINGS_PORT_DISABLED;
  settings.flood_coolant_bit = 0;

  settings.acceleration_ticks_per_second = 40L;  
}

void settings_dump() {
  printPgmString(PSTR("$0 = ")); 
  printFloat(settings.steps_per_mm[X_AXIS]);
  printPgmString(PSTR(" (steps/mm x)\r\n"));
  
  printPgmString(PSTR("$1 = ")); 
  printFloat(settings.steps_per_mm[Y_AXIS]);
  printPgmString(PSTR(" (steps/mm y)\r\n"));
  
  printPgmString(PSTR("$2 = ")); 
  printFloat(settings.steps_per_mm[Z_AXIS]);
  printPgmString(PSTR(" (steps/mm z)\r\n"));
  
  printPgmString(PSTR("$3 = ")); 
  printFloat(settings.steps_per_mm[C_AXIS]);
  printPgmString(PSTR(" (steps/deg. c)\r\n"));
    
  printPgmString(PSTR("$4 = ")); 
  printInteger(settings.pulse_microseconds);
  printPgmString(PSTR(" (microseconds step pulse)\r\n"));
  
  printPgmString(PSTR("$5 = ")); 
  printFloat(settings.default_feed_rate);
  printPgmString(PSTR(" (mm/min default feed rate)\r\n"));

  printPgmString(PSTR("$6 = ")); 
  printFloat(settings.default_seek_rate);
  printPgmString(PSTR(" (mm/min default seek rate)\r\n"));
  
  printPgmString(PSTR("$7 = ")); 
  printFloat(settings.mm_per_arc_segment);
  printPgmString(PSTR(" (mm/arc segment)\r\n"));
  
  printPgmString(PSTR("$8 = ")); 
  printInteger(settings.invert_mask);   
  printPgmString(PSTR(" (step port invert mask. binary = ")); 
  printIntegerInBase(settings.invert_mask, 2);  
  printPgmString(PSTR(")\r\n"));
  
  printPgmString(PSTR("$9 = ")); 
  printFloat(settings.acceleration);
  printPgmString(PSTR(" (acceleration in mm/sec^2)\r\n"));
  
  printPgmString(PSTR("$10 = ")); 
  printFloat(settings.max_jerk);
  printPgmString(PSTR(" (max instant cornering speed change in delta mm/min)\r\n"));
  
  printPgmString(PSTR("$11 = ")); 
  printInteger(settings.baud_rate);
  printPgmString(PSTR(" (bps baud rate)\r\n"));
  
  printPgmString(PSTR("$12 = ")); 
  printInteger(settings.steppers_enable_port);
  printPgmString(PSTR(" (stepper enable PORT/DDR B=1, C=2, D=3)\r\n"));
  
  printPgmString(PSTR("$13 = ")); 
  printInteger(settings.steppers_enable_bit);
  printPgmString(PSTR(" (stepper enable bit #, negative to invert\r\n"));
  
  printPgmString(PSTR("$14 = ")); 
  printInteger(settings.stepping_port);
  printPgmString(PSTR(" (stepping PORT/DDR B=1, C=2, D=3)\r\n"));
  
  printPgmString(PSTR("$15 = ")); 
  printInteger(settings.step_bits[0]);
  printPgmString(PSTR(" (x step bit #, negative to invert behavior\r\n"));
  
  printPgmString(PSTR("$16 = ")); 
  printInteger(settings.step_bits[1]);
  printPgmString(PSTR(" (y step bit #, negative to invert behavior\r\n"));
  
  printPgmString(PSTR("$17 = ")); 
  printInteger(settings.step_bits[2]);
  printPgmString(PSTR(" (z step bit #, negative to invert behavior\r\n"));
  
  printPgmString(PSTR("$18 = ")); 
  printInteger(settings.step_bits[3]);
  printPgmString(PSTR(" (c step bit #, negative to invert behavior\r\n"));
  
  printPgmString(PSTR("$19 = ")); 
  printInteger(settings.direction_port);
  printPgmString(PSTR(" (direction PORT/DDR B=1, C=2, D=3)\r\n"));
  
  printPgmString(PSTR("$20 = ")); 
  printInteger(settings.direction_bits[0]);
  printPgmString(PSTR(" (x direction bit #, negative to invert behavior\r\n"));
  
  printPgmString(PSTR("$21 = ")); 
  printInteger(settings.direction_bits[1]);
  printPgmString(PSTR(" (y direction bit #, negative to invert behavior\r\n"));
  
  printPgmString(PSTR("$22 = ")); 
  printInteger(settings.direction_bits[2]);
  printPgmString(PSTR(" (z direction bit #, negative to invert behavior\r\n"));
  
  printPgmString(PSTR("$23 = ")); 
  printInteger(settings.direction_bits[3]);
  printPgmString(PSTR(" (c direction bit #, negative to invert behavior\r\n"));
  
  printPgmString(PSTR("$24 = ")); 
  printInteger(settings.spindle_enable_port);
  printPgmString(PSTR(" (spindle enable PORT/DDR B=1, C=2, D=3)\r\n"));
  
  printPgmString(PSTR("$25 = ")); 
  printInteger(settings.spindle_enable_bit);
  printPgmString(PSTR(" (spindle enable bit #, negative to invert\r\n"));
  
  printPgmString(PSTR("$26 = ")); 
  printInteger(settings.spindle_direction_port);
  printPgmString(PSTR(" (spindle enable PORT/DDR B=1, C=2, D=3)\r\n"));
  
  printPgmString(PSTR("$27 = ")); 
  printInteger(settings.spindle_direction_bit);
  printPgmString(PSTR(" (spindle enable bit #, negative to invert\r\n"));
  
  printPgmString(PSTR("$28 = ")); 
  printInteger(settings.flood_coolant_port);
  printPgmString(PSTR(" (flood coolant PORT/DDR B=1, C=2, D=3)\r\n"));
  
  printPgmString(PSTR("$29 = ")); 
  printInteger(settings.flood_coolant_bit);
  printPgmString(PSTR(" (flood coolant bit #, negative to invert\r\n"));
  
  printPgmString(PSTR("$30 = ")); 
  printInteger(settings.acceleration_ticks_per_second);
  printPgmString(PSTR(" (acceleration ticks per second\r\n"));
  
  printPgmString(PSTR("'$x=value' to set parameter or just '$' to dump current settings\r\n"));
}

void write_settings() {
  eeprom_put_char(0, SETTINGS_VERSION);
  memcpy_to_eeprom_with_checksum(1, (char*)&settings, sizeof(settings_t));
}

int read_settings() {
  // Check version-byte of eeprom
  uint8_t version = eeprom_get_char(0);
  
  if (version == SETTINGS_VERSION) {
    // Read settings-record and check checksum
    if (!(memcpy_from_eeprom_with_checksum((char*)&settings, 1, sizeof(settings_t)))) {
      return(FALSE);
    }
  } 
  else if (version == 1) {
    // Migrate from old settings version
    if (!(memcpy_from_eeprom_with_checksum((char*)&settings, 1, sizeof(settings_v1_t)))) {
      return(FALSE);
    }
    settings.acceleration = DEFAULT_ACCELERATION;
    settings.max_jerk = DEFAULT_MAX_JERK;
  }
  else {      
    return(FALSE);
  }
  return(TRUE);
}

// A helper method to set settings from command line
void settings_store_setting(int parameter, double value) {
  switch(parameter) {
    case 0: case 1: case 2: case 3:
    settings.steps_per_mm[parameter] = value; break;
    case 4: settings.pulse_microseconds = round(value); break;
    case 5: settings.default_feed_rate = value; break;
    case 6: settings.default_seek_rate = value; break;
    case 7: settings.mm_per_arc_segment = value; break;
    case 8: settings.invert_mask = trunc(value); break;
    case 9: settings.acceleration = value; break;
    case 10: settings.max_jerk = fabs(value); break;
    case 11: settings.baud_rate = trunc(value); break;
    case 12: settings.steppers_enable_port = trunc(value); break;
    case 13: settings.steppers_enable_bit = trunc(value); break;
    case 14: settings.stepping_port = trunc(value); break;
    case 15: case 16: case 17: case 18:
    settings.step_bits[parameter - 15] = trunc(value); break;
    case 19: settings.direction_port = trunc(value); break;
    case 20: case 21: case 22: case 23:
    settings.direction_bits[parameter - 20] = trunc(value); break;
    case 24: settings.spindle_enable_port = trunc(value); break;
    case 25: settings.spindle_enable_bit = trunc(value); break;
    case 26: settings.spindle_direction_port = trunc(value); break;
    case 27: settings.spindle_direction_bit = trunc(value); break;
    case 28: settings.flood_coolant_port = trunc(value); break;
    case 29: settings.flood_coolant_bit = trunc(value); break;
    case 30: settings.acceleration_ticks_per_second = trunc(value); break;
    default: 
      printPgmString(PSTR("Unknown parameter\r\n"));
      return;
  }
  write_settings();
  printPgmString(PSTR("Stored new setting\r\n"));
}

// Initialize the config subsystem
void settings_init() {
  if(read_settings()) {
    printPgmString(PSTR("'$' to dump current settings\r\n"));
  } else {
    printPgmString(PSTR("Warning: Failed to read EEPROM settings. Using defaults.\r\n"));
    settings_reset();
    write_settings();
    settings_dump();
  }
}
