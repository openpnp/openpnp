Joint effort to add OpenPnP support for the Neoden 4 PnP machine.

# Commands

It seems like the protocol consist of a send / receive pattern, one or more bytes are sent to the motion controller, and then one of more bytes are received back from the controller.  All number, unless stated otherwise, are given in hex in send / receive examples.

## Unkown commands

### Feeders / peelers
* Selecting feeder
* Configuring feeder (after selecting) is:
```
B1 B2 00 00 00 00 00 00 B9 B10
B1: Strength, hex between 0 and 100% (in Neoden software it's increments of 10)
B2: Feedrate, in hex. (In Neoden software it's in steps of 4,8,12,24,32,48)
B9: Unknown
B10: Unknwon
```

Example dumps 
* Feeder 21, Feedrate 4, Strength 50%:
```
[25/10/2018 10:34:24] Written data (COM2) 
    3f                                                ?                
[25/10/2018 10:34:24] Read data (COM2) 
    0c                                                .                
[25/10/2018 10:34:24] Written data (COM2) 
    5b                                                [                
[25/10/2018 10:34:24] Read data (COM2) 
    41                                                A                
[25/10/2018 10:34:24] Written data (COM2) 
    ff                                                ÿ                
[25/10/2018 10:34:24] Read data (COM2) 
    00                                                .                
[25/10/2018 10:34:24] Written data (COM2) 
    5b                                                [                
[25/10/2018 10:34:24] Read data (COM2) 
    41                                                A                
[25/10/2018 10:34:24] Written data (COM2) 
    32 04 00 00 00 00 00 00 bc 3f                     2.......¼?       
[25/10/2018 10:34:24] Read data (COM2) 
    0c                                                .                
[25/10/2018 10:34:24] Written data (COM2) 
    5b                                                [                
[25/10/2018 10:34:24] Read data (COM2) 
    14                                                .                
[25/10/2018 10:34:24] Written data (COM2) 
    3f                                                ?                
[25/10/2018 10:34:24] Read data (COM2) 
    0c                                                .                
[25/10/2018 10:34:24] Written data (COM2) 
    5b                                                [                
[25/10/2018 10:34:24] Read data (COM2) 
    41                                                A                
```
* Feeder 21, Feedrate 24, Strength 100%:
```
    64 18 00 00 00 00 00 00 03 3f                     d........?       
    0c                                                .                
```
 
* Feeder 30, Feedrate 4, Strength 50%:
```
[25/10/2018 10:39:23] Written data (COM2) 
    3f                                                ?                
[25/10/2018 10:39:23] Read data (COM2) 
    0c                                                .                
[25/10/2018 10:39:23] Written data (COM2) 
    64                                                d                
[25/10/2018 10:39:23] Read data (COM2) 
    0e                                                .                
[25/10/2018 10:39:23] Written data (COM2) 
    ff                                                ÿ                
[25/10/2018 10:39:23] Read data (COM2) 
    00                                                .                
[25/10/2018 10:39:23] Written data (COM2) 
    64                                                d                
[25/10/2018 10:39:23] Read data (COM2) 
    0e                                                .                
[25/10/2018 10:39:23] Written data (COM2) 
    32 04 00 00 00 00 00 00 bc 3f                     2.......¼?       
[25/10/2018 10:39:23] Read data (COM2) 
    0c                                                .                
[25/10/2018 10:39:23] Written data (COM2) 
    64                                                d                
[25/10/2018 10:39:23] Read data (COM2) 
    1f                                                .                
[25/10/2018 10:39:23] Written data (COM2) 
    3f                                                ?                
[25/10/2018 10:39:23] Read data (COM2) 
    0c                                                .                
[25/10/2018 10:39:23] Written data (COM2) 
    64                                                d                
[25/10/2018 10:39:23] Read data (COM2) 
    4a                                                J                
```

