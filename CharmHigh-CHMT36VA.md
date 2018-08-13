This page will serve as a repository of information on the CharmHigh CHMT36VA and the process of developing an OpenPnP driver for it.

For more information about how we got here, please see: https://www.gofundme.com/help-openpnp-grow

For more information about the CHMT36VA in general, and SparkFun's experiences with it, see: https://github.com/sparkfunX/Desktop-PickAndPlace-CHMT36VA

# Project Status and Updates

## 2018-08-12

First week with the machine. Spent a couple days getting comfortable with the machine and the OEM software.

* Fixed a small problem (with the help of factory rep) where Z was not homing correctly. Some protective film was left on the homing disk.
* Captured serial traces of several different sessions and started building a Java based decoder to.
* Determined that write encryption differs from read encryption - still working on figuring out read.
* Postulated that the machine works mostly autonomous after setup. It looks like the software sends the job to the machine and the machine executes it. This may complicate an OpenPnP port.

# Machine Overview

The CHMT36VA is a desktop pick and place machine. It has an internal controller but requires an external PC to run.

For feeders, the machine has 29 lanes of drag feeders: 22 8mm lanes, 4 12mm lanes, 2 16mm lanes, and 1 24mm lane, and a clutch driven cover tape peeler. There are also 14 single chip trays, and the software supports trays placed randomly around the machine.

There are two nozzles in the shared Z see-saw configuration that is now common on almost all of these types of machines. The nozzles each have a NEMA8 hollow shaft stepper and a permanently mounted nozzle tip holder. The nozzle tip holder takes standard Juki style nozzles, and they are retained by 4 ball bearings and a rubber band.

The vision system has an up looking and a down looking camera, both analog, at 640x480. As far as I can tell, the down looking camera is only used for manual positioning, it does not appear to be used for any type of vision operations. The up looking camera is used for the standard "bottom vision" operation.

The OEM software is Windows based, written with Qt, and is designed for a small touch screen interface. There are other CharmHigh desktop pick and places that use an on board tablet interface, and I suspect the same software runs on the tablet.

For vacuum and air, the machine has an internal vacuum pump and a small internal air compressor. The vacuum pump is used to pick components up, and the air compressor is used to evacuate the vacuum lines when the machine places a part.

## Axes

The X and Y axes are belt driven, with linear bearings on smooth rod. Power comes from NEMA23 steppers with optical encoders and the motion is closed loop. The steppers are powered at 36v and are quite fast. A drive shaft spreads the load of the Y axis to dual belts on either side, while the X axis has a single belt and is directly driven.

Homing is via two mechanical homing switches with rollers.

The Z axis uses a see-saw configuration with spring return. The motor is a NEMA11 (or 14?) and has an optical homing disk with a small slot in it. The disk interrupts the optical beam and the slot allows it through. The machine checks this whenever Z returns to center to ensure it's safe to move.

The C axes (nozzle rotation) are via NEMA 8 hollow shaft steppers with vacuum tubing connected directly to the back shaft. 

## Head

The head includes the two nozzles, a drag pin solenoid, vacuum sensors, vacuum solenoids, and a couple PCBs. The drag solenoid also has an optical interruptor style gate to ensure it's in the up position before moving. It appears to use a spring return made of a piece of rubber tubing, but I haven't been able to inspect it closely to be sure.

## Lighting

The gantry has a white LED strip across it's entire length that the software refers to as the "Work Light". This does a pretty good job of lighting the work area without creating intense reflections.

The up facing camera has a white LED ring light with 3mm LEDs bent at an angle.

## Cameras

There are two cameras, on up facing and one down facing. They are analog cameras and both feed into an off the shelf single capture card with a strange switching PCB soldered to the top of it. The result is that only one camera can send a signal to the host at a time. In general, the software keeps the up looking camera activated and only activates the down looking during user targeting.

