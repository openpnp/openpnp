/*
 * Copyright (C) 2019 makr@makr.zone
 *
 * This 3D printed feeder is inteded to be used together with the "BlindsFeeder" feeder in OpenPNP. 
 *
 * This file is part of OpenPnP. 
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */
 
 debug_view = true;

// Include the BlindsFeeder Library. 
// To learn the details about available parameters, please read the extensive comments in the library file. 
// The library also defines some 3D printing constants that you might want to tune along with some advice 
// how to setup your slicer. 
include <BlindsFeeder-Library.scad>

// All dimensions in Millimeters.

// Let's create some tape definitions
// Again, please read the extensive comments in the library file to learn what 
// parameters are avaialable and what exactly they mean and do. 
tape0402 = TapeDefinition(
    tape_width=8,
    tape_thickness=0.5,
    pocket_pitch=2,
    pocket_width=2.25,
    tape_play=-0.03,
    cover_play=0.14);

tape0603 = TapeDefinition(
    tape_width=8,
    tape_thickness=1.1,
    pocket_pitch=4,
    pocket_width=2.5,
    tape_play=-0.0,
    cover_play=0.1);

tapeSOT3 = TapeDefinition(
    tape_width=8,
    tape_thickness=0.4,
    pocket_portrusion=1.6,
    pocket_pitch=4,
    pocket_width=4,
    tape_play=0.05,
    cover_play=0.1,
    blinds=false);

tape12mm = TapeDefinition(
    tape_width=12,
    tape_thickness=0.4,
    pocket_portrusion=1.2,
    pocket_pitch=8,
    pocket_width=5,
    tape_play=0.1,
    cover_play=0.1,
    sprocket_thorns=2);
    
trayQLPF64 = TapeDefinition(
    tape_width=16,
    tape_thickness=1.6,
    pocket_pitch=16,
    pocket_width=12.1,
    pocket_length=12.1,
    tape_play=0.1,
    cover_play=0.1,
    blinds=false,
    sprocket_thorns=0);


// Create the feeder array with these tape definitions.
// Note the BlindsFeeder has a myriad of parameters you can tweak, the ones used here are just the most important. 
// See the Library file to learn more. 
BlindsFeeder(
    // Tape length from feeder edge to edge, usually multiples of 4mm. 
    // Other values are supported if you manually adjust the edge distance in the OpenPNP feeder.
    tape_length=84,
    
    // Number of lanes per tape definition.  
    arrayed_tape_lanes=      [1,         1,         1,         1,         1],
    // The arrayed tape definitions.
    arrayed_tapes=           [tape0402,  tape0603,  tapeSOT3,  tape12mm,  trayQLPF64],
    
    debug = debug_view
);

