/*
  settings.h - eeprom configuration handling 
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

#ifndef settings_h
#define settings_h


#include <math.h>
#include <inttypes.h>

#define GRBL_VERSION "0.6b"

// Version of the EEPROM data. Will be used to migrate existing data from older versions of Grbl
// when firmware is upgraded. Always stored in byte 0 of eeprom
#define SETTINGS_VERSION 102

#define SETTINGS_PORT_DISABLED 0
#define SETTINGS_PORT_B 1
#define SETTINGS_PORT_C 2
#define SETTINGS_PORT_D 3


// Current global settings (persisted in EEPROM from byte 1 onwards)
typedef struct {
  double steps_per_mm[4]; // G: x, y, z, c
  uint8_t microsteps;
  uint8_t pulse_microseconds;
  double default_feed_rate;
  double default_seek_rate;
  uint8_t invert_mask;
  double mm_per_arc_segment;
  double acceleration;
  double max_jerk;

  uint32_t baud_rate;
  
  uint8_t steppers_enable_port;
  int8_t steppers_enable_bit;
  
  uint8_t stepping_port;
  int8_t step_bits[4];
  
  uint8_t direction_port;
  int8_t direction_bits[4];
  
  uint8_t spindle_enable_port;
  int8_t spindle_enable_bit;
  
  uint8_t spindle_direction_port;
  int8_t spindle_direction_bit;
  
  uint8_t flood_coolant_port;
  int8_t flood_coolant_bit;
  
  uint32_t acceleration_ticks_per_second;
  
} settings_t;
extern settings_t settings;

// Initialize the configuration subsystem (load settings from EEPROM)
void settings_init();

// Print current settings
void settings_dump();

// A helper method to set new settings from command line
void settings_store_setting(int parameter, double value);

/*
  Mega328P Arduino Pin Mapping
  Digital 0     PD0 (RX)
  Digital 1     PD1 (TX)
  Digital 2     PD2
  Digital 3     PD3
  Digital 4     PD4
  Digital 5     PD5
  Digital 6     PD6
  Digital 7     PD7
  Digital 8     PB0
  Digital 9     PB1
  Digital 10    PB2
  Digital 11    PB3 (MOSI)
  Digital 12    PB4 (MISO)
  Digital 13    PB5 (SCK)
  
  Analog 0      PC0
  Analog 1      PC1
  Analog 2      PC2
  Analog 3      PC3
  Analog 4      PC4
*/

// Default settings (used when resetting eeprom-settings)
#define MICROSTEPS 8
#define DEFAULT_X_STEPS_PER_MM (94.488188976378*MICROSTEPS)
#define DEFAULT_Y_STEPS_PER_MM (94.488188976378*MICROSTEPS)
#define DEFAULT_Z_STEPS_PER_MM (94.488188976378*MICROSTEPS)
// G: For C_AXIS, equate 1mm == 1 degree of rotation (imaginary pulley circumference of 360mm)
#define DEFAULT_C_STEPS_PER_MM (5.556*MICROSTEPS)
#define DEFAULT_STEP_PULSE_MICROSECONDS 30
#define DEFAULT_MM_PER_ARC_SEGMENT 0.1
#define DEFAULT_RAPID_FEEDRATE 480.0 // in millimeters per minute
#define DEFAULT_FEEDRATE 480.0
#define DEFAULT_ACCELERATION (DEFAULT_FEEDRATE/100.0)
#define DEFAULT_MAX_JERK 50.0
#define DEFAULT_STEPPING_INVERT_MASK 0

#define DEFAULT_BAUD_RATE               38400

#define DEFAULT_STEPPERS_ENABLE_PORT    SETTINGS_PORT_C
#define DEFAULT_STEPPERS_ENABLE_BIT     4

#define DEFAULT_STEPPING_PORT           SETTINGS_PORT_D
#define DEFAULT_X_STEP_BIT              2
#define DEFAULT_Y_STEP_BIT              3
#define DEFAULT_Z_STEP_BIT              4
#define DEFAULT_C_STEP_BIT              5

#define DEFAULT_DIRECTION_PORT          SETTINGS_PORT_C
#define DEFAULT_X_DIRECTION_BIT         0
#define DEFAULT_Y_DIRECTION_BIT         1
#define DEFAULT_Z_DIRECTION_BIT         2
#define DEFAULT_C_DIRECTION_BIT         3

#define DEFAULT_LIMIT_PORT              SETTINGS_PORT_B
#define DEFAULT_X_LIMIT_BIT             0
#define DEFAULT_Y_LIMIT_BIT             1
#define DEFAULT_Z_LIMIT_BIT             2
#define DEFAULT_C_LIMIT_BIT             3

#define DEFAULT_SPINDLE_ENABLE_PORT     SETTINGS_PORT_D
#define DEFAULT_SPINDLE_ENABLE_BIT      6

#define DEFAULT_SPINDLE_DIRECTION_PORT  SETTINGS_PORT_D
#define DEFAULT_SPINDLE_DIRECTION_BIT   7

#define DEFAULT_FLOOD_COOLANT_PORT      SETTINGS_PORT_B
#define DEFAULT_FLOOD_COOLANT_BIT       4

#define DEFAULT_ACCELERATION_TICKS_PER_SECOND 40L

#endif
