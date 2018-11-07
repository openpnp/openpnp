Joint effort to add OpenPnP support for the Neoden 4 PnP machine.

# Coordinate system

The machines motion controller has the following coordinate system:
<pre>
Left             (Back / Top)        Right
          (-437, 437)          (0,437)
             +-------------------+
             |                   |   Feeders
Feeders      |                   |   Up-looking camera
             |                   |   Feeders
             +-------------------+
          (-437, 0)            (0, 0)
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

`XX` is the checksom for the message.<br>
The other strange thing you see is that there's no response on the long array of bytes, but rather after sending a single byte right after the long array of bytes. This is the same for most (all?) long array of bytes sent.
The last write / read might be repeated, until the read is actually the correct response.

**NOTE!** Home seems to be `(-437, 437)`, eg left top corner.

## Moving the head

<pre>
48 -> 05
c8 -> 0d
x1x2x3x4y1y2y3y4XX
08 -> 09
08 -> 4d
</pre>

`x1x2x3x4` is the X-coordinate in int32, little endian, in .01mm<br>
`y1y2y3y4` is the Y-coordinate in int32, little endian, in .01mm<br>
`XX` is the checksum of the message.<br>

## Nozzles

### Rotating
<pre>
41 -> 0d
c1 -> 05
d1d232NN00000000XX
01 -> 01
01 -> 45
</pre>

`XX` is the checksum of the message.<br>
`d1d2` degrees of rotation in 0.1 units, int16, little endian. f8f8 = -1800, 0000 = 0, 0807 = 1800<br>
`NN` is 01, 02, 03 or 04. For Nozzles 1-4.<br>

### Moving up / down
<pre>
42 -> 0e
c2 -> 06
h1h232NN00000000XX
02 -> 02
02 -> 46
</pre>

`XX` is the checksum of the message.<br>
`NN` is 01, 02, 03 or 04. For nozzles 1-4.<br>
`h1h2` is int16, little endian. Height of nozzle; 0000 is max retracted into head, e02e is max down.<br>
In the neoden software it is visualised as 12.0 -> 0.0 (12.0 being max retracted into head).


### Blowing / sucking
<pre>
43 -> 0f
c3 -> 07
PPNN000000000000XX 
03 -> 03
03 -> 47
</pre>

`XX` is the checksum of the message.<br>
`NN` is 01, 02, 03 or 04. For nozzle 1-4.<br>
`PP` 01 => blow, 00 => idle, ff => max suction.<br>

### Status (vacuum / suction)
<pre>
40 -> 0c
00 -> 11
80 -> 19
   -> n1 n2 n3 n4 03 00 00 00 XX
</pre>

`XX` is the checksum of the message.<br>
`n1` is the vacuum / pressure for nozzle 1, int8. Negative is vacuum <br>
`n2` is the vacuum / pressure for nozzle 2, int8. Negative is vacuum <br>
`n3` is the vacuum / pressure for nozzle 3, int8. Negative is vacuum <br>
`n4` is the vacuum / pressure for nozzle 4, int8. Negative is vacuum <br>

## Flash for camera

### Down camera, mounted on head
<pre>
44 -> 08
c4 -> 00
MM00000000000000XX
04 -> 04
04 -> 40
</pre>

`XX` is the checksum of the message.<br>
`MM` is the mode 00=off, 01=mode1, 02=mode2, 03=mode3.<br>

### Up camera, between feeders
<pre>
</pre>

## Feeders and peelers

**NOTE!** This section is still work-in-progress.

### Feeders

#### Feed a part
<pre>
3f -> 0c
xx -> yy   // xx=0x47== feeder01  yy=response
ff -> 00
xx -> yy   // xx and yy == same as above
SS RR 00 00 00 00 00 00 XX
3f -> 0c    # Repeat,                                           .                
xx -> 4d    # these two until response is 4n
</pre>

`XX` is the checksum of the message.<br>
`SS` is feed strength, 32 = 50%.<br>
`RR` is feed rate. 04 is used for 0402, for instance.<br>

#### Unknown status #1
Seen when using feeders: 
<pre>
64 -> 0e
24 -> 13  # repeat if response isn't 13
a4 -> 1b 
   -> MM 00 00 00 00 00 00 00 XX
</pre>

`XX` is the checksum of the message (read).<br>
`MM` when MM is 20, feeder is done / ready again.<br>

#### Unknown status $2
Seen when using feeders: 14
<pre>
62 -> 08
22 -> 15   # repeat if response isn't 15
a2 -> 1d
   -> MM 00 00 00 00 00 00 00 XX
</pre>

`XX` is the checksum of the message (read).<br>
`MM` when MM is 20, feeder is done / ready again.<br>

#### Unknown status #3
Seen when using feeders: 21
<pre>
69 -> 02
29 -> 1f   # repeat if response isn't 1f
a9 -> 17
   -> MM 00 00 00 00 00 00 00 XX
</pre>

`XX` is the checksum of the message (read).<br>
`MM` when MM is 20, the feeder is done / ready again.<br>

#### Unknown status #4
Seen when using feeders: 1
<pre>
5c -> 02
1c -> 1f   # repeat if response isn't 1f
9c -> 17
   -> MM 00 00 00 00 00 00 00 XX
</pre>

`XX` is the checksum of the message (read).<br>
`MM` when MM is 20, the feeder is done / ready again.<br>

#### Unknown status #4
Seen when using feeders: 30
<pre>
72 -> 0b / 1a  # Seen both 0b and 1a on same feeder
32 -> 16       # repeat if response isn't 16
b2 -> 1e
   -> MM 00 00 00 00 00 00 00 XX
</pre>

`XX` is the checksum of the message (read).<br>
`MM` when MM is 20, the feeder is done / ready again.<br>


### Peelers
<pre>
4c -> 01
cc -> 09
PPRRSS0000000000XX
0c -> 0d
0c -> 49
</pre>

`XX` is the checksum of the message.<br>
`PP` is the peeler number; 0e= peeler 14.<br>
`RR` is the feedrate of the peeler; 10 = feedrate 16<br>
`SS` is the strength of the peeler; 50 = 80% strength.<br>

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

`XX` is the checksum for the message.<br>
What is currently unknown is how to adjust speed on the rails.


### Moving backward (out of the machine, to Front)
<pre>
49 -> 04 
c9 -> 0c
0000000037fcf3ffXX 
09 -> 08
09 -> 4c
</pre>

`XX` is the checksum for the message.<br>
What is currently unknown is how to adjust speed on the rails.

### Stopping a move
<pre>
47 -> 0b
c7 -> 03
0002000000000000XX
07 -> 07
07 -> 43
</pre>

`XX` is the checksum for the message.<br>
Whenever you need to stop moving the rails, you issue this command.

## Unknown messages
### Unknown status message
<pre>
45 -> 09
05 -> 14
85 -> 1c
   -> 00 03 00 14 00 00 00 00 XX
</pre>

`XX` is the checksum for the message.<br>
This is repeated at a regular interval when running a job, or when being in the "Manual test" UI in the original application from Neoden. Currently unknown why and what it does.