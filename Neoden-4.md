Joint effort to add OpenPnP support for the Neoden 4 PnP machine.

# Coordinate system

The machines motion controller has the following coordinate system:
<pre>
Left             (Back / Top)        Right
             (0, 0)        (0,437)
             +-------------------+
             |                   |   Feeders
Feeders      |                   |   Up-looking camera
             |                   |   Feeders
             +-------------------+
             (-437, 0) (-437, 437)
                (Front / Bottom)
</pre>


# Commands

The protocol consists of a series of write / read sequences, most are single byte write / read sequences, but not all. The last byte of longer sequences (both when writing and reading) is a checksum of sorts. See the "Homing the head" command for a more detailed description of the protocol, with an actual example.

Format is:<br>
`write` -> `expected answer`

**NOTE!** All move and homing commands are done with nozzles as high as possible. When no parts are attached nozzles are usually reset to top position before moving.

## Homing the head
<pre>
47 -> 0b
c7 -> 03
0100000000000000XX
07 -> 07
07 -> 43
</pre>

`XX` is the checksom for the message.
The other strange thing you see is that there's no response on the long array of bytes, but rather after sending a single byte right after the long array of bytes. This is the same for most (all?) long array of bytes sent.
The last write / read might be repeated, until the read is actually the correct response.

## Moving the head

<pre>
48 -> 05
c8 -> 0d
x1x2x3x4y1y2y3y4XX
08 -> 09
08 -> 4d
</pre>

`x1x2x3x4` is the X-coordinate in int32, little endian, in .01mm
`y1y2y3y4` is the Y-coordinate in int32, little endian, in .01mm
`XX` is the checksum of the message.

## Nozzles

### Rotating
<pre>
41 -> 0d
c1 -> 05
d1d232NN00000000XX
01 -> 01
01 -> 45
</pre>

`XX` is the checksum of the message.
`d1d2` degrees of rotation in 0.1 units, int16, little endian. f8f8 = -1800, 0000 = 0, 0807 = 1800
`NN` is 01, 02, 03 or 04. For Nozzles 1-4.

### Moving up / down
<pre>
42 -> 0e
c2 -> 06
h1h232NN00000000XX
02 -> 02
02 -> 46
</pre>

`XX` is the checksum of the message.
`NN` is 01, 02, 03 or 04. For nozzles 1-4.
`h1h2` is int16, little endian. Height of nozzle; 0000 is max retracted into head, e02e is max down.
In the neoden software it is visualised as 12.0 -> 0.0 (12.0 being max retracted into head).


### Blowing / sucking
<pre>
43 -> 0f
c3 -> 07
PPNN000000000000XX 
03 -> 03
03 -> 47
</pre>

`XX` is the checksum of the message.
`NN` is 01, 02, 03 or 04. For nozzle 1-4.
`PP` 01 => blow, 00 => idle, ff => max suction.

## Flash for camera

### Down camera, mounted on head
<pre>
44 -> 08
c4 -> 00
MM00000000000000XX
04 -> 04
04 -> 40
</pre>

`XX` is the checksum of the message.
`MM` is the mode 00=off, 01=mode1, 02=mode2, 03=mode3.

### Up camera, between feeders
<pre>
</pre>

## Feeders and peelers

**NOTE!** This section is still work-in-progress.

### Feeders
Starting the feeder
<pre>
3f -> 0c
xx -> yy   // xx=0x47== feeder01  yy=response
ff -> 00
xx -> yy   // xx and yy == same as above
SS RR 00 00 00 00 00 00 XX
3f -> 0c                                                .                
56 -> 18
3f -> 0c
56 -> 4d
</pre>

`XX` is the checksum of the message.
`SS` is feed strength, 32 = 50%.
`RR` is feed rate. 04 is used for 0402, for instance.

Then, getting status
<pre>
64 -> 0e
24 -> 02
24 -> 13
a4 -> 1b 
   -> MM 00 00 00 00 00 00 00 XX
</pre>

`XX` is the checksum of the message (read).
`MM` when MM is 20, feeder is done.

### Peelers
<pre>
4c -> 01
cc -> 09
PPRRSS0000000000XX
0c -> 0d
0c -> 49
</pre>

`XX` is the checksum of the message.
`PP` is the peeler number; 0e= peeler 14.
`RR` is the feedrate of the peeler; 10 = feedrate 16
`SS` is the strength of the peeler; 50 = 80% strength.

Seems like the peeler is always started before the feeder, when feeding components.

## Rails

### Moving forward (into the machine, from Front)
<pre>
49 -> 04
c9 -> 0c
00000000c9030c00XX
09 -> 08
09 -> 4c
</pre>

`XX` is the checksum for the message.
What is currently unknown is how to adjust speed on the rails.


### Moving backward (out of the machine, to Front)
<pre>
49 -> 04 
c9 -> 0c
0000000037fcf3ffXX 
09 -> 08
09 -> 4c
</pre>

`XX` is the checksum for the message.
What is currently unknown is how to adjust speed on the rails.

### Stopping a move
<pre>
47 -> 0b
c7 -> 03
0002000000000000XX
07 -> 07
07 -> 43
</pre>

`XX` is the checksum for the message.
Whenever you need to stop moving the rails, you issue this command.