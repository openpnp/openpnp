Some notes to help get started with a Neoden4 machine and Openpnp

### Getting ready
1) Get a windows 7 32-bit ISO 
e.g. https://tech-latest.com/download-windows-7-iso/

2) Make a bootable USB drive (>16GB) with WinToUSB (free version is fine)
https://www.easyuefi.com/wintousb/

3) Plug in USB drive to Neoden4, select boot order for USB drive in BIOS

4) Windows boots up. Suggest using a USB-Ethernet adapter and enabling remote desktop access if useful.

5) Install the camera drivers (top and bottom camera) in device manager. There are two camera hardware versions (new and old), the new version VID:PID is 0x52CB:0x52CB.

6) Install java (e.g. JRE 8 32-bit http://dl.msystems.ch/Java/jre-8u301-windows-i586.exe )

### Building Openpnp application

In my experience was easier to build the openpnp (jar) on a second machine. 

Get Eclipse (I used 32-bit, JDK 8 32-bit), pull the neoden4camera_jna branch from
https://github.com/MatSpy/openpnp/commits/neoden4camera_jna . 

Some other notes in this one on importing the maven project
https://firepickdelta.dozuki.com/Guide/Installing+Eclipse+IDE+and+Running+OpenPnP/35

To build the jar in eclipse, choose Run Configurations->Maven Build->Open PNP, type 'package' in the goals field, check 'skip tests' if needed, hit run to build it.

Copy the 'target' folder onto the neoden machine. Also need to copy NeodenCamera.dll into the folder with openpnp.bat.

### Configuring Openpnp
- Starting .xml files
- Camera setup
- Axis setup