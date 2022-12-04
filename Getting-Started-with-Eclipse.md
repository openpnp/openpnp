## Video

[![screen shot 2015-12-17 at 7 42 49 pm](https://cloud.githubusercontent.com/assets/1182323/11888779/72454a3a-a4f6-11e5-9907-725518ad7c02.png)](https://www.youtube.com/watch?v=Ml03yALid10)

## Step by Step

1. Go to http://www.eclipse.org/downloads/ and download "Eclipse IDE for Java Developers" for your platform. When the download completes, unzip or install Eclipse and start it up.
2. Eclipse will ask you where to store it's workspace. The default is fine. You can click "Use this as the default and do not ask again." if you don't want to be bothered every time you start Eclipse.
3. When Eclipse starts you will see a Welcome screen. Close the Welcome screen by clicking the small X in the tab.
4. You should now see the Package Explorer tab. Right click in this area and select Import.
5. On the Import screen, expand "Git", select "Projects from Git" and click Next.
6. On the Select Repository Source screen select "Clone URI" and click Next.
7. On the Source Git Repository screen enter `https://github.com/openpnp/openpnp.git` in the URI field<sup>1</sup> or enter the address to your fork at Github if you would like to contribute back to the project later<sup>2</sup> and click Next.
8. On the Branch Selection screen select the "develop" branch and unselect all other branches, then click Next.
9. On the Local Destination screen enter the directory where you would like to store the OpenPnP source code. You can keep the default if it's okay. Then click Next.
10. Eclipse will now download the OpenPnP source code. This may take a few minutes.
11. When it finishes, on the same screen select the "Import as general project" option and click Next.
12. On the Import Projects screen accept the default project name of "openpnp" and click Finish.
13. The openpnp project will now appear in the Package Explorer. Right click on the project "openpnp" and in the menu select Configure -> Convert to Maven Project. Eclipse will download any resources that it needs and build the project. This can take some time and you need to wait for it to complete before proceeding to the next step. 
14. Expand the openpnp project and look for the `src/main/java folder`. Under this folder expand the `org.openpnp` folder and look for `Main.java`.
15. Right click `Main.java` and from the menu select Run As -> Java Application.
16. After a few seconds OpenPnP will start.
17. You can now edit files in Eclipse, the changes will automatically be hot swapped if enabled "Project" -> "Build Automatically". If function signatures were changed hot swap does not apply and you need to rerun OpenPnP with your changes, just click the green Run button in the toolbar. The button is a green circle with a white right facing arrow in it.
18. Close OpenPnP
* <sup>1</sup> It is possible to later change so your clone is pointing to your fork at Github.
* <sup>2</sup> You cannot directly push your changes to OpenPnP, it must go through your own fork.

### Designing UI Elements
To edit user interface Wizards etc. you need to install a graphical UI designer like WindowBuilder.

Follow the video:
- [Eclipse WindowBuilder Setup](https://youtu.be/bOiI6bGpINY)

## Translations in Eclipse

### Java Properties Encoding in UTF-8

To support translations in different languages and scriptures in Eclipse, go to Windows / Preferences / General / Content Type / Text / Java Properties File and set Default encoding to `UTF-8`:

![UTF-8 setting](https://user-images.githubusercontent.com/9963310/205128585-a3a652f0-099f-414f-918c-04ac4b1171d8.png)

### Translate in WindowBuilder

The WindowBuilder designer has built-in localization support, so you can directly select the language in the drop-down (click on arrow-down, see below) and then set and preview texts like labels and tooltips transparently in the graphical designer. WindowBuilder will automatically generate the translation key, add missing translation entries and texts to the Java properties files.

![select language](https://user-images.githubusercontent.com/9963310/205491644-3687cbe9-cf98-44cb-ba7c-e9da8e5e1981.png)

You can also click on the center of the drop-down button to get a tabular editor for all the text strings:

![image](https://user-images.githubusercontent.com/9963310/205493808-56053e4d-a22c-439f-8f89-5e00ba5b499c.png)

The default table contains all the strings in OpenPnP. Select the "Show strings only for current form" checkbox, to just translate the current form.

### Externalize other Strings

Some text strings in the source code might be outside the scope of WindowBuilder's designer. To make these source code texts available for translation, you can use menu **Source / Externalize Strings...**. 

Press **Configure...** to check whether the configuration is correct, if not, make it so:

![image](https://user-images.githubusercontent.com/9963310/205495114-7d3b7c41-15a7-41ea-8614-17a38d60d195.png)

**CAUTION:** if this dialog does not accept the **Properties file location and name** as show here, you must cancel the dialogs and first remove an Exclusion pattern from the `openpnp/src/main/resources` path in the project properties:

![image](https://user-images.githubusercontent.com/9963310/205499213-172562d4-751b-4d4f-8113-9852f7b8748d.png)

This exclusion pattern is apparently mistakenly (?) added in the maven project conversion. See [Eclipse/m2e bug](https://bugs.eclipse.org/bugs/show_bug.cgi?id=369296), and [follow up here](https://github.com/eclipse-m2e/m2e-core/issues/139).

#### Externalize Strings Dialog

The dialog lists all the yet untreated strings found in the source code. Select an entry to position to the code.

**CAUTION:** it is very, _very_ important to understand the nature of a string. If it is a string that has is used for a _computation purpose_ rather than as a _human readable_ text, you must select it and press the **Ignore** button. If in doubt, or if the string has _both_ a human readable and computation meaning, select the string and press **Ignore**, as in the following example. Multi-selection is possible. An `x` sign must appear on the left:

![image](https://user-images.githubusercontent.com/9963310/205495428-20cae7e7-2ed9-49e2-8edb-9c7f2a2e51a0.png)

Only if the string is clearly intended as a **text to only be read by a human**, you can leave it checked. Make sure to set a speaking **Key**, so that the meaning of the text is still understandable for developers. The externalized string will be removed from the source code, so the **Key** is all that is left for understanding it:

![image](https://user-images.githubusercontent.com/9963310/205496316-33658b56-931a-4aed-85ee-b42118d8e964.png)

Yes, composing all these keys well is hard work! Leaving just the proposed numbers is unacceptable (pull requests with these will not be accepted). 

#### Preview Changes

After having carefully assessed each string, press **Next** to review the changes. 

Strings that are not externalized will be marked with the `//$NON-NLS-x$` pattern (if there are multiple strings on one line, `x` is denoting the position): 

![image](https://user-images.githubusercontent.com/9963310/205496468-06466ef8-5b4a-41d4-8413-700785ada917.png)

String that are externalized are replaced with the `Translations.getString()` call, using the defined key:

![image](https://user-images.githubusercontent.com/9963310/205496447-8f6da8ca-22fb-4f72-8ce4-d2a2f5a90fa2.png)

The externalized key=string pair is automatically added to the `translations.properties` file. 

### Working with different configuration directories

When launching OpenPNP from within Eclipse IDE it will use your default configuration directory.
If you want to use an own directory, for example if you are using different branches with different machine.xml, then you can supply the **configDir** variable in the "Run Configuration":

![eclipse_configDir](https://user-images.githubusercontent.com/11256235/90951519-ff069600-e45b-11ea-90e3-b6bf13bd6caf.png)

* Select menu "Run -> Run configurations"
* Select your Main Java Application settings
* Choose the Arguments tab
* In the section VM arguments add: -DconfigDir=yourdirectory

## Additional steps if you want to contribute
1. Ensure you have the automatic code formatter for eclipse activated. See the [coding style-section in the developers guide to download a preconfigured settings file](https://github.com/openpnp/openpnp/wiki/Developers-Guide#coding-style).
2. You need to run tests by executing "Maven test". Maven test needs the full JDK + GIT be installed (not only JRE).
    * Download and install the JDK.
    * Download and install GIT.
5. Add the JDK to the environments in eclipse:
![jdk-env](https://user-images.githubusercontent.com/3868450/51134165-166cae80-1837-11e9-933e-2a6fbf1301ac.PNG)

