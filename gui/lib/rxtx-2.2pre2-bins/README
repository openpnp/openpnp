Wed Feb  4 20:13:07 EST 2009 Rxtx-2.2pre2

Build Solaris with ../configure cc='gcc -static-libgcc'

More 64 bit fixes.

Maintain DTR=false during setSerialPortParams

Tried adjusting the header files for win32/64.  The pin events are getting lost.  Still need to check if
this fixed it.  Cross compiling works but I'm fixing the native w32 MSVC builds.

Tue Dec 30 22:02:44 EST 2008 Rxtx-2.2pre1

For information on how to use these files, please see the installation page on
http://rxtx.qbang.org

This is the rxtx-2.2pre1.  The purpose is to expose any problems with the
builds on as many platforms as we can.

To reproduce these files, download the source.  For all platforms except
windows, the following was used to build:

cd rxtx-2.2pre1
mkdir build
cd build
../configure && build

For Windows, the following was done in a Visual Studio command prompt:

cd rxtx-2.2pre1
mdkir build
copy Makefile.msvc build\Makefile
nmake


Feel free to add additional information if you run into or solve a problem.


The files:

mac-10.4/librxtxSerial.jnilib:               Mach-O universal binary with 2 architectures
mac-10.4/librxtxSerial.jnilib (for architecture i386):  Mach-O bundle i386
mac-10.4/librxtxSerial.jnilib (for architecture ppc):   Mach-O bundle ppc

mac-10.5/librxtxSerial.jnilib:               Mach-O universal binary with 3 architectures
mac-10.5/librxtxSerial.jnilib (for architecture i386):  Mach-O bundle i386
mac-10.5/librxtxSerial.jnilib (for architecture x86_64):        Mach-O 64-bit bundle x86_64
mac-10.5/librxtxSerial.jnilib (for architecture ppc7400):       Mach-O bundle ppc

win32/rxtxParallel.dll:                      MS-DOS executable PE  for MS Windows (DLL) (GUI) Intel 80386 32-bit
win32/rxtxSerial.dll:                        MS-DOS executable PE  for MS Windows (DLL) (GUI) Intel 80386 32-bit

win64/rxtxParallel.dll:                      MS-DOS executable PE  for MS Windows (DLL) (GUI)
win64/rxtxSerial.dll:                        MS-DOS executable PE  for MS Windows (DLL) (GUI)

i686-pc-linux-gnu/librxtxParallel.so:        ELF 32-bit LSB shared object, Intel 80386, version 1 (SYSV), not stripped
i686-pc-linux-gnu/librxtxSerial.so:          ELF 32-bit LSB shared object, Intel 80386, version 1 (SYSV), not stripped

x86_64-unknown-linux-gnu/librxtxParallel.so: ELF 64-bit LSB shared object, AMD x86-64, version 1 (SYSV), not stripped
x86_64-unknown-linux-gnu/librxtxSerial.so:   ELF 64-bit LSB shared object, AMD x86-64, version 1 (SYSV), not stripped

sparc-sun-solaris2.10/librxtxSerial.so: ELF 64-bit MSB dynamic lib SPARCV9 Version 1, dynamically linked, not stripped

sparc-sun-solaris2.6/librxtxSerial.so:  ELF 32-bit MSB shared object, SPARC, version 1 (SYSV), not stripped

