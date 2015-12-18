# Video

[![screen shot 2015-12-17 at 7 38 36 pm](https://cloud.githubusercontent.com/assets/1182323/11888726/cd026382-a4f5-11e5-9b3b-6f9729ebeb0a.png)](https://www.youtube.com/watch?v=Ml03yALid10)

# Step by Step

1. Go to http://www.eclipse.org/downloads/ and download "Eclipse IDE for Java Developers" for your platform. When the download completes, unzip or install Eclipse and start it up.
2. Eclipse will ask you where to store it's workspace. The default is fine. You can click "Use this as the default and do not ask again." if you don't want to be bothered every time you start Eclipse.
3. When Eclipse starts you will see a Welcome screen. Close the Welcome screen by clicking the small X in the tab.
4. You should now see the Package Explorer tab. Right click in this area and select Import.
5. On the Import screen, expand "Git", select "Projects from Git" and click Next.
6. On the Select Repository Source screen select "Clone URI" and click Next.
7. On the Source Git Repository screen enter `https://github.com/openpnp/openpnp.git` in the URI field and click Next.
8. On the Branch Selection screen select the "develop" branch and unselect all other branches, then click Next.
9. On the Local Destination screen enter the directory where you would like to store the OpenPnP source code. You can keep the default if it's okay. Then click Next.
10. Eclipse will now download the OpenPnP source code. This may take a few minutes.
11. When it finishes, on the same screen select the "Import as general project" option and click Next.
12. On the Import Projects screen accept the default project name of "openpnp" and click Finish.
13. The openpnp project will now appear in the Package Explorer. Right click on the project "openpnp" and in the menu select Configure -> Convert to Maven Project. Eclipse will download any resources that it needs and build the project.
14. Expand the openpnp project and look for the `src/main/java folder`. Under this folder expand the `org.openpnp` folder and look for `Main.java`.
15. Right click `Main.java` and from the menu select Run As -> Java Application.
16. After a few seconds OpenPnP will start. Close OpenPnP.
17. You can now edit files in Eclipse and the changes will automatically compile. To run OpenPnP again with your changes just click the green Run button in the toolbar. The button is a green circle with a white right facing arrow in it.



