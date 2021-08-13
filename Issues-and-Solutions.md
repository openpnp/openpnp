## What is it?
____
NOTE: some part of the following are not yet available in the current OpenPnP Version. It is already documented here, because some solutions currently in development are linked to sections on this page. 
____

OpenPnP is complex to set up and the more flexibility and optimization is added, the more the complexity grows. In the course of developing and community testing the [[Advanced Motion Control]] it became apparent, that it is easier to program an automatism to _propose_ the proper setup than to document or provide support for it.

Hence, the Issues & Solutions system was born. It has these features:

1. Tracks and structures your progress in the machine setup process (Milestones).
2. Detects issues and (most of the time) proposes a solution.
3. Lists issues from outright mistakes to suggestions for optimization.
4. The user is the boss: proposed solutions can be accepted or dismissed.
5. Accept solutions, explore them and undo them, if you don't like them.
6. Significantly lowers the required expertise of users. Solutions generate machine-specific, ready-to-use Gcode commands and regular expressions.
7. Removes former `machine.xml` hacking.
8. Each issue/solution is accompanied by an Info page, often linking to this Wiki.

## General Usage

### Top Bar

![Issues and Solutions](https://user-images.githubusercontent.com/9963310/118361321-376e7700-b58b-11eb-8ea8-48a8c6dc6ff0.png)

![Find Issues & Solutions](https://user-images.githubusercontent.com/9963310/118360497-338d2580-b588-11eb-801b-7acd6c0886d4.png) Scans your whole machine configuration for Issues & Solutions. The found issues will be listed below. 

**Include Solved?**: Includes issues in the list that already have been accepted as a solution before. This only works for issues that can be revisited, most notably calibration solutions. 

**Include Dismissed?**: Includes issues in the list that have been dismissed before. You can then reopen them and revisit the reported issue or proposed solution. 

![Info](https://user-images.githubusercontent.com/9963310/118359670-d8a5ff00-b584-11eb-9f89-b46d004b4630.png) Goes to this Wiki page. 

### Issues

Each issue points to a problem or task at hand. Select it in the list to see the details in the area below the list.

Some issues just alert you to problems or tasks, i.e. you have to resolve it yourself, by configuring OpenPnP settings or for instance by setting your motion controller up right. Other issues propose a ready-made solution, and you can just accept it to be automatically applied to your machine setup. If you don't like the proposed solution, you can alternatively dismiss it and resolve the issue manually. 

![Info](https://user-images.githubusercontent.com/9963310/118359670-d8a5ff00-b584-11eb-9f89-b46d004b4630.png) Links to the Wiki or other sources for guidance regarding this issue. **Note**, the issue specific button is the one at the bottom. 

![Accept](https://user-images.githubusercontent.com/9963310/118359624-9f6d8f00-b584-11eb-875d-b89c068e97de.png) Accepts the proposed solution to be applied to your machine configuration. **Important:** immediately after doing that, there is a window of opportunity to safely inspect the applied solution and perhaps to even try it out. The solution can then still be undone by pressing **Reopen**. However, as soon as you press **Find Issues & Solutions** again, the changes will be permanent. 

![Dismiss](https://user-images.githubusercontent.com/9963310/118359604-8d8bec00-b584-11eb-9fe1-1816ec853985.png) Dismisses the reported issue or proposed solution. **Caution**: unless the reported issue is a false alarm (which it rarely is), the underlying problem will not magically go away, just by pressing **Dismiss**! Be sure to really understand what you are dismissing, and that you have provided a better, alternative resolution, or that you are sure it is a false alarm.

![Reopen](https://user-images.githubusercontent.com/9963310/118359592-7fd66680-b584-11eb-9259-3103db8c6e0c.png) Reopens an issue that has previously been accepted or dismissed. As long as you haven't pressed **Find Issues & Solutions** in the meantime, the former state of your machine configuration will be restored. 

### Severity

Issues are classified and color-coded by **Severity**. 

* **Fundamental**: Issues that must be resolved up-front. They represent fundamental building blocks that lay the foundation for further work. Only when the answer to that issue is provided, can the Issues & Solutions system go on to find more solutions. 
* **Information**: Issues that provide information, guidance to go on with the process. 
* **Suggestion**: Issues that point to configuration that may be missing or can likely be improved. If you hand-tuned your configuration, a suggestion may pop up to revert it to the suggested standard setting. Dismiss it in these cases. 
* **Warning**: Issues that point to configuration that is likely wrong or missing. 
* **Error**: Issues that point to configuration that is almost certainly wrong or missing. 

## Milestones / Tracking Progress

Issues & Solutions has **Milestones** to track the progress when building and configuring a machine. Only the Issues & Solution due for the current milestone will be shown. Some solutions will be adaptive to the targeted milestone, proposing simpler solutions for earlier milestones. These will be revisited when you proceed to later milestones. 

For troubleshooting, you can also go back to earlier milestones. Issues & Solutions will then sometimes propose taking back more advanced choices you already made. This way you can try to solve a problem by starting from a safer base. 

The last issue per milestone will always be the **Milestone Issue**, giving you the option to proceed or go back:

![Milestone issue](https://user-images.githubusercontent.com/9963310/116826874-95b45680-ab96-11eb-9d04-c67612b5ba61.png)

## The Milestones

### Welcome Milestone 
The **Welcome** milestone allows you to get to know OpenPnP with a simulation machine. Furthermore, you can use Issues & Solutions to choose between common head / nozzle topologies: standalone nozzles, pairs of nozzles with shared Z and negation, pairs with shared Z and cam. Using a multiplier you can tell OpenPnP how many standalone nozzles or nozzle pairs you want. Issues & Solutions then creates all the required axes and nozzles and wires them up. 

![Nozzle Solutions](https://user-images.githubusercontent.com/9963310/116826397-0f971080-ab94-11eb-83a0-f00aa16103ef.png)

Even complex nozzle solutions are created with a few clicks:

![Complex nozzle solution](https://user-images.githubusercontent.com/9963310/116826561-e32fc400-ab94-11eb-9354-e14ac4b391a3.png)

You can even revisit the solution i.e. change the number and type again and again. Issues & Solutions will carefully re-use any nozzles and axes it finds and only add new ones as needed. On reused components the detail configuration is preserved (except for names).  

### Connect Milestone 
The **Connect** milestone handles the conversion from the simulated machine to a real connected machine with motion controller and USB cameras. It also tests the driver connection and tries to discover the firmware of your controller. If the firmware cannot be detected, there is the option to use generic G-code.

![Connect Firmware](https://user-images.githubusercontent.com/9963310/116827634-56880480-ab9a-11eb-9ee4-d0581348daa6.png)


### Basics Milestone 
The **Basics**  milestone handles moving around the machine manually a.k.a. jogging, switching vacuum and lights. It makes sure all axes have letters and are mapped to drivers. In case you use a G-code motion controller, it generates most G-code snippets, regular expressions etc. to talk to your motion controller.

### Kinematics Milestone
The **Kinematics** milestone defines the kinematic profile of your machine, used for automatic motion: Safe Z, soft limits, motion control model, feed-rates, acceleration limits etc.

### Vision Milestone 
The **Vision** milestone sets up the cameras for Computer Vision. A calibration rig is used to calibrate the camera lenses and optical properties such as the true spacial position and imaging scale of the camera. See the [[Vision Solutions]] page for more information.

### Calibration Milestone 
The **Calibration** milestone brings Kinematics and Vision together to calibrate the machine. Nozzle head offsets, backlash compensation etc. See the [[Calibration Solutions]] page for more information.

### Production Milestone 
The **Production** milestone handles recurrent configuration tasks such as feeder setup as well as production issues such as board, placement and job handling.  

### Advanced Milestone
The **Advanced** milestone, while also serving as a production milestone, finally exposes all the advanced features, such as advanced driver settings, motion planner, asynchronous driver, contact probing nozzle etc. For the most problematic of these settings, Issues & Solutions will propose taking them back when going back to earlier milestones.


## Video

**Outdated:** ~~For a quick Demo, [watch the video](https://youtu.be/VVaZo6BfhOM). Please disregard any mention of "testing version", "migration" etc. the system is now integrated into regular OpenPnP 2.0 and covers both new or changed machines and machines migrated from earlier versions.~~

___


| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Computer Vision]] | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Next Steps](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Next-Steps) |
___

## Advanced Motion Control Topics

### Motion Control
- [[Advanced Motion Control]]
- [[GcodeAsyncDriver]]
- [[Motion Planner]]
- [[Visual Homing]]
- [[Motion Controller Firmwares]]

### Machine Axes
- [[Machine Axes]]
- [[Backlash-Compensation]]
- [[Transformed Axes]]
- [[Linear Transformed Axes]]
- [[Mapping Axes]] 
- [[Axis Interlock Actuator]]

### General
- [[Issues and Solutions]]
