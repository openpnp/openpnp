var util = require('util');

function tape(
    x,
    y,
    tapeWidth, 
    tapePitch, 
    tapeDriveHoleDistanceFromEdge, 
    tapeDriveHoleDiameter,
    partDistanceFromDriveHole,
    partWidth,
    partHeight,
    partCount) {
    console.log(util.format('GRID mm 1;'));

    // draw the tape
    console.log(util.format('LAYER 113;'));
    console.log(util.format('RECT (%d %d) (%d %d);',
        x + 0,
        y + 0,
        x + tapeWidth, 
        y + tapePitch * (partCount + 1)
        ));

    // draw the holes
    console.log(util.format('LAYER 114;'));
    console.log(util.format('CHANGE WIDTH 0;'));
    for (var i = 0; i < partCount; i++) {
        console.log(util.format('CIRCLE (%d %d) (%d %d);',
            x + tapeDriveHoleDistanceFromEdge, 
            y + tapePitch * i + (tapePitch / 2),
            x + tapeDriveHoleDistanceFromEdge + (tapeDriveHoleDiameter / 2), 
            y + tapePitch * i + (tapePitch / 2)
            ));
    }

    // draw the parts
    for (var i = 0; i < partCount; i++) {
        console.log(util.format('RECT (%d %d) (%d %d);',
            x + tapeDriveHoleDistanceFromEdge + partDistanceFromDriveHole - (partWidth / 2),
            y + tapePitch * i + tapePitch - (partHeight / 2),
            x + tapeDriveHoleDistanceFromEdge + partDistanceFromDriveHole + (partWidth / 2),
            y + tapePitch * i + tapePitch + (partHeight / 2)
            ));
    }
}

tape(
    0,
    0,
    8,
    4,
    1.75,
    1.5,
    3.5,
    0.080 * 25.4,
    0.050 * 25.4,
    10
    );
    
tape(
    10,
    0,
    8,
    4,
    1.75,
    1.5,
    3.5,
    0.060 * 25.4,
    0.030 * 25.4,
    10
    );
    
tape(
    20,
    0,
    8,
    4,
    1.75,
    1.5,
    3.5,
    0.040 * 25.4,
    0.020 * 25.4,
    10
    );
    
tape(
    30,
    0,
    8,
    4,
    1.75,
    1.5,
    3.5,
    0.020 * 25.4,
    0.010 * 25.4,
    10
    );
    
    