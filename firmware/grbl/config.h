/*
  config.h - compile time configuration
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

#ifndef config_h
#define config_h

#define BAUD_RATE 38400

// Updated default pin-assignments from 0.6 onwards 
// (see bottom of file for a copy of the old config)

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

#define STEPPERS_ENABLE_DDR     DDRC
#define STEPPERS_ENABLE_PORT    PORTC
#define STEPPERS_ENABLE_BIT         4

// For performance reasons it is neccesary to have all of the
// step outputs on one port. They cannot be split across
// multiple ports.
#define STEPPING_DDR       DDRD
#define STEPPING_PORT      PORTD
#define X_STEP_BIT           2
#define Y_STEP_BIT           3
#define Z_STEP_BIT           4
#define C_STEP_BIT           5

// For performance reasons it is neccesary to have all of the
// direction outputs on one port. They cannot be split across
// multiple ports.
#define DIRECTION_DDR      DDRC
#define DIRECTION_PORT     PORTC
#define X_DIRECTION_BIT      0
#define Y_DIRECTION_BIT      1
#define Z_DIRECTION_BIT      2
#define C_DIRECTION_BIT      3


// For performance reasons it is neccesary to have all of the
// limit inputs on one port. They cannot be split across
// multiple ports.
#define LIMIT_DDR      DDRB
#define LIMIT_PORT     PORTB
#define X_LIMIT_BIT          0
#define Y_LIMIT_BIT          1
#define Z_LIMIT_BIT          2
#define C_LIMIT_BIT          3

#define SPINDLE_ENABLE_DDR DDRD
#define SPINDLE_ENABLE_PORT PORTD
#define SPINDLE_ENABLE_BIT 6

#define SPINDLE_DIRECTION_DDR DDRD
#define SPINDLE_DIRECTION_PORT PORTD
#define SPINDLE_DIRECTION_BIT 7

#define FLOOD_COOLANT_DDR DDRB
#define FLOOD_COOLANT_PORT PORTB
#define FLOOD_COOLANT_BIT 4


// The temporal resolution of the acceleration management subsystem. Higher number
// give smoother acceleration but may impact performance
#define ACCELERATION_TICKS_PER_SECOND 40L

#endif

// Pin-assignments from Grbl 0.5

// #define STEPPERS_ENABLE_DDR     DDRD
// #define STEPPERS_ENABLE_PORT    PORTD
// #define STEPPERS_ENABLE_BIT         2
// 
// #define STEPPING_DDR       DDRC
// #define STEPPING_PORT      PORTC 
// #define X_STEP_BIT           0
// #define Y_STEP_BIT           1
// #define Z_STEP_BIT           2
// #define X_DIRECTION_BIT            3
// #define Y_DIRECTION_BIT            4
// #define Z_DIRECTION_BIT            5
// 
// #define LIMIT_DDR      DDRD
// #define LIMIT_PORT     PORTD
// #define X_LIMIT_BIT          3
// #define Y_LIMIT_BIT          4
// #define Z_LIMIT_BIT          5
// 
// #define SPINDLE_ENABLE_DDR DDRD
// #define SPINDLE_ENABLE_PORT PORTD
// #define SPINDLE_ENABLE_BIT 6
// 
// #define SPINDLE_DIRECTION_DDR DDRD
// #define SPINDLE_DIRECTION_PORT PORTD
// #define SPINDLE_DIRECTION_BIT 7

