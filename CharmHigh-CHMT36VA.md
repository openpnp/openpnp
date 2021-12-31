# Project Status
___
**This Wiki page is outdated.** There are newer/better versions of Smoothieware firmware, Smoothieware configurations, and OpenPnP configuration around. This Wiki page should be updated by someone who owns such a machine and is on top of things. 

**HELP WANTED!**

Some pointers (unconfirmed):
- Discussion and OpenPnP Config:
  https://groups.google.com/g/desktop-pick-and-place/c/bkAQFhvyEE4/m/yJ9yCGLtAAAJ
- Smoothieware firmware and config:
  https://github.com/c-riegel/Smoothieware-CHMT/
  with these important patches:
  https://github.com/c-riegel/Smoothieware-CHMT/commits/chmt

_Mark (2021-12-04)
___


The stock firmware on the CHMT36VA turned out to be a dead end. There does not appear to be any command that can be used to rotate the nozzle. The best way forward seems to be to use [Matt Baker's Smoothie port](https://github.com/mattthebaker/Smoothieware-CHMT) which can be flashed on the stock main board.

Using the Smoothie firmware is an irreversible operation. Once you flash it, there does not appear to be a way to go back to the stock firmware, so consider carefully if you want to do that. If you'd prefer to buy a second controller board to use for OpenPnP instead of using your stock one, contact Kimi. I was quoted $280 including shipping to the US.

Using Smoothie, OpenPnP can use all functions of the CHMT36VA. It does not reach the same level of performance as the original software, but there are many new features and functions in OpenPnP that are not available in the OEM software.

A common question is whether or not this port is ready for daily production use. The answer is yes, but it will take much longer to initially configure the system and there is a steep learning curve. Be prepared to have the machine down for a week or more while you get everything configured and become familiar with OpenPnP.

# Setup Guide

The following can be used as a rough guide to getting OpenPnP working on your CHMT36VA. Reminder: **Installing Smoothie on the main board is a irreversible operation. Once you install Smoothie you will not be able to flash the stock firmware, and you will not be able to use the OEM software.**

If you want to proceed, follow these steps:

1. Install [Matt Baker's Smoothie port](https://github.com/mattthebaker/Smoothieware-CHMT) on your CHMT36VA main board. This requires some familiarity with the STM32 tools, and requires a flash tool. See [[Charmhigh Modifications for OpenPnP]] and the [Smoothie on STM32/CHMT Thread](https://groups.google.com/d/msg/desktop-pick-and-place/C-n9dksqhDQ/xZdmKPh3CAAJ) for more information on this.
2. Either Install [Matt Baker's example OpenPnP configuration](https://github.com/mattthebaker/openpnp-config-chmt) to get started or configure from scratch using the [[Setup and Calibration]] Guide. It is recommended to step through the guide even if you use the example config and check that each setting applies to your machine and works correctly.
3. If you are using the stock cameras and camera board, see the [SwitcherCamera Documentation](https://github.com/openpnp/openpnp/wiki/SwitcherCamera) for how to set up the multiplexer.
4. Please consider joining the [OpenPnP Discussion Group](http://groups.google.com/group/openpnp) and the [SparkFun Desktop Pick and Place Discussion Group](https://groups.google.com/forum/#!forum/desktop-pick-and-place) to post questions and information about your setup.

This documentation is incomplete and can use your help! Anyone with a GitHub account can edit this page and improve it. Please consider adding your experiences and findings here.

# Helpful Resources

* [The GoFundMe Page](https://www.gofundme.com/help-openpnp-grow)
* [SparkFun's CHMT36VA Repo](https://github.com/sparkfunX/Desktop-PickAndPlace-CHMT36VA) contains translations, documentation, software, and utilities for working with the machine.
* [Matt Baker's Smoothie port](https://github.com/mattthebaker/Smoothieware-CHMT) which can be used to flash Smoothie to your CHMT36VA for use with OpenPnP.
* [Matt Baker's example OpenPnP configuration](https://github.com/mattthebaker/openpnp-config-chmt).
* [OpenPnP + CHMT Thread](https://groups.google.com/forum/#!msg/desktop-pick-and-place/qaoGrnM7pPw/-2k-5FBHCAAJ) contains tons of good information on making the CHMT work with OpenPnP.
* [Smoothie on STM32/CHMT Thread](https://groups.google.com/d/msg/desktop-pick-and-place/C-n9dksqhDQ/xZdmKPh3CAAJ)
* [SwitcherCamera Documentation](https://github.com/openpnp/openpnp/wiki/SwitcherCamera): The SwitcherCamera lets OpenPnP switch the camera board on the CHMT between the top and bottom camera.
* [CharmHigh Conversion](https://github.com/openpnp/openpnp/wiki/Charmhigh-modifications-for-OpenPnP): how to convert the CharmHigh machine to use Smoothieware and OpenPnP.

---