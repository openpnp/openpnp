See also:
* [Troubleshooting Placement Accuracy](https://github.com/openpnp/openpnp/wiki/Troubleshooting-Placement-Accuracy)

## Backlash
See [[Backlash-Compensation]] for configuring OpenPnP to compensate for backlash in a machines axes, however you should reduce as much backlash as possible at the machine level before compensating for it in software. Software backlash compensation should only be used to address backlash that cannot be removed in a properly built and adjusted machine due to the machines inherent design.

### Belts
Make sure they're tightened appropriately (twang!).

### Lead screws
Use ballscrews. If you use another type, use anti-backlash nuts if possible.

### Other causes of apparent backlash errors
This might apply to small errors like 0.2 – 0.3mm when jogging in opposite directions but not when jogging in the same direction. When you focus your downward looking camera, make sure you tighten the lens lock screw so the lens doesn't move and cause a false backlash error (not to be confused with actual backlash).
