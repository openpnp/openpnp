## Where are configuration and log files located?

The configuration and log files are located in a subdirectory of your user's home directory called `.openpnp`.

To find your home directory, check out this Wikipedia article that explains where the home directory lives on various operating systems: http://en.wikipedia.org/wiki/Home_directory

Examples:

```
Windows: C:\Users\your_username\.openpnp
Linux: /home/your_username/.openpnp
Mac OS X: /Users/your_username/.openpnp
```

Configuration files are `machine.xml`, `parts.xml` and `packages.xml` along with other plugin specific files.

Log configuration is in `log4j.properties`.

Log files are under the `log` subdirectory and the current file is always called `OpenPnP.log`.

## How do I reset my configuration?

Sometimes it's easiest just to completely reset your configuration and start over. To do that, just delete the whole OpenPnP configuration directory. See [Where are configuration and log files located?](#where-are-configuration-and-log-files-located) for it's location.


## How do I use other config directory

It's possible to use command line argument for selecting the config directory, example below:

```
java -DconfigDir=src/main/resources/config -jar target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
```

## How do I turn on debug logging?

First, make sure OpenPnP is not running.

Next, find your log configuration file. See [Where are configuration and log files located?](#where-are-configuration-and-log-files-located) to do that.

Edit the file in any basic text editor and follow the instructions within.

For instance, if you wanted to turn on driver debug logging, you would find the line that says `### change to debug to log low-level driver activity` and change the `info` string to `debug`.

Save the file and start OpenPnP.