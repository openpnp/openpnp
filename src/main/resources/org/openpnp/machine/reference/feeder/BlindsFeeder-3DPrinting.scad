/*
 * Copyright (C) 2019-2020 makr@makr.zone
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
 label_view = false;

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
    cover_play=0.08);

tape0603 = TapeDefinition(
    tape_width=8,
    tape_thickness=1.1,
    pocket_pitch=4,
    pocket_width=2.5,
    tape_play=-0.0,
    cover_play=0.05);

tapeSOT3 = TapeDefinition(
    tape_width=8,
    tape_thickness=0.4,
    pocket_portrusion=1.6,
    pocket_pitch=4,
    pocket_width=4,
    tape_play=0.05,
    cover_play=0.05,
    blinds=false);

tape12mm = TapeDefinition(
    tape_width=12,
    tape_thickness=0.4,
    pocket_portrusion=1.2,
    pocket_pitch=8,
    pocket_width=5,
    tape_play=0.1,
    cover_play=0.05,
    sprocket_thorns=2);
    
trayQLPF64 = TapeDefinition(
    tape_width=16,
    tape_thickness=1.6,
    pocket_pitch=16,
    pocket_width=12.1,
    pocket_length=12.1,
    tape_play=0.1,
    cover_play=0.05,
    blinds=false,
    sprocket_thorns=0);


// Create the feeder array with these tape definitions.
// Note the BlindsFeeder has a myriad of parameters you can tweak, the ones used here are just the most important. 
// See the Library file to learn more. 
rotate([0, 0, 180]) BlindsFeeder(
    // Tape length from feeder edge to edge (not including the margin), usually multiples of 4mm. 
    // Other values are supported if you manually adjust the default 2mm edge distance in the OpenPNP feeder.
    tape_length=100,
    
    // For OCR, add a margin at the begin of the tape.
    margin_length_begin=20,
    // For OCR, blinds are closed when the cover is flush with tape begin.
    blinds_closed_when_flush=true,
    
    // Define the lanes with number, tape definitinon, part label (String array with multiple lines).
    arrayed_tape_lanes=      [
        LaneDefinition(1, tape0402,   ["R0402-1K"]), 
        LaneDefinition(1, tape0402,   ["R0402-100K"]), 
        LaneDefinition(1, tape0603,   ["C0603-10uF"]), 
        LaneDefinition(1, tapeSOT3,   ["SOT23_HV-\\","DMN24H3D5L"]), 
        LaneDefinition(1, tape12mm,   ["CSD16340Q3"]), 
        LaneDefinition(1, trayQLPF64, ["C1810_HV-\\", "4.7nF"]), 
        ],
    
    label=label_view,
    tray=(! label_view),
    cover=(! label_view),
    debug=debug_view
);

