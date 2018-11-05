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
`47` -> `0b`<br>
`c7` -> `03`<br>
`0100000000000000XX`<br>
`07` -> `07`<br>
`07` -> `43`<br>

`XX` is the checksom for the message.
The other strange thing you see is that there's no response on the long array of bytes, but rather after sending a single byte right after the long array of bytes. This is the same for most (all?) long array of bytes sent.
The last write / read might be repeated, until the read is actually the correct response.

## Moving the head

`48` -> `05`<br>
`c8` -> `0d`<br>
`x1x2x3x4y1y2y3y4XX`<br>
`08` -> `09`<br>
`08` -> `4d`<br>

`x1x2x3x4` is the X-coordinate in int32, little endian, in .01mm
`y1y2y3y4` is the Y-coordinate in int32, little endian, in .01mm
`XX` is the checksum for the message.