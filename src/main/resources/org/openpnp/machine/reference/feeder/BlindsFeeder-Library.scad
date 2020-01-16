/*
 * Copyright (C) 2019 makr@makr.zone
 *
 * This 3D printed feeder is inteded to be used together with the "BlindsFeeder" feeder in OpenPNP. 
 *
 * TODO: This file is intended to become part of OpenPnP. 
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

// All dimensions in Millimeters.

// Some 3D printing constants:
// This might be somewhat biased towards the Prusa MK3 / Slic3r / PrusaSlicer I used. Please report back to me for different 
// printers / slicers and needed adjustments. 

// Overall layer height. Only tested for 0.2mm. 
layer_height=0.2;
// Standard extrusion width. 
extrusion_width=0.45;
// Minimum sturdy enough wall width.
min_width=0.8;

// Slicer and 3D Printer Setup advice:
// * I based it on PrusaSlicer 2.1.0 "0.2mm SPEED MK3". 
// * Use the same layer height for all layers (might already be that way).
// * Use 2 perimeters.
// * Use 2 solid layers To and Bottom. 
// * Absolutely need the "Detect thin walls" option enabled. 
// * Set your slicer to use one uniform extrusion width for fastest printing. I had to change the "First" and "Top solid infill" layers.



// Font size that will be extruded in single extrusion strokes. This is an empirically optimized value 
// for your printer and the font used.
font_size=2.667;
font_name="Arial Rounded MT Bold:style=Regular";

// Default circles subdivisions $fn. This is often overridden. 
$fn=20;
// Epsilon for Boolean body overlap to render nicely in OpenSCAD's Z-Buffer driven CSG preview.
e=0.01; 

// OpenSCAD has no classes or structs, so we simulate one using an array.
function TapeDefinition(
    // Nominal tape width, only use multiples of 4mm. Use tape_play to adjust.
    tape_width=8,
    // Thickness of the tape at the sprocket holes i.e. not counting embossed pockets.
    tape_thickness=0.5,
    // Pocket portrusion in an ebossed tape (measured from underside of tape).
    pocket_portrusion=0,
    // Pitch of the pockets in the tape. Use 2mm or multiples of 4mm.
    pocket_pitch=2,
    // Pocket width across the tape.
    pocket_width=2.25,
    // Pocket length along the tape if compartments should be printed to contain the parts directly.
    // For normal tapes set to 0.
    pocket_length=0,
    // Tape play. Use this to adjust the precise tape width and the play for print imperfections.
    tape_play=0.0,
    // Horizontal per-side play of the cover to allow for imperfections of the print.
    cover_play=0.1,    
    // Whether the cover has blinds.
    blinds=true, 
    // Nozzle diameter (only used for drag handle - not implemented in the current OpenPNP software feeder).
    nozzle_diameter=1,
    // Print every nth sprocket thorn. Set to 0 to switch off thorns.
    sprocket_thorns=1
    ) =
    [ tape_width, tape_thickness, pocket_portrusion, pocket_pitch, pocket_width, pocket_length, tape_play, cover_play, blinds, nozzle_diameter, sprocket_thorns];
    
    function get_tape_width(tape)=tape[0];
    function get_tape_thickness(tape)=tape[1];
    function get_pocket_portrusion(tape)=tape[2];
    function get_pocket_pitch(tape)=tape[3];
    function get_pocket_width(tape)=tape[4];
    function get_pocket_length(tape)=tape[5];
    function get_tape_play(tape)=tape[6];
    function get_cover_play(tape)=tape[7];
    function get_blinds(tape)=tape[8];
    function get_nozzle_diameter(tape)=tape[9];
    function get_sprocket_thorns(tape)=tape[10];
// End of pseudoclass TapeDefinition


module BlindsFeeder(
    // Tape length from feeder edge to edge, usually multiples of 4mm. 
    // Other values are supported if you manually adjust the edge distance in the OpenPNP feeder.
    tape_length=44,
    
    // Number of lanes per tape type. If set to 0 it is not produced. 
    arrayed_tape_lanes=      [],
    // The arrayed tape definitions.
    arrayed_tapes=           [],
    
    // the wall on each side of the tape. Keep this at multiples of 1mm for automatic OpenPNP support.
    wall_width=1,
    // how much one wall will overlap with the next in addition to wall_width.
    wall_overlap=min_width*0.5,

    // Want to print the tray?
    tray=true,
    // Want to print the covers?
    cover=true,
    
    // How the covers are arranged on the printing bed. 
    // If you want to print very long feeders, set this to false.
    // If you want to print many, many short feeders, set it to true.
    covers_to_the_right=false,
    
    // Gap between tray and covers, as arranged on the printing bed.
    gap=2,
    
    // Tape orientation in the feeder local coordinate system. We are picking parts further and 
    // further back in the tape and as the pick location advances, that is our +X. Consequently in 
    // our 2D local coordinate system, the sprocket holes are on the bottom. 
    // But if this were an automatic feeder, the tape would actually be pushed out in -X. Therefore 
    // the tape orientation is actually -1 by default, the sprocket holes at the top. 
    // You can choose to feed your tapes the other way around and set tape_orientation to +1. This is not
    // recommended as it changes the notion of "Rotation in Tape" in OpenPNP. But it is supported by the 
    // OpenPNP BlindsFeeder and its vision operations. 
    tape_orientation=-1,
    
    // The feeder floor thickness. This feeder is based on the idea that it is taped/glued to a sturdy 
    // surface. No need to be very thick.
    floor_thickness=layer_height*2,
        
    // Sprocket pitch. This is 4mm by EIA-481-C standard.  
    sprocket_pitch=4,
    // Distance of the sprockte hole from the tape edge. This is 1.75mm by EIA-481-C standard.
    sprocket_dist=1.75,
    // Printing diameter of the sprocket thorn at the base and at the top. As the extruded 
    // filament tend to shrink around tight corners, this must usually be enlarged from the 
    // theoretically correct value of 1.5mm. I guess this is highly printer/material/temperature 
    // specific.
    sprocket_diameter0=1.8,
    sprocket_diameter1=1.6,
    // With very thick paper tapes you might want to limit the sprocket thorn height. 
    sprocket_max_height=1,
    
    // If the tape_length is not a multiple of 4mm, the rest is distributed by this alignment.
    // It can be left=0, center=0.5 or right=1 aligned, as seen in the local coordinate system. 
    // With any value but 0.5 you need to manually set your edge distances in OpenPNP.
    tape_align=0.5, 
    
    // Unify wall height to maximum thickness of paper tapes, 1.1mm as per EIA-481-C.
    // Set to 0 if you don't want to unify and save some printing time.
    tape_max_thickness=0,//1.1, 
    
    // Standard fiducial diameter (diagonal). We want the diamond shaped fiducials to be 
    // 2mm squares turned 45°. Other values not supported by the OpenPNP feeder. 
    fiducial_diameter=2*sqrt(2),
    
    // Underhang of the wall sides. The idea is that the walls are narrowest at the top, 
    // clamping the tape sidewas but also driving it down.
    underhang=0.05,
    // Overhang of walls on top of the tape thickness. Should create a certain tension to snap
    // the tape in and down.
    overhang=0.15,
    // Thickness of the overhang.
    overhang_thickness=layer_height*3,
    
    // Cover thickness. Should be sturdy enough but still quite flexible. 
    cover_thickness=layer_height*2,
    // How deep the cover is V-slotted into the side wall.
    cover_slot=0.5,
    // How thick the V-slot is. This is a shallow V-slot, so it needs some room.
    cover_slot_thickness=layer_height*13,
    // Mid point of the V-slot measured from the wall top.
    cover_slot_mid=layer_height*5.5,
    // How much the V-slot closes in over the cover again at the top.
    cover_overhang=0.7,
    // How much vertical down-tension is applied to the overhang.
    cover_tensions=0.1,
    // Vertical play of the cover to the tape surface. 
    cover_vertical_play=0.08,
    
    // Blinds opening vs. half the pocket pitch. 
    blinds_opening_ratio=1.05, 

    // Play of the nozzle tip against blinds etc.
    nozzle_play=0.1,

    // Want to print the flanges around the tray? (setting false is just for debugging)
    flange=true,
    // Flange at the ends of the tapes.
    flange_length=0,
    // Flange at the sides of the arrayed tapes. 
    flange_width=5,
    // Thickness of a border on the flange to reinforce it.
    flange_bar=0,
    // Thickness of some reinforced parts of a flange.
    flange_thickness=layer_height*3,

    // Experimental features (not supported in the OpenPNP feeder)
    
    // Enables a drag handle on top of the cover to open/close the cover. 
    // Contains a centering cone to automatically center the cover to the nozzle tip. Because this 
    // requires you to nail down the diameter of the nozzle tip it was replaced by automatic push
    // edge calibration in the OpenPNP feeder. So the software insted of the hardware does the 
    // centering, allowing you to be more flexible with your nozzle tips. 
    drag_handle=false,
    drag_handle_length=8,
    drag_handle_height=2,
    drag_handle_z=1,// above tape_width surface
    
    // Use drag holes instead of push edges to open/close the cover. Would allow operation from one 
    // side. Not (yet) supported in OpenPNP.
    drag_holes=false,

    // How thick the reinforced push edge should be. 
    nozzle_push_height=1, 
    // How the nozzle tip grows in diameter as you go up on it. Only used for the drag handle.
    nozzle_push_upper_height=4.1, 
    nozzle_push_upper_diameter=5,

    // Version tag to be printed on the feeder. 
    version_tag="v126",
    // If debug is set true, it places the cover on the tray in the opened state.
    // It is automatically disabled when rendering. 
    debug=false 

    ) {
    
    // Calculate the summed width of the arrayed tape lanes up to and including tape t.
    function arrayed_width(arrayed_tape_lanes, arrayed_tapes, wall_width, t) 
        = (t >= 0 ? (arrayed_tape_lanes[t]*(get_tape_width(arrayed_tapes[t])+2*wall_width) 
            + arrayed_width(arrayed_tape_lanes, arrayed_tapes, wall_width, t-1)) : 0);
    
    // Only allow effective debug mode in preview mode.
    debug_eff = debug && $preview;
    
    // Go through all the tapes and construct them.
    for (t = [0:len(arrayed_tape_lanes)-1]) {
        first = (arrayed_width(arrayed_tape_lanes, arrayed_tapes, wall_width, t-1) == 0);
        last  = (arrayed_width(arrayed_tape_lanes, arrayed_tapes, wall_width, t) == 
                 arrayed_width(arrayed_tape_lanes, arrayed_tapes, wall_width, len(arrayed_tape_lanes)-1));
        translate([0, arrayed_width(arrayed_tape_lanes, arrayed_tapes, wall_width, t-1), 0]) {
            // take arrayed values
            tape_lanes      = arrayed_tape_lanes[t];
            tape            = arrayed_tapes[t];
            // get the properties
            tape_width      = get_tape_width(tape);
            tape_thickness  = get_tape_thickness(tape);
            pocket_portrusion = get_pocket_portrusion(tape);
            pocket_width    = get_pocket_width(tape);
            pocket_pitch    = get_pocket_pitch(tape);
            pocket_length   = get_pocket_length(tape);
            blinds          = get_blinds(tape);
            nozzle_diameter = get_nozzle_diameter(tape);
            tape_play       = get_tape_play(tape);
            sprocket_thorns = get_sprocket_thorns(tape);
            cover_play      = get_cover_play(tape);
            
            // calculate some helper values
            pocket_dist=
                (tape_width-1)*0.5+sprocket_dist; // as per EIA-481-C (by deduction)
            pocket_center_dist_orient = (-sprocket_dist + 0.5)*tape_orientation;
            fiducial_dist = pocket_center_dist_orient + (tape_orientation < 0 ? -5 : -2);
            
            tape_surface_height_net=floor_thickness+pocket_portrusion+tape_thickness;
            tape_surface_height=tape_surface_height_net+cover_vertical_play;
            
            tape_std_height=max(floor_thickness+tape_max_thickness, tape_surface_height);
            knee_height=tape_surface_height+overhang_thickness;
            wall_height=tape_std_height+overhang_thickness+cover_slot_thickness;

            // This geometry must match the OpenPNP BlindsFeeder class implementation,
            // see org.openpnp.machine.reference.feeder.BlindsFeeder.recalculateGeometry()
            
            // According to the EIA-481-C, pockets align with the mid-point between two sprocket holes, 
            // however for the 2mm pitch tapes (0402 and smaller) obviously the pockets also directly 
            // align with sprocket holes.
            // This means that for 2mm pitch parts we can accomodate one more pocket in the tape i.e. 
            // the first one aligns with the sprocket instead of half the sprocket pitch away. 
            is_small = (sprocket_pitch/pocket_pitch) == 2;
            tape_avail=tape_length-sprocket_pitch;
            sprocket_count=floor(tape_avail/sprocket_pitch);
            tape_net = sprocket_count*sprocket_pitch;
            pocket_count=floor(tape_net/pocket_pitch)
                + (is_small ? 1 : 0); // one more if 2mm pocket pitch
            sprocket_d = sprocket_pitch*0.5+
                tape_align*(tape_avail - (sprocket_count)*sprocket_pitch);
            // The wanted pocket center position relative to the pocket pitch is 0.25 for blinds covers,
            // but 0.5 for all other cover types where the part can be larger than half the pitch.  
            pocket_position = blinds ? 0.25 : 0.5;
            // Align pocket center to sprocket pitch. Make sure to round 0.5 downwards (hence the -e).
            pocket_align = round(pocket_pitch*pocket_position/sprocket_pitch - e)*sprocket_pitch; 
            // Now shift that to a mid-point between two sprocket holes (unless it is small pitch)
            pocket_d = sprocket_d+sprocket_pitch*(is_small ? 0.0 : 0.5)+pocket_align;
            
            extension_size=blinds ? (drag_holes ? pocket_pitch*1.5 : pocket_pitch*0.5) : 0;
            
            // create the tray cross section 
            tape_eff = tape_width + tape_play*2;
            pocket_left=tape_orientation*(tape_width*0.5-pocket_dist)-pocket_width*0.5;
            pocket_right=tape_orientation*(tape_width*0.5-pocket_dist)+pocket_width*0.5;
            tray_points = [
                [tape_width*0.5+wall_width+wall_overlap+e,0],
                [tape_width*0.5+wall_width+wall_overlap+e,wall_height-cover_slot_mid],
                [tape_eff*0.5+cover_slot-cover_overhang-cover_tensions+min_width,wall_height+layer_height], 
                [tape_eff*0.5+cover_slot-cover_overhang-cover_tensions,wall_height], 
                [tape_eff*0.5+cover_slot,wall_height-cover_slot_mid], 
                [tape_eff*0.5+cover_slot,wall_height-cover_slot_mid-layer_height], 
                [tape_eff*0.5-overhang,knee_height], 
                [tape_eff*0.5,tape_surface_height_net], 
                [tape_eff*0.5+underhang,floor_thickness+pocket_portrusion], 
                [pocket_right,floor_thickness+pocket_portrusion], 
                [pocket_right,floor_thickness], 
                [pocket_left,floor_thickness], 
                [pocket_left,floor_thickness+pocket_portrusion], 
                [-tape_eff*0.5-underhang,floor_thickness+pocket_portrusion], 
                [-tape_eff*0.5,tape_surface_height_net], 
                [-tape_eff*0.5+overhang,knee_height], 
                [-tape_eff*0.5-cover_slot,wall_height-cover_slot_mid-layer_height], 
                [-tape_eff*0.5-cover_slot,wall_height-cover_slot_mid], 
                [-tape_eff*0.5-cover_slot+cover_overhang+cover_tensions,wall_height], 
                [-tape_eff*0.5-cover_slot+cover_overhang+cover_tensions-min_width,wall_height+layer_height], 
                [-tape_width*0.5-wall_width-wall_overlap,wall_height-cover_slot_mid],
                [-tape_width*0.5-wall_width-wall_overlap,0]
            ];
            
            slope=overhang/overhang_thickness;
            simple_cover_edge=
                tape_eff*0.5-cover_play-slope*cover_thickness;
            simple_cover_inner_edge=blinds ? 
                max(max(pocket_right, -pocket_left)+cover_play+e,
                simple_cover_edge-min_width) :
                simple_cover_edge-min_width;
            
            has_sprocket_slot=(sprocket_thorns > 0);
            sprocket_slot_height = has_sprocket_slot ? layer_height*2 : 0;
            sprocket_slot_peak = has_sprocket_slot ? layer_height*3 : 0;
            // the cover cross section
            cover_points= 
            [   [simple_cover_edge-cover_play, 0],
                [simple_cover_edge-cover_play, knee_height-tape_surface_height],
                [tape_eff*0.5+cover_slot-cover_play, wall_height-cover_slot_mid-tape_surface_height],
                [tape_eff*0.5+cover_slot-cover_overhang-cover_play, wall_height-tape_surface_height],
                [tape_eff*0.5+cover_slot-cover_overhang-cover_play-extrusion_width, 
                    wall_height-tape_surface_height-layer_height],
                [tape_eff*0.5+cover_slot-cover_play-min_width, wall_height-cover_slot_mid-tape_surface_height],
                [simple_cover_inner_edge, cover_thickness*2+sprocket_slot_height],
                [simple_cover_inner_edge, cover_thickness],
                // sprocket slot above
                [min(simple_cover_inner_edge, 
                    tape_orientation*(tape_width*0.5-sprocket_dist)+sprocket_diameter0*0.5+min_width),
                    cover_thickness],
                [min(simple_cover_inner_edge, 
                    tape_orientation*(tape_width*0.5-sprocket_dist)+sprocket_diameter1*0.5+min_width*0.5),
                    tape_orientation>0 ? 
                        cover_thickness*2+sprocket_slot_height :
                        cover_thickness+sprocket_slot_peak],
                [tape_orientation*(tape_width*0.5-sprocket_dist)+min_width*0.5,
                    cover_thickness+sprocket_slot_peak],
                [tape_orientation*(tape_width*0.5-sprocket_dist)-min_width*0.5,
                    cover_thickness+sprocket_slot_peak],
                [max(-simple_cover_inner_edge, 
                    tape_orientation*(tape_width*0.5-sprocket_dist)-sprocket_diameter1*0.5-min_width*0.5),
                    tape_orientation<0 ? 
                        cover_thickness*2+sprocket_slot_height :
                        cover_thickness+sprocket_slot_peak],
                [max(-simple_cover_inner_edge, 
                    tape_orientation*(tape_width*0.5-sprocket_dist)-sprocket_diameter0*0.5-min_width),
                    cover_thickness],
                // end sprocket slot
                [-simple_cover_inner_edge, cover_thickness],
                [-simple_cover_inner_edge, cover_thickness*2+sprocket_slot_height],
                [-tape_eff*0.5-cover_slot+cover_play+min_width, wall_height-cover_slot_mid-tape_surface_height],
                [-tape_eff*0.5-cover_slot+cover_overhang+cover_play+extrusion_width, wall_height-tape_surface_height-layer_height],
                [-tape_eff*0.5-cover_slot+cover_overhang+cover_play, wall_height-tape_surface_height],
                [-tape_eff*0.5-cover_slot+cover_play, wall_height-cover_slot_mid-tape_surface_height],
                [-simple_cover_edge+cover_play, knee_height-tape_surface_height],
                [-simple_cover_edge+cover_play, 0],
                // sprocket slot below
                [tape_orientation*(tape_width*0.5-sprocket_dist)-sprocket_diameter0*0.5-cover_play, 0],
                [tape_orientation*(tape_width*0.5-sprocket_dist)-sprocket_diameter0*0.5-cover_play, sprocket_slot_height],
                [tape_orientation*(tape_width*0.5-sprocket_dist), sprocket_slot_peak],
                [tape_orientation*(tape_width*0.5-sprocket_dist)+sprocket_diameter0*0.5+cover_play, sprocket_slot_height],
                [tape_orientation*(tape_width*0.5-sprocket_dist)+sprocket_diameter0*0.5+cover_play, 0],
                
            ];
            

            // Go through all the lanes of this tape type.
            union() if(tape_lanes > 0) for (lane_i = [0:tape_lanes-1]) {
                lane = lane_i+0.5;
                translate([0,lane*(tape_width+wall_width*2),0]) color("Lime", 0.5) union() {
                    
                    ///////////////////  Flange ///////////////////////////////////////////////
                    
                    lane_flange = ((lane_i == 0 && first)
                                 || (lane_i == tape_lanes-1 && last));
                    if (tray && flange) {
                        difference() {
                            union() {
                                if (flange_length > 0) {
                                    thickness= min(tape_thickness+pocket_portrusion, flange_thickness);
                                    // simple flange around the tray 
                                    translate([-flange_length, -0.5*tape_width-wall_width, 0]) {
                                        cube([flange_length*2+tape_length+e, tape_width+wall_width*2+e, floor_thickness]);
                                        translate([0, 0, floor_thickness-e])
                                            cube([flange_bar, tape_width+wall_width*2+e, thickness+e]);
                                        translate([flange_length*2+tape_length-flange_bar, 0, floor_thickness-e])
                                            cube([flange_bar, tape_width+wall_width*2+e, thickness+e]);
                                    }
                                    if (blinds && drag_holes) {
                                        // drag hole support
                                        translate([-pocket_pitch*0.75-extension_size*0.5, 
                                            (pocket_left+pocket_right)*0.5, floor_thickness-e]) {
                                            translate([0, -pocket_width*0.5-extrusion_width*2, 0]) 
                                                cube([pocket_pitch*0.5+extension_size*0.5, 
                                                    extrusion_width*2,tape_thickness+pocket_portrusion+e]);
                                            translate([0, pocket_width*0.5, 0]) 
                                                cube([pocket_pitch*0.5+extension_size*0.5, 
                                                    extrusion_width*2,tape_thickness+pocket_portrusion+e]);
                                        }
                                    }
                                }
                            }
                        }
                       
                                    
                        if (lane_i == 0 && first) {
                            flange_offset=-0.5*tape_width-wall_width-flange_width;
                            fiducial_y=-0.5*tape_width+fiducial_dist;
                            difference() {
                                union()  {
                                    // flange
                                    translate([-flange_length, flange_offset, 0]) {
                                        cube([flange_length*2+tape_length+e, flange_width+e, floor_thickness]);
                                        translate([0, 0, floor_thickness-e])
                                            cube([flange_bar, flange_width+e, flange_thickness+e]);
                                        translate([tape_length+2*flange_length-flange_bar, 0, floor_thickness-e])
                                            cube([flange_bar, flange_width+e, flange_thickness+e]);
                                        translate([flange_bar-e, 0, floor_thickness-e])
                                            cube([tape_length+2*(flange_length-flange_bar+e), 
                                                flange_bar, flange_thickness+e]);
                                        
                                    }
                                    // text
                                    translate([tape_length*0.5, flange_offset+flange_width*0.5, floor_thickness-e]) 
                                        linear_extrude(height=layer_height+2*e) 
                                            rotate([0, 0, tape_orientation < 0 ? 180 : 0]) 
                                                text(text=str(tape_length,"-",
                                                    tape_length-tape_net,"|",
                                                    (1*pocket_center_dist_orient-1*fiducial_dist),"-",
                                                    wall_width,"|",
                                                    cover_thickness+sprocket_slot_peak,"|",version_tag), 
                                                    valign="center", halign="center", 
                                                    font=font_name, 
                                                    size=font_size);
                                }
                                
                                union() {
                                    // fiducial
                                    translate([sprocket_d, 
                                        fiducial_y, -e]) {
                                        cylinder(d=fiducial_diameter,  
                                            h=floor_thickness+flange_thickness+3*e, $fn=4);
                                    }
                                    translate([sprocket_d*3, 
                                        fiducial_y, -e]) {
                                        rotate([0,0,45]) cylinder(d=fiducial_diameter,  
                                            h=floor_thickness+flange_thickness+3*e, $fn=4);
                                    }
                                    translate([sprocket_d+sprocket_pitch*sprocket_count, 
                                        fiducial_y, -e]) {
                                        cylinder(d=fiducial_diameter, 
                                            h=floor_thickness+flange_thickness+3*e, $fn=4);
                                    }
                                }
                                
                            }
                        }
                        if (lane_i == tape_lanes-1 && last) {
                            flange_offset=0.5*tape_width+wall_width;
                            fiducial_y=0.5*tape_width+fiducial_dist+7;
                            difference() {
                                union() {
                                    // flange
                                    translate([-flange_length, 0.5*tape_width+wall_width, 0]) {
                                        cube([flange_length*2+tape_length+e, flange_width, floor_thickness]);
                                        translate([0, 0, floor_thickness-e])
                                            cube([flange_bar, flange_width+e, flange_thickness+e]);
                                        translate([tape_length+2*flange_length-flange_bar, 0, floor_thickness-e])
                                            cube([flange_bar, flange_width+e, flange_thickness+e]);
                                        translate([flange_bar-e, flange_width-flange_bar, floor_thickness-e])
                                            cube([tape_length+2*(flange_length-flange_bar+e), 
                                            flange_bar, flange_thickness+e]);
                                    }
                                }
                                
                                union() {
                                    // fiducial
                                    translate([sprocket_d, 
                                        fiducial_y, -e]) {
                                        cylinder(d=fiducial_diameter,  
                                            h=floor_thickness+flange_thickness+3*e, $fn=4);
                                    }
                                    translate([sprocket_d+sprocket_pitch*sprocket_count, 
                                        fiducial_y, -e]) {
                                        cylinder(d=fiducial_diameter, 
                                            h=floor_thickness+flange_thickness+3*e, $fn=4);
                                    }
                                }
                                
                            }
                        }
         
                    }
                    
                    
                    ///////////////////  Tray ///////////////////////////////////////////////
                    
                    if (tray) { 
                        rotate([90,0,90]) linear_extrude(height=tape_length) {
                            difference() {
                                polygon(tray_points);
                                
                                /* 
                                // in case of deep pockets this code creates struts underneath the tape to force 
                                // a walled support structure as opposed to the slicer's fill mode. 
                                // While currently disabled it may proove useful in the future.
                                union() {
                                    strut=2;
                                    strut_ceiling=2;
                                    if (pocket_portrusion > layer_height*strut_ceiling) {
                                        w_left=tape_width*0.5+pocket_left;
                                        steps_left=floor(w_left/min_width/strut);
                                        if (steps_left>0) {
                                            step_left=w_left/steps_left;
                                            for (c = [-tape_width*0.5:step_left:pocket_left-step_left+e]) {
                                                translate([c, floor_thickness]) 
                                                    square([step_left-min_width, pocket_portrusion-layer_height*strut_ceiling+e]);
                                            }
                                        }
                                        w_right=tape_width*0.5-pocket_right;
                                        steps_right=floor(w_right/extrusion_width/strut);
                                        if (steps_right>0) {
                                            step_right=w_right/steps_right;
                                            for (c = [pocket_right+extrusion_width:
                                                step_right:tape_width*0.5+extrusion_width-step_right+e]) {
                                                translate([c, floor_thickness]) 
                                                    square([step_right-min_width, pocket_portrusion-layer_height*strut_ceiling+e]);
                                            }
                                        }
                                    }
                                }*/
                            }
                        }
                        // pocket outlines
                        if (pocket_portrusion+tape_thickness > 0 && pocket_length > 0) {
                            for (i = [0:pocket_count-1]) { 
                                x=i*pocket_pitch+pocket_d;
                                w = extrusion_width;
                                x0 = max(0, x-pocket_length*0.5-w);
                                x1 = x-pocket_length*0.5;
                                x2 = x+pocket_length*0.5;
                                x3 = min(tape_length, x+pocket_length*0.5+w);
                                translate([x0, 
                                    pocket_left-e, 
                                    floor_thickness-e])
                                    cube([x1 - x0, 
                                        pocket_right-pocket_left+2*e, 
                                        pocket_portrusion+tape_thickness+e]);
                                translate([x0, 
                                    pocket_left-w, 
                                    floor_thickness-e])
                                    cube([x3 - x0, 
                                        w, 
                                        pocket_portrusion+tape_thickness+e]);
                                translate([x0, 
                                    pocket_right, 
                                    floor_thickness-e])
                                    cube([x3 - x0, 
                                        w, 
                                        pocket_portrusion+tape_thickness+e]);
                                translate([x2, 
                                    pocket_left-e, 
                                    floor_thickness-e])
                                    cube([x3 - x2, 
                                        pocket_right-pocket_left+2*e, 
                                        pocket_portrusion+tape_thickness+e]);
                            }
                        }
                        
                        if (sprocket_thorns > 0) {
                            // sprocket thorns
                            sprocket_height=min(sprocket_max_height, max(layer_height, tape_thickness));
                            sprocket_slant=layer_height*(sprocket_diameter0-sprocket_diameter1)/sprocket_height;
                            for (i = [0:sprocket_thorns:sprocket_count]) { 
                                x=i*sprocket_pitch+sprocket_d;
                                translate([x, tape_orientation*(tape_width*0.5-sprocket_dist), 
                                    floor_thickness+pocket_portrusion-e])
                                    cylinder(
                                        d1=sprocket_diameter0+sprocket_slant, 
                                        d2=sprocket_diameter1-sprocket_slant, 
                                        h=sprocket_height+e, $fn=8);
                            }
                        }
                    }

                    ///////////////////  Cover ///////////////////////////////////////////////

                    if (cover) {
                        translate([(debug_eff ? 0 : 1) * (covers_to_the_right ? tape_length+flange_length+gap : 0), 
                                   (debug_eff ? 0 : 1) * (covers_to_the_right ? 0 : arrayed_width(
                                arrayed_tape_lanes, arrayed_tapes, wall_width, 
                                len(arrayed_tape_lanes)-1)+flange_width+gap), 
                            (debug_eff ? 1 : 0) * tape_surface_height]) union() {
                            difference()  {
                                union() {
                                    // cover profile
                                    translate([0, 0, 0]) rotate([90,0,90]) 
                                        linear_extrude(height=tape_length+extension_size, convexity=10)
                                            polygon(cover_points);
                                    // push edges
                                    push_edge=min(pocket_width*0.5, simple_cover_inner_edge-min_width*2);
                                    if (! drag_holes) {
                                        push_y=(pocket_right+pocket_left)*0.5-push_edge;
                                        if (blinds) {
                                            // simple push edge 
                                            translate([0, push_y, cover_thickness-e]) 
                                                cube([min_width, 2*push_edge, 1-cover_thickness+e]);
                                            translate([tape_length+extension_size-min_width, 
                                                push_y, cover_thickness-e]) 
                                                cube([min_width, 2*push_edge, 1-cover_thickness+e]);
                                        }
                                        else {
                                            // tall nozzle push
                                            nozzle_push_offset=0; //Do not: nozzle_push_diameter*0.5-pocket_pitch*0.5-nozzle_play*2;
                                            union(){
                                                translate([nozzle_push_offset, 
                                                    push_y, cover_thickness-e]) 
                                                    cube([min_width, 2*push_edge, 
                                                        nozzle_push_height-cover_thickness+e]);
                                                if (nozzle_push_height > 1) {
                                                    translate([nozzle_push_offset+min_width-e, 
                                                        push_y+push_edge-min_width*0.5, 
                                                        cover_thickness-e]) 
                                                        cube([tape_length+extension_size-nozzle_push_offset*2-min_width*2, 
                                                            min_width, 
                                                            nozzle_push_height-cover_thickness-layer_height+e]);
                                                }
                                                translate([tape_length+extension_size-min_width-nozzle_push_offset,
                                                    push_y, cover_thickness-e]) 
                                                    cube([min_width, 2*push_edge, 
                                                        nozzle_push_height-cover_thickness+e]);
                                            }
                                        }
                                        // drag handle
                                        if (drag_handle && blinds) {
                                            nozzle_cone=(nozzle_push_upper_diameter-nozzle_diameter)
                                                /(nozzle_push_upper_height-nozzle_play*2);
                                            drag_handle_upper_diameter=nozzle_diameter
                                                +nozzle_cone*2*(drag_handle_height+cover_thickness-drag_handle_z);
                                            translate ([sprocket_d+1*sprocket_pitch, 
                                                tape_orientation*(tape_width*0.5-sprocket_dist),
                                                cover_thickness+sprocket_slot_peak-e]) 
                                                difference() { 
                                                    intersection() {
                                                        translate([-drag_handle_length*0.5, -min_width, 0]) 
                                                            cube([drag_handle_length*3, min_width*2, drag_handle_height+e]);
                                                        cylinder(d1=drag_handle_length, d2=drag_handle_upper_diameter
                                                            +2*extrusion_width, 
                                                            h=drag_handle_height+2*e);
                                                    }
                                                    translate([0, 0, drag_handle_z-cover_thickness]) 
                                                        cylinder(d1=nozzle_diameter-nozzle_cone*layer_height, 
                                                            d2=drag_handle_upper_diameter, 
                                                            h=cover_thickness+drag_handle_height-drag_handle_z+2*e);
                                                
                                            }
                                        }
                                    }
                                }
                                if (blinds) translate([0,0,0]) union() {
                                    // pocket blinds
                                    for (i = [0:pocket_count-1]) { 
                                        x=i*pocket_pitch+pocket_d;
                                        translate([x-pocket_pitch*0.25*(blinds_opening_ratio)-nozzle_play, 
                                            (pocket_left+pocket_right)*0.5-pocket_width*0.5-nozzle_play, -e])
                                            cube([pocket_pitch*0.5*blinds_opening_ratio+2*nozzle_play, pocket_width+2*nozzle_play, 
                                                cover_thickness*2+e+e]);    
                                    }
                                    if (drag_holes) {
                                        // drag holes
                                        translate([pocket_d-pocket_pitch*1.5, 
                                            (pocket_left+pocket_right)*0.5-nozzle_play, -e])
                                            minkowski() {
                                                cube([pocket_pitch*0.5, 2*nozzle_play, 2*e]);
                                                cylinder(d1=pocket_pitch*0.5, d2=pocket_pitch*0.5+nozzle_play,
                                                    h=floor_thickness+pocket_portrusion, $fn=16);
                                            }
                                        translate([tape_length-pocket_d+pocket_pitch*1, 
                                                (pocket_left+pocket_right)*0.5-nozzle_play, -e])
                                            minkowski() {
                                                cube([pocket_pitch*0.5, 2*nozzle_play, 2*e]);
                                                cylinder(d1=pocket_pitch*0.5, d2=pocket_pitch*0.5+nozzle_play,
                                                    h=floor_thickness+pocket_portrusion, $fn=16);
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
