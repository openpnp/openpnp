/*
  coolant_control.c - coolant control methods
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

#include "coolant_control.h"
#include "settings.h"
#include "config.h"

#include <avr/io.h>

void coolant_init()
{
  FLOOD_COOLANT_DDR |= 1<<FLOOD_COOLANT_BIT;
}

void coolant_flood(int on) 
{
  if (on) {
    FLOOD_COOLANT_PORT |= 1<<FLOOD_COOLANT_BIT;
  }
  else {
    FLOOD_COOLANT_PORT &= ~(1<<FLOOD_COOLANT_BIT);
  }
}