The up looking camera appears to be a Sony Effio SN700, which is a cheap and common security camera. I have not yet determined what the down looking one is.

The capture card seems to be a clone of an EZCAP and is identified as "USB2.0 PC CAMERA" with VID 18EC, PID 5850.

# Communications

The machine has an RS-232 9 pin connector, and a USB B connector on the back. The RS-232 is used by the host to send and receive commands and status, and the USB B connector routes directly to the capture card.

## Protocol

The protocol is still being figured out, but here is what we know so far:

* Packet based with a header of 0xebfbebfb, footer of 0xcbdbcbdb.
* Encrypted using keys from the included LICENSE.smt file.
  * The encryption is basic, but not fully figured out yet.
  * In LICENSE.smt, there is a 12 byte key at 1024, and a 1024 byte key at 4096.
  * The 12 byte key is "decrypted" by XOR 0xAA.
  * The 1024 byte key is encrypted by XORing with the decrypted 12 byte key with the index mod 12.
* Commands are somewhat generic, and can be found in the software's smt.db sqlite3 database. See https://docs.google.com/spreadsheets/d/1mVowA6ZmbxwnvEy32Ap_YYBsWuUACl5wKZB5wjBqljY
* The machine might send the entire job information to the machine which the machine autonomously executes. Not sure about this yet, but evidence leads to this conclusion.
* So far I am able to decrypt commands from the PC to the machine, but not from the machine to the PC. They have a similar format, but there is some variation in the encryption.

An example session looks like this:

