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

For instance, if you wanted to turn on driver trace logging, you would find the line that says `### change to debug to log low-level driver activity` and change the `info` string to `trace`. You can also use `debug` here. Trace shows more information than debug.

Save the file and start OpenPnP.

## How do I troubleshoot GcodeDriver?

See https://github.com/openpnp/openpnp/wiki/GcodeDriver#troubleshooting

## "It would be faster to do it by hand."

This comes up so often on the mailing list I thought it would be worth addressing as a FAQ. Often, when someone is talking about building a machine or discussing requirements, someone will comment with things along the lines of:

* "Running a pick and place is more work than you think."
* "For small runs it's faster just to do it by hand."
* "You will spend more time setting it up than using it."
* "It will cost you more in the end than just sending the work out, hiring someone, etc."

While some of these reasons might fit your situation, it is important to keep in mind that everyone has their own reasons for wanting to build a pick and place, and very often those reasons have nothing to do with time or money. Here are some other good reasons to build a pick and place machine, even if it's comparatively slow or costs a lot of money:

* Inability to place by hand due to age, illness, disability or disease.
* Live in a place where it's cost prohibitive or just impossible to send work out.
* Want to learn more about CNC, computer vision, pick and place, electronics assembly, etc.
* Just doing it for fun!
* Strange or complex jobs that contract assembly houses don't want to deal with.
* Want to try a new or novel idea for placing components.
* Any other reason you like!

It's okay to let people know about the complexities of running a machine, but I've found that this topic tends to come up over and over again and people seem to feel the need to really pound the point home that it might cost money or time to run the machine. Make your point, but remember that there are lots of reasons to run a machine other than just building boards for a business.