### Rails, forward
* Dump of speed 100%, forward (sent, received, alternating lines, starting with sent):
```
    00 00 00 00 c9 03 0c 00 ee 09                     ....É...î.       
    08                                                .                
    09                                                .                
    19                                                .                
    09                                                .                
    4c                                                L                
    47                                                G                
    0b                                                .                
    c7                                                Ç                
    03                                                .                
    00 02 00 00 00 00 00 00 e3 07                     ........ã.       
    07                                                .                
    07                                                .                
    16                                                .                
    07                                                .                
    43                                                C                
```
* Dump speed 90%, forward:
```
[25/10/2018 10:15:36] Written data (COM2) 
    00 00 00 00 c9 03 0c 00 ee 09                     ....É...î.       
[25/10/2018 10:15:36] Read data (COM2) 
    08                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    09                                                .                
[25/10/2018 10:15:36] Read data (COM2) 
    19                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    09                                                .                
[25/10/2018 10:15:36] Read data (COM2) 
    4c                                                L                
[25/10/2018 10:15:36] Written data (COM2) 
    40                                                @                
[25/10/2018 10:15:36] Read data (COM2) 
    0c                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    00                                                .                
[25/10/2018 10:15:36] Read data (COM2) 
    00                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    00                                                .                
[25/10/2018 10:15:36] Read data (COM2) 
    00                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    00                                                .                
[25/10/2018 10:15:36] Read data (COM2) 
    11                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    80                                                €                
[25/10/2018 10:15:36] Read data (COM2) 
    19 04 02 04 01 0f 00 00 00 90                     .........       
[25/10/2018 10:15:36] Written data (COM2) 
    45                                                E                
[25/10/2018 10:15:36] Read data (COM2) 
    09                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    05                                                .                
[25/10/2018 10:15:36] Read data (COM2) 
    05                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    05                                                .                
[25/10/2018 10:15:36] Read data (COM2) 
    14                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    85                                                …                
[25/10/2018 10:15:36] Read data (COM2) 
    1c 00 01 00 14 00 00 00 00 3d                     .........=       
[25/10/2018 10:15:36] Written data (COM2) 
    45                                                E                
[25/10/2018 10:15:36] Read data (COM2) 
    09                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    05                                                .                
[25/10/2018 10:15:36] Read data (COM2) 
    14                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    85                                                …                
[25/10/2018 10:15:36] Read data (COM2) 
    1c 00 01 00 14 00 00 00 00 3d                     .........=       
[25/10/2018 10:15:36] Written data (COM2) 
    45                                                E                
[25/10/2018 10:15:36] Read data (COM2) 
    09                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    05                                                .                
[25/10/2018 10:15:36] Read data (COM2) 
    14                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    85                                                …                
[25/10/2018 10:15:36] Read data (COM2) 
    1c 00 01 00 14 00 00 00 00 3d                     .........=       
[25/10/2018 10:15:36] Written data (COM2) 
    47                                                G                
[25/10/2018 10:15:36] Read data (COM2) 
    0b                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    c7                                                Ç                
[25/10/2018 10:15:36] Read data (COM2) 
    03                                                .                
[25/10/2018 10:15:36] Written data (COM2) 
    00 02 00 00 00 00 00 00 e3 07                     ........ã.       
[25/10/2018 10:15:36] Read data (COM2) 
    07                                                .                
```
* Dump speed 80%, forward:
```
[25/10/2018 10:16:01] Written data (COM2) 
    00 00 00 00 c9 03 0c 00 ee 09                     ....É...î.       
[25/10/2018 10:16:01] Read data (COM2) 
    08                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    09                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    19                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    09                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    4c                                                L                
[25/10/2018 10:16:01] Written data (COM2) 
    40                                                @                
[25/10/2018 10:16:01] Read data (COM2) 
    0c                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    00                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    00                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    00                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    11                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    80                                                €                
[25/10/2018 10:16:01] Read data (COM2) 
    19 04 02 03 01 0f 00 00 00 d1                     .........Ñ       
[25/10/2018 10:16:01] Written data (COM2) 
    45                                                E                
[25/10/2018 10:16:01] Read data (COM2) 
    09                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    14                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    85                                                …                
[25/10/2018 10:16:01] Read data (COM2) 
    1c 00 01 00 14 00 00 00 00 3d                     .........=       
[25/10/2018 10:16:01] Written data (COM2) 
    45                                                E                
[25/10/2018 10:16:01] Read data (COM2) 
    09                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    14                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    85                                                …                
[25/10/2018 10:16:01] Read data (COM2) 
    1c 00 01 00 14 00 00 00 00 3d                     .........=       
[25/10/2018 10:16:01] Written data (COM2) 
    45                                                E                
[25/10/2018 10:16:01] Read data (COM2) 
    09                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    14                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    85                                                …                
[25/10/2018 10:16:01] Read data (COM2) 
    1c 00 01 00 14 00 00 00 00 3d                     .........=       
[25/10/2018 10:16:01] Written data (COM2) 
    47                                                G                
[25/10/2018 10:16:01] Read data (COM2) 
    0b                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    c7                                                Ç                
[25/10/2018 10:16:01] Read data (COM2) 
    03                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    00 02 00 00 00 00 00 00 e3 07                     ........ã.       
[25/10/2018 10:16:01] Read data (COM2) 
    07                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    07                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    16                                                .                
[25/10/2018 10:16:01] Written data (COM2) 
    07                                                .                
[25/10/2018 10:16:01] Read data (COM2) 
    43                                                C                
```
* Dump speed 10%, backward:
```
[25/10/2018 10:17:01] Written data (COM2) 
    00 00 00 00 37 fc f3 ff 95 09                     ....7üóÿ•.       
[25/10/2018 10:17:01] Read data (COM2) 
    08                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    09                                                .                
[25/10/2018 10:17:01] Read data (COM2) 
    08                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    09                                                .                
[25/10/2018 10:17:01] Read data (COM2) 
    4c                                                L                
[25/10/2018 10:17:01] Written data (COM2) 
    40                                                @                
[25/10/2018 10:17:01] Read data (COM2) 
    0c                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    00                                                .                
[25/10/2018 10:17:01] Read data (COM2) 
    00                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    00                                                .                
[25/10/2018 10:17:01] Read data (COM2) 
    11                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    80                                                €                
[25/10/2018 10:17:01] Read data (COM2) 
    19 03 02 03 02 0f 00 00 00 1b                     ..........       
[25/10/2018 10:17:01] Written data (COM2) 
    45                                                E                
[25/10/2018 10:17:01] Read data (COM2) 
    09                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:17:01] Read data (COM2) 
    05                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:17:01] Read data (COM2) 
    14                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    85                                                …                
[25/10/2018 10:17:01] Read data (COM2) 
    1c 00 01 00 14 00 00 00 00 3d                     .........=       
[25/10/2018 10:17:01] Written data (COM2) 
    45                                                E                
[25/10/2018 10:17:01] Read data (COM2) 
    09                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:17:01] Read data (COM2) 
    14                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    85                                                …                
[25/10/2018 10:17:01] Read data (COM2) 
    1c 00 01 00 14 00 00 00 00 3d                     .........=       
[25/10/2018 10:17:01] Written data (COM2) 
    45                                                E                
[25/10/2018 10:17:01] Read data (COM2) 
    09                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:17:01] Read data (COM2) 
    05                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:17:01] Read data (COM2) 
    05                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    05                                                .                
[25/10/2018 10:17:01] Read data (COM2) 
    14                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    85                                                …                
[25/10/2018 10:17:01] Read data (COM2) 
    1c 00 01 00 14 00 00 00 00 3d                     .........=       
[25/10/2018 10:17:01] Written data (COM2) 
    47                                                G                
[25/10/2018 10:17:01] Read data (COM2) 
    0b                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    c7                                                Ç                
[25/10/2018 10:17:01] Read data (COM2) 
    03                                                .                
[25/10/2018 10:17:01] Written data (COM2) 
    00 02 00 00 00 00 00 00 e3 07                     ........ã.       
[25/10/2018 10:17:01] Read data (COM2) 
    07                                                .                
```

