Some notes to help get started with a Neoden4 machine and Openpnp

### Getting ready
1) The onboard Atom PC running Openpnp directly or streaming with KernelPro causes occasional pauses, get a secondary Windows PC for this setup.

2) Open the bottom of the ND4 case, attach the two USB cables (cameras) to the secondary Windows PC

3) Get a USB serial (RS232) adapter <link>, attach to the ND4 machine uart to the secondary Windows PC

4) Install the camera drivers (top and bottom camera) in device manager. There are two camera hardware versions (new and old), the new version VID:PID is 0x52CB:0x52CB. Drivers available here https://github.com/charlie-x/neoden4/tree/master/windows7/camera

5) Install java 

6) Install OpenPNP either pre-built or from source

### 6a) Downloading pre-built package

Support has been added to the test branch currently, there are pre-built binaries to download here:

https://openpnp.org/test-downloads/

### 6b Building Openpnp application from source

Get Eclipse, pull the test branch from
https://github.com/openpnp/openpnp/tree/test

Some other notes in this one on importing the maven project
https://firepickdelta.dozuki.com/Guide/Installing+Eclipse+IDE+and+Running+OpenPnP/35

To build the jar in eclipse, choose Run Configurations->Maven Build->Open PNP, type 'package' in the goals field, check 'skip tests' if needed, hit run to build it.

Copy the 'target' folder onto the neoden machine. Also need to copy NeodenCamera.dll into the folder with openpnp.bat.

### Configuring Openpnp
- Starting machine .xml files
- Camera setup
- Axis setup
- Feeder setup
- Head setup

### Running a first job

### Replacing the motor drivers

### Adjusting the up looking camera