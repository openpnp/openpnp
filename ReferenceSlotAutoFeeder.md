ReferenceSlotAutoFeeder allows you to define any number of slots on the machine which feeders can be inserted into. You can move feeders from slot to slot without having to reconfigure the part or location of the feeder. This type of system is most commonly used with the popular Yamaha CL style feeders, but it can be used for any auto feeder that can be moved around the machine.

# Core Concepts

The slot feeder is more complex than most feeders, so it's important to understand how the different pieces work before you set one up.

## Banks

Banks are used to group feeders on the machine. Every feeder belongs to one bank. Any feeder in a bank can be moved to any slot in that bank. Most machines will just have one bank, the default, but if you have multiple types of incompatible feeders you can create additional banks.

## Feeders

The term feeder is used a little differently for the slot feeder. While the slot itself is considered a feeder, there are child feeders that can be placed in the slot. Note that due to this confusing terminology, this will likely be changed in the future.

Each feeder in this case has an assigned part and a set of offsets. The offsets are added to the slot location when picking. This allows you to indicate to OpenPnP that, for instance, the parts are rotated 90 degrees in the feeder. Most feeders will use all zeros for the offsets.

## Slots

ReferenceSlotAutoFeeder represents one slot on the machine. A slot is a place where you can install a feeder and easily remove and replace it. The slot has a pick location which you can define and in general it should never change. When you install a new feeder in a slot that pick location plus the feeder's offsets become the new pick location. In this way you can move feeders from slot to slot without having to reconfigure any locations.

Each slot belongs to a bank and any feeder in that bank can be installed in the slot.

## Actuators

The ReferenceSlotAutoFeeder is an extension of the ReferenceAutoFeeder, and has the same actuator support. You can define an actuator that is triggered to feed the feeder and another to trigger after a part has been picked. It's important to note that the actuator definitions stay with the slot, not the feeder. It is expected that the slot is the hardware interface and it is responsible for controlling whatever feeder is installed.

# Setup

1. Add a ReferenceSlotAutoFeeder by using the New Feeder button in the Feeders panel. The feeder will have a default bank assigned. If you have only one bank there's no need to change it.
2. Set the ReferenceSlotAutoFeeder's name by double clicking it in the table. Use whatever name you use to identify the slot on the machine.
3. Physically mount the feeder you wish to use in this slot.
4. If this is your first slot, add a new feeder by clicking the New button next to the feeder line in the configuration panel. A new feeder will be created and you can change it's name by typing in the text field.
5. Choose the part that is installed in the feeder. Typically you will keep this value set until you replace the reel in the feeder.
6. Set the nominal pick location of the slot by centering the camera over the exposed part in the feeder and click the camera capture button. Once set, the pick location for a fixed slot should never have to change.
7. Optional: If the part in the installed feeder is not presented in the normal orientation, adjust the camera and then set the offsets by clicking the capture camera button. The offsets will be set to the pick location minus the current location.
8. Define the actuators that are used to feed the feeder and set any values needed to trigger them.
9. Click the Feed and Pick button in the toolbar and test it out!

# Usage

Once a slot is configured you should not have to change it's pick location, bank or actuators. In daily use, all you will need to do is change the feeder that is installed in the slot.

## Moving a Feeder to a New Slot

1. Select the slot in the Feeders tab.
2. In the configuration panel below, click the Feeder dropdown and select the new feeder to install in the slot. If the feeder was previously installed in another slot it will be removed from that slot and installed in the selected one.
3. Press Apply to save your changes.

## Changing a Part or Reel

To change the part installed in a particular feeder:

1. Select the slot in the Feeders tab that holds the feeder you want to change.
2. In the configuration panel below, click the Part dropdown and select the new part for this feeder.
3. Press Apply to save your changes.