## Status commands, sent periodically, several times per second
### 80 - Get status (pressure, and more)
Send: `80`
Examples of receive (each line is an example of a response):
* `19 03 00 03 01 0f 00 00 00 2a`
* `19 03 02 04 01 0f 00 00 00 88`

* Byte 1: Unknown
* Byte 2: Pressure, nozzle 1
* Byte 3: Pressure, nozzle 2
* Byte 4: Pressure, nozzle 3
* Byte 5: Pressure, nozzle 4
* Byte 6-10: Unknown

### 00 - Unknown status command
Send: `00`
Examples of receive (each line is an example of a response):
* `11`

### 05 - Unknown status command
Send: `05`
Examples of receive (each line is an example of a response):
* `05`
* `14`

### 40 - Unknown status command
Send: `40`
Examples of receive (each line is an example of a response):
* `0c`

### 45 - Unknown status command
Send: `45`
Examples of receive (each line is an example of a response):
* `09`

### 49 - Unknown status command
Send: `49`
Examples of receive (each line is an example of a response):
* `04`

### 85 - Unknown status command
Send: `85`
Examples of receive (each line is an example of a response):
* `1c 00 03 00 14 00 00 00 00 de`

### c9 - Unknown status command
Send: `c9`
Examples of receive (each line is an example of a response):
* `0c`

