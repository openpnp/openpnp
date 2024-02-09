Below is a complete list of TinyG configuration settings taken from a stock LitePlacer that is known to work with OpenPnP. These are also available for download as a text file [here](https://github.com/openpnp/openpnp/files/11004279/TinyG_settings.txt). Note, the settings in **bold** should be customized for your particular machine but the values shown below should be good enough as a starting point: 

* [fb]  firmware build            440.21
* [fv]  firmware version            0.97
* [hp]  hardware platform           1.00
* [hv]  hardware version            8.00
* [id]  TinyG ID                    7X2109-JGS
* **[ja]  junction acceleration 2000000 mm**
* [ct]  chordal tolerance           0.0100 mm
* [sl]  soft limit enable           0
* [st]  switch type                 0 [0=NO,1=NC]
* [mt]  motor idle timeout    1000000.00 Sec
* [ej]  enable json mode            0 [0=text,1=JSON]
* [jv]  json verbosity              0 [0=silent,1=footer,2=messages,3=configs,4=linenum,5=verbose]
* [js]  json serialize style        0 [0=relaxed,1=strict]
* [tv]  text verbosity              1 [0=silent,1=verbose]
* [qv]  queue report verbosity      0 [0=off,1=single,2=triple]
* [sv]  status report verbosity     0 [0=off,1=filtered,2=verbose]
* [si]  status interval    4000000000 ms
* [ec]  expand LF to CRLF on TX     0 [0=off,1=on]
* [ee]  enable echo                 0 [0=off,1=on]
* [ex]  enable flow control         2 [0=off,1=XON/XOFF, 2=RTS/CTS]
* [baud] USB baud rate              5 [1=9600,2=19200,3=38400,4=57600,5=115200,6=230400]
* [net] network mode                0 [0=master]
* [gpl] default gcode plane         0 [0=G17,1=G18,2=G19]
* [gun] default gcode units mode    1 [0=G20,1=G21]
* [gco] default gcode coord system  1 [1-6 (G54-G59)]
* [gpa] default gcode path control  0 [0=G61,1=G61.1,2=G64]
* [gdi] default gcode distance mode 0 [0=G90,1=G91]
* [1ma] m1 map to axis              0 [0=X,1=Y,2=Z...]
* [1sa] m1 step angle               0.900 deg
* **[1tr] m1 travel per revolution   40.0000 mm**
* [1mi] m1 microsteps               8 [1,2,4,8]
* [1po] m1 polarity                 0 [0=normal,1=reverse]
* [1pm] m1 power management         2 [0=disabled,1=always on,2=in cycle,3=when moving]
* [2ma] m2 map to axis              1 [0=X,1=Y,2=Z...]
* [2sa] m2 step angle               0.900 deg
* **[2tr] m2 travel per revolution   40.0000 mm**
* [2mi] m2 microsteps               8 [1,2,4,8]
* [2po] m2 polarity                 0 [0=normal,1=reverse]
* [2pm] m2 power management         2 [0=disabled,1=always on,2=in cycle,3=when moving]
* [3ma] m3 map to axis              2 [0=X,1=Y,2=Z...]
* [3sa] m3 step angle               1.800 deg
* [3tr] m3 travel per revolution    8.0000 mm
* [3mi] m3 microsteps               8 [1,2,4,8]
* [3po] m3 polarity                 1 [0=normal,1=reverse]
* [3pm] m3 power management         2 [0=disabled,1=always on,2=in cycle,3=when moving]
* [4ma] m4 map to axis              3 [0=X,1=Y,2=Z...]
* [4sa] m4 step angle               0.900 deg
* [4tr] m4 travel per revolution  160.0000 mm
* [4mi] m4 microsteps               8 [1,2,4,8]
* [4po] m4 polarity                 0 [0=normal,1=reverse]
* [4pm] m4 power management         2 [0=disabled,1=always on,2=in cycle,3=when moving]
* [xam] x axis mode                 1 [standard]
* **[xvm] x velocity maximum      20000 mm/min**
* **[xfr] x feedrate maximum      20000 mm/min**
* [xtn] x travel minimum            0.000 mm
* [xtm] x travel maximum          600.000 mm
* **[xjm] x jerk maximum         540000 mm/min^3 * 1 million**
* **[xjh] x jerk homing          540000 mm/min^3 * 1 million**
* **[xjd] x junction deviation        0.5000 mm (larger is faster)**
* [xsn] x switch min                3 [0=off,1=homing,2=limit,3=limit+homing]
* [xsx] x switch max                2 [0=off,1=homing,2=limit,3=limit+homing]
* **[xsv] x search velocity        2000 mm/min**
* **[xlv] x latch velocity         1000 mm/min**
* **[xlb] x latch backoff            16.000 mm**
* **[xzb] x zero backoff              2.000 mm**
* [yam] y axis mode                 1 [standard]
* **[yvm] y velocity maximum      20000 mm/min**
* **[yfr] y feedrate maximum      20000 mm/min**
* [ytn] y travel minimum            0.000 mm
* [ytm] y travel maximum          400.000 mm
* **[yjm] y jerk maximum         540000 mm/min^3 * 1 million**
* **[yjh] y jerk homing          540000 mm/min^3 * 1 million**
* **[yjd] y junction deviation        0.5000 mm (larger is faster)**
* [ysn] y switch min                3 [0=off,1=homing,2=limit,3=limit+homing]
* [ysx] y switch max                2 [0=off,1=homing,2=limit,3=limit+homing]
* **[ysv] y search velocity        2000 mm/min**
* **[ylv] y latch velocity         1000 mm/min**
* **[ylb] y latch backoff            13.000 mm**
* **[yzb] y zero backoff              2.000 mm**
* [zam] z axis mode                 1 [standard]
* **[zvm] z velocity maximum       5000 mm/min**
* **[zfr] z feedrate maximum       2000 mm/min**
* [ztn] z travel minimum            0.000 mm
* [ztm] z travel maximum           80.000 mm
* **[zjm] z jerk maximum           5400 mm/min^3 * 1 million**
* **[zjh] z jerk homing           20000 mm/min^3 * 1 million**
* **[zjd] z junction deviation        0.5000 mm (larger is faster)**
* [zsn] z switch min                0 [0=off,1=homing,2=limit,3=limit+homing]
* [zsx] z switch max                3 [0=off,1=homing,2=limit,3=limit+homing]
* **[zsv] z search velocity        1000 mm/min**
* **[zlv] z latch velocity          100 mm/min**
* **[zlb] z latch backoff             4.000 mm**
* **[zzb] z zero backoff              2.000 mm**
* [aam] a axis mode                 1 [standard]
* **[avm] a velocity maximum     200000 deg/min**
* **[afr] a feedrate maximum     200000 deg/min**
* [atn] a travel minimum           -1.000 deg
* [atm] a travel maximum          600.000 deg
* **[ajm] a jerk maximum          51840 deg/min^3 * 1 million**
* [ajh] a jerk homing            5000 deg/min^3 * 1 million
* **[ajd] a junction deviation        0.5000 deg (larger is faster)**
* [ara] a radius value              0.1990 deg
* [asn] a switch min                0 [0=off,1=homing,2=limit,3=limit+homing]
* [asx] a switch max                0 [0=off,1=homing,2=limit,3=limit+homing]
* [asv] a search velocity         600 deg/min
* [alv] a latch velocity          100 deg/min
* [alb] a latch backoff             5.000 deg
* [azb] a zero backoff              2.000 deg
* [bam] b axis mode                 0 [disabled]
* [bvm] b velocity maximum       3600 deg/min
* [bfr] b feedrate maximum       3600 deg/min
* [btn] b travel minimum           -1.000 deg
* [btm] b travel maximum           -1.000 deg
* [bjm] b jerk maximum             20 deg/min^3 * 1 million
* [bjd] b junction deviation        0.0500 deg (larger is faster)
* [bra] b radius value              1.0000 deg
* [cam] c axis mode                 0 [disabled]
* [cvm] c velocity maximum       3600 deg/min
* [cfr] c feedrate maximum       3600 deg/min
* [ctn] c travel minimum           -1.000 deg
* [ctm] c travel maximum           -1.000 deg
* [cjm] c jerk maximum             20 deg/min^3 * 1 million
* [cjd] c junction deviation        0.0500 deg (larger is faster)
* [cra] c radius value              1.0000 deg
* [p1frq] pwm frequency               100 Hz
* [p1csl] pwm cw speed lo            1000 RPM
* [p1csh] pwm cw speed hi            2000 RPM
* [p1cpl] pwm cw phase lo           0.125 [0..1]
* [p1cph] pwm cw phase hi           0.200 [0..1]
* [p1wsl] pwm ccw speed lo           1000 RPM
* [p1wsh] pwm ccw speed hi           2000 RPM
* [p1wpl] pwm ccw phase lo          0.125 [0..1]
* [p1wph] pwm ccw phase hi          0.200 [0..1]
* [p1pof] pwm phase off             0.100 [0..1]
* [g54x] g54 x offset               0.000 mm
* [g54y] g54 y offset               0.000 mm
* [g54z] g54 z offset               0.000 mm
* [g54a] g54 a offset               0.000 deg
* [g54b] g54 b offset               0.000 deg
* [g54c] g54 c offset               0.000 deg
* [g55x] g55 x offset              75.000 mm
* [g55y] g55 y offset              75.000 mm
* [g55z] g55 z offset               0.000 mm
* [g55a] g55 a offset               0.000 deg
* [g55b] g55 b offset               0.000 deg
* [g55c] g55 c offset               0.000 deg
* [g56x] g56 x offset               0.000 mm
* [g56y] g56 y offset               0.000 mm
* [g56z] g56 z offset               0.000 mm
* [g56a] g56 a offset               0.000 deg
* [g56b] g56 b offset               0.000 deg
* [g56c] g56 c offset               0.000 deg
* [g57x] g57 x offset               0.000 mm
* [g57y] g57 y offset               0.000 mm
* [g57z] g57 z offset               0.000 mm
* [g57a] g57 a offset               0.000 deg
* [g57b] g57 b offset               0.000 deg
* [g57c] g57 c offset               0.000 deg
* [g58x] g58 x offset               0.000 mm
* [g58y] g58 y offset               0.000 mm
* [g58z] g58 z offset               0.000 mm
* [g58a] g58 a offset               0.000 deg
* [g58b] g58 b offset               0.000 deg
* [g58c] g58 c offset               0.000 deg
* [g59x] g59 x offset               0.000 mm
* [g59y] g59 y offset               0.000 mm
* [g59z] g59 z offset               0.000 mm
* [g59a] g59 a offset               0.000 deg
* [g59b] g59 b offset               0.000 deg
* [g59c] g59 c offset               0.000 deg
* [g92x] g92 x offset               0.000 mm
* [g92y] g92 y offset               0.000 mm
* [g92z] g92 z offset               0.000 mm
* [g92a] g92 a offset               0.000 deg
* [g92b] g92 b offset               0.000 deg
* [g92c] g92 c offset               0.000 deg
* [g28x] g28 x position             0.000 mm
* [g28y] g28 y position             0.000 mm
* [g28z] g28 z position             0.000 mm
* [g28a] g28 a position             0.000 deg
* [g28b] g28 b position             0.000 deg
* [g28c] g28 c position             0.000 deg
* [g30x] g30 x position             0.000 mm
* [g30y] g30 y position             0.000 mm
* [g30z] g30 z position             0.000 mm
* [g30a] g30 a position             0.000 deg
* [g30b] g30 b position             0.000 deg
* [g30c] g30 c position             0.000 deg
