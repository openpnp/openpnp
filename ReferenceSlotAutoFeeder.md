ReferenceSlotAutoFeeder allows you to define a number of slots on the machine that feeders can be inserted into. You can move feeders from slot to slot without having to reconfigure the part or location of the feeder. This type of system is most commonly used with the popular Yamaha CL style feeders, but it can be used for any auto feeder that can be moved around the machine.

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

# Setup and Usage

1. Add a ReferenceSlotAutoFeeder by using the New Feeder button in the Feeders panel.
2. The feeder will have a default bank assigned. If you have only one bank there's no need to change it.
3. If this is your first slot, add a new feeder by clicking the New button next to the feeder.