```
Failed to parse param: 30,,ACK
W length 21, tableID 50, paramID 42, name CmdOpenCameraDown, packet ebfbebfb110400e50032002a0001012c00cbdbcbdb
R length 21, tableID 32, paramID 1, name UNKNOWN, packet ebfbebfb09040095382000010001000200cbdbcbdb
W length 21, tableID 51, paramID 1, name CmdMachineCheck, packet ebfbebfb110400bf003300010001000200cbdbcbdb
R length 29, tableID 1, paramID 67, name InhaleY_19, packet ebfbebfb000c00d8e7010043484d542056322e36362e61fd02cbdbcbdb
W length 21, tableID 51, paramID 2, name CmdReqSysInfo, packet ebfbebfb1104006e003300020001000300cbdbcbdb
W length 21, tableID 2, paramID 0, name UNKNOWN, packet ebfbebfb02040062000200000004000400cbdbcbdb
Waiting for CONFIRM
R ERROR: ebfbebfb00ec0b307b96613a13fa83896a96ed414878f02d95e5512203c6d5a008f6d876430d800d8deb543b24b4d1d30bc7bf5f7f72f4079297616913d0839e3ba3dd707d72c11fa3d3307623fce7f53ac3e8964d04900293f5547b2294d2830b87b97f7c22f40cabf3616913d0839e3ba1dd707d40c11fa3d3304edf0918edc43c175ab0fb6f7d6d0aab40de6b2d03f578464080dd0b746c689e53ef2f7ce4c55e224f81bf3e975d2ccf4edf091817c43c1759b0fb6f8f6d0aab44de6b2d00f578463f80dd0b8f6c689e52ef2f7cf2c55e224b81bf3e995d2ccf4bdf0918e8c43c1759b0fb6f7b6d0aab40de6b2d05f578464080dd0b8e6c689e56ef2f7ce6c55e224e81bf3e6f5d2ccf4bdf0918e5c43c175db0fb6f7e6d0aab44de6b2df5f578463d80dd0b8e6c689e56ef2f7c25c65e223381bf3edb5e2ccf4adf09185cc73c1759b0fb6fb96e0aabb4df6b2d72f77846ad81dd0bf46e689ed2ee2f7cc7c05e22fa80bf3eae582ccffede091820c13c17e8b1fb6fb3680aabf1df6b2d32f07846f581dd0bbc69689ec1ee2f7c2fc05e22a37e40c177a2d330a420f6e7f03bc3e8b14f04906992f554ab2194d2f20a87b9a97f22f46f939761a410d083fa3aa1dda37e40c17aa2d330a720f6e7fa3bc3e8af4f04905e92f554b62194d2eb0a87b9ac7f22f467939761ba10d083f03aa1ddbd7e40c174a2d330a420f6e7cf3bc3e8b14f04905692f554a82194d2e60a87b9af7f22f455939761b910d083c03aa1ddba7e40c17ea2d330be20f6e7cb3bc3e8b24f04905592f554a82194d2d60a87b9af7f22f445909761ba10d083a339a1dd707d40c11fa3d330a420f6e7d438c3e8164e04900990f554142094d2a60887b90b7e22f4989697611d11d0830b3fa1dd047f40c18aa7d3300021f6e7043ec3e8164e04909797f5540f2094d2160f87b90b7e22f4989697616913d0839e3ba1dd707d40c11fa3d3307423f6e7913ac3e8624c04900293f5547b2294d2830b87b97f7c22f40d9297616913d0839e3ba1dd707d40c11fa3d3307423f6e7913ac3e82e0904907cdcf5547b2294d2830b87b97f7c22f40d9297616913d0839e3ba1dd707d40c11fa3d3307423f6e7913ac3e8624c04900293f5547b2294d2830b87b97f7c22f40d9297616913d0839e3ba1dd707d40c11fa3d3307423f6e7913ac3e8624c04900293f5547b2294d2830b87b97f7c22f40d9297616913d0839e3ba1dd707d40c11fa3d3307423f6e7913ac3e8624c04900293f5547b2294d2830b87b9ce3d22f4cd999761091ed08354c25e22036b40c1d65a2ccf7423f6e7913ac3e8a35d0490729bf554760e94d2ca2b87b94d7c22f40d9297616916d0839e3ea1ddc47d40c1d7a3d330f823f6e7313ac3e8624c04900293f554372394d2e40987b9cff8c5f5a9f87060c21dd0836736a1dd707d40c11fa3d33040bdf6e74d8ec3e8624c94916d07f554663794d2830b87b97f7c22f40d920760271ad083733ca1dd707d40c11fa3d330742366e6df33c3e8644104900293f5547b2294d2830b17b8317522f4118097616913d0839e3ba1dd707dd0c051aad3304234f6e7913ac3e8624c049002936555352b94d2ca1787b97f7c22f40d92976169134082d032a1dd235c40c11fa3d3307423f6e7913a53e92c45049064b5f5547b2294d2830b87b97f7cb2f5439b97611638d0839e3ba1dd707d40c11fa343313a2af6e7080ac3e8624c04900293f5547b2204d3cd0287b9d34922f40d9297616913d0839e3b31dc3e7440c1d399d3307423f6e7913ac3e8624c94914c9af554a21d94d2830b87b97f7c22f40d920760271ad0836b7fa1dd707d40c11fa3d330742366e6df33c3e8690604900293f5547b2294d2830b17b8317522f402dd97616913d0839e3ba1dd707dd0c051aad3305677f6e7913ac3e8624c049002936555352b94d2c15287b97f7c22f40d92976169134082d032a1dd2e2340c11fa3d3307423f6e7913a53e92c45049069f0f5547b2294d2830b87b97f7cb2f5439b9761177bd0839e3ba1dd707d40c11fa343313a2af6e70957c3e8624c04900293f5547b22b4d1cd0287b9dd0e22f40d9297616913d0839e3b81de3e7440c14ddad3307423f6e7913ac3e8624c24934c9af5548b5d94d2830b87b97f7c22f40d92b762271ad0830abda1dd707d40c11fa3d330742346e3df33c3e85cc104900293f5547b2294d2830b37bd317522f47b0797616913d0839e3ba1dd707df0c551aad330d3bef6e7913ac3e8624c049002934550352b94d285a287b97f7c22f40d929761691360877a33a1dd02d540c11fa3d3307423f6e7913a73ec86440490703bf5547b2294d2830b87b97f7c92f0e99a97611bbbd0839e3ba1dd707d40c11fa36334902bf6e7e392c3e8624c04900293f5547b2224d6670387b90dd422f40d9297616913d0839e3b11d9947540c16d0bd3307423f6e7913ac3e8624cb494713cf554cc2794d2830b87b97f7c22f40d9227651abcd0835231a1dd707d40c11fa3d330742346e3e295c3e8bf4304900293f5547b2294d2830b37bd0cd322f4f88697616913d0839e3ba1dd707df0c56c0cd3307939f6e7913ac3e8624c049002934550088d94d29f1487b97f7c22f40d92976169136087ed94a1dd435940c11fa3d3307423f6e7913a73ec11e304903dbaf5547b2294d2830b87b97f7c92f07e3d9761333dd0839e3ba1dd707d40c11fa36334078cf6e7e009c3e8624c04900293f5547b2224d6f0a487b9fc4422f40d9297616913d0839e3b11d903d240c1949ed3307423f6e7913ac3e8624cb494713cf554d26094d2830b87b97f7c22f40d9227651abcd083597ca1dd707d40c11fa3d330742346e3e295c3e8870004900293f5547b2294d2830b37bd0cd322f4e7c397616913d0839e3ba1dd707df0c56c0cd3307674f6e7913ac3e8624c049002934550088d94d2925787b97f7c22f40d92976169136087ed94a1dd581c40c11fa3d3307423f6e7913a73ec11e3049042f5f5547b2294d2830b87b97f7c92f07e3d97613c78d0839e3ba1dd707d40c11fa36334078cf6e7f84ac3e8624c04900293f5547b2224d6f0a487b90d0922f40d9297616913d0839e3b11d903d240c10fdfd3307423f6e7913ac3e8644c049099c7f554723794d2830b87b97f7c22f40b929761864ed083972ea1dd707d40c11fa3d3307223f6e7dd5dc3e86b5904900293f5547b2294d2850b87b9cc0c22f4048797616913d0839e3ba1dd767d40c10ed9d3307d36f6e7913ac3e8624c04900493f55410a194d28a1e87b97f7c22f40d9297616f13d0834cb7a1dd796840c11fa3d3307423f6e7973ac3e8011904902698f5547b2294d2830b87b9797c22f453f297614818d0839e3ba1dd707d40c119a3d3302c48f6e7b031c3e8624c04900293f5547d2294d2c07d87b95e7722f40d9297616913d083983ba1dd55fc40c13ea8d3307423f6e7913ac3e8644c04901f1ff5545a2994d2830b87b97f7c22f40b9297615a85d083d32ba1dd707d40c11fa3d3307223f6e76126c3e8ab4604900293f5547b2294d2850b87b98f6022f4c59897616913d0839e3ba1dd767d40c1efbfd330bc29f6e7913ac3e8624c04900493f5548b3e94d24b0187b97f7c22f40d9297616f13d0836e27a1ddb87740c11fa3d3307423f6e7973ac3e892500490ca99f5547b2294d2830b87b9787c22f4b7819761261ad0839e3ba1dd707d40c118a3d330ea56f6e75c36c3e8624c04900293f5547c2294d20e8b87b9b27022f40d9297616913d083993ba1ddf2f640c1d2afd3307423f6e7913ac3e8654c0490a706f554933394d2830b87b97f7c22f40a929761090fd0832337a1dd707d40c11fa3d3307323f6e7f126c3e8de4004900293f5547b2294d2840b87b91f6022f4b19e97616913d0839e3ba1dd777d40c17fbfd330c82ff6e7913ac3e8624c04900593f5541b3e94d23f0787b97f7c22f40d9297616e13d083fe27a1ddcc7140c11fa3d3307423f6e7963ac3e8454ae9950a93f8547b2294d2830b87b9787c22f40d9297616913d0839e3ba1dd707d40c118a3d330042b5be3933acfe8624c04900293f5547c2294d2830b87b97f7c22f40d9297616913d083993ba1dd707d40c11fa3d3307423f6e7913ac3e8654c04900293f5547b2294d2830b87b97f7c22f40a9297616913d0839e3ba1dd707d40c11fa3d3307323f6e7913ac3e8624c04900293f5547b2294d2840b87b97f7c22f40d9297616913d0839e3ba1dd707d40c11fa3d3307423f6e7913ac3e8624c04900076cbdbcbdb
R length 25, tableID 2, paramID 0, name UNKNOWN, packet ebfbebfb8208007574020000000400140070008800cbdbcbdb
Clicked CONFIRM
W length 21, tableID 50, paramID 21, name CmdToOrigZeroNoV, packet ebfbebfb110400ed003200150001001600cbdbcbdb
R length 21, tableID 32, paramID 1, name UNKNOWN, packet ebfbebfb090400ca932000010001000200cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200e58a0700000000000000000076feffff2afe24424a93cf58cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb80120004a70700000000000000000012f5ffffc6f44f009f2ace9ccbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200c45d07000000000000000000abebffff62eb58695dc636d9cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200d54a070000000000000000004ae2fffffbe1306591cba6fbcbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200d80d07000000000000000000e3d8ffff99d8c954c315f697cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb8012004c920700000000000000000080cfffff35cfa4d5d2e9f31bcbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb8012008084070000000000000000001cc6ffffcec50ced5d812818cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200ed8d07000000000000000000b8bcffff69bcc15dac102814cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200cc390700000000000000000054b3ffff05b39da46138a2d8cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb8012003fd307000000000000000000eea9ffff00005eea888cd5bbcbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200d20607000000000000000000000000000000d628f32f6b7ecbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb80120019b6070000000000000000005b0000005b00b4b451ec70facbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb80120063e107000000000000000000eb010000eb01aeacd83226f9cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb8012001d2b07000000000000000000f5010000f5014740b619022fcbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200210107000000000000000000c9010000c90187fdc0f66685cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb8012007199070000000000000000003f0100003f0186b590fec0cfcbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb80120059b807000000000000000000af000000af00ebd6048c32c8cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb8012006eb107000000000000000000220000000000ab037ce5860dcbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200441b07000000000000000000000000000000019c3381e282cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200c77607000000000000000000c804000077055ae4b6840a75cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200c4c1070000000000000000007a480000e052a7965dc61ddecbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200044407000000000000000000eb8c000023a1b0ff9f2a4898cbdbcbdb
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb8012001f1107000000000000000000d19d000079b48a8024f2c33acbdbcbdb
Homing complete
R length 35, tableID 7, paramID 0, name UNKNOWN, packet ebfbebfb801200293107000000000000000000d19d000079b4919969641c24cbdbcbdb
R length 21, tableID 51, paramID 6, name CmdReqProcessInfo, packet ebfbebfb1104009f763300060001000700cbdbcbdb
W length 21, tableID 50, paramID 4, name CmdPumpClose, packet ebfbebfb11040094003200040001000500cbdbcbdb
R length 21, tableID 32, paramID 1, name UNKNOWN, packet ebfbebfb09040060ab2000010001000200cbdbcbdb
W length 21, tableID 50, paramID 5, name CmdMotorClose, packet ebfbebfb11040022003200050001000600cbdbcbdb
R length 21, tableID 32, paramID 1, name UNKNOWN, packet ebfbebfb090400d8202000010001000200cbdbcbdb
W length 21, tableID 50, paramID 25, name CmdSendRunStatusEnd, packet ebfbebfb11040068003200190001001a00cbdbcbdb
End
R length 21, tableID 32, paramID 1, name UNKNOWN, packet ebfbebfb090400620a2000010001000200cbdbcbdb
```