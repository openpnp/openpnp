# Charmhigh Conversion Page

## Introduction
This page gives instructions on how to get OpenPnP to work with the pick-and-place machines from Charmhigh.

## Overview
This process will have you reflash the pick-and-place's control board firmware with [Smoothieware](https://github.com/Smoothieware/Smoothieware), which is a popular CNC controller.

There are also some recommendations made so that you can get the most out of your machine when using OpenPnP.

## Consider not doing this
This process will surely void your warranty.  You might break your machine.  

It might also make your machine slower.  The Charmhigh firmware has the machine only operate at its maximum speed.  This requires your machine to be placed on a very sturdy table for operation as it will transfer those acceleration forces into its base.  Smoothie is different.  It will accelerate smoothly and you won't have your table shaking every time the machine moves.  This acceleration ramping does come at the cost of some speed.

This process takes also takes time, so plan for your machine to be down for a while.

This process is known to work for the 48VB model.  I do not know for certain if other machines use the exact same control board or if the process will work for other Charmhigh models.

Some of the steps that follow might not be in the best order or might lack detail.  Feel free to update this page if you go through the process and can make it easier for those that follow you.

## Let's go shopping
The parts below are known to work for the conversion.  It might be possible to substitute these parts with others.
* [STLINK-V3SET](https://octopart.com/search?q=STLINK-V3SET)<br>You need a ST device programmer to reflash the firmware on the Charmhigh's main control board.  There are many options available for this so feel free to use another programmer if you have one.
* [Downward looking USB camera](https://www.aliexpress.com/item/3-6-2-8-6-8-12mm-Board-Lens-720P-HD-Very-Small-Bullet-Mini-Waterproof/32892346870.html?spm=a2g0s.9042311.0.0.27424c4d4JlZL1)<br>The cameras built in to the Charmhigh have analog output and are hardwired in to the machine's electronics.  The easiest route is to replace the machine's cameras with USB cameras that can be directly read from the computer running OpenPnP.  Get the 12mm version of the camera at the link above.  The Charmhigh's original down-looking camrea has an 8mm lens on it, but the 12mm will give you more of a zoom in case you want to see 0402 parts better.  If you don't like the 12mm lens you can take the 8mm lens off of the Charmhigh's original analog camera and put it on the USB camera.
* [USB extension cables](https://www.amazon.com/gp/product/B07N2JYMS5/ref=ppx_yo_dt_b_asin_title_o04_s00?ie=UTF8&psc=1)<br>The USB cable on the downward looking camera is not long enough.  You will also need two USB cables to go inside the machine.  It's recommended that you get 3 extension cables.  These extension cables easily fit through the existing opening at the back-left of the machine so your modification will look as professional as possible.  Since these cables come in packs of two you'll have to get two packs so you have three of them for the modification.
* [Semi-closed drag chain](https://www.amazon.com/gp/product/B07MJPBV4Y/ref=ppx_yo_dt_b_asin_title_o02_s00?ie=UTF8&psc=1)<br>Since you are replacing the downward looking camera you are going to have to get in to the drag chain.  However, the drag chain that comes with the Charmhigh isn't made to be modified.  Consider just cutting the cables out of the existing drag chain and replacing it with drag chain that lets you open and close it if you ever have to run more wires through it.  If you do this replacement you'll need to order two sets of this drag chain: one for your x-axis and another for your y-axis.  You need the 15mm x 20mm version.
* [RS-422 adapter](https://www.amazon.com/StarTech-com-Industrial-Serial-Adapter-Isolation/dp/B001Q7X0XK/ref=sr_1_3?keywords=rs485+rs422&qid=1568577935&s=electronics&sr=1-3)<br>The updated firmware will communicate over RS-422 and RS-232.  You'll need a USB to RS-422 adapter for the communication.  This one features isolation that should protect your setup in case there is a problem.
* [Right-angle USB connector](https://www.amazon.com/gp/product/B01HB91CRM/ref=ppx_yo_dt_b_asin_title_o09_s00?ie=UTF8&psc=1)<br>A right-angle connector will make it easy to fit the RS-422 adapter inside your machine.
* [Upward looking USB camera](https://www.aliexpress.com/item/33015394467.html)<br>The camera inside the Charmhigh is not a USB camera.  It's easiest to replace it with a camera like this.  This camera features a global shutter, which means it reads all of its 1 million pixels at the same time, which eliminates motion blur.  It also operates at 60fps, which gives amazing performance.  This camera is black-and-white only, no color, but you shouldn't need color.  It's recommended that you get it with the 2.8-12mm lens, or you can buy the lens separately with the next link.
* [Variable lens for upward camera](https://www.aliexpress.com/item/32879326475.html?spm=a2g0o.detail.0.0.647772824bIqbl&gps-id=pcDetailCartBuyAlsoBuy&scm=1007.12908.99722.0&scm_id=1007.12908.99722.0&scm-url=1007.12908.99722.0&pvid=6c0a11ee-b5e9-4793-a112-31d9b790155f)<br>This variable lens will let you set the field-of-view for your upward looking camera.  Adjust it so that you can see your largest part in the camera when your largest part is rotated by 15 degrees (which is OpenPnP's default value for worst expected rotation error). You can optionally get this lens packaged with your camera when you order it.
* [Black pick nozzles](https://www.aliexpress.com/item/32837486168.html?spm=a2g0s.9042311.0.0.3c0f4c4d9s8v2T)<br>Nozzle tips are usually green so that the tip can act as a [green-screen](https://en.wikipedia.org/wiki/Chroma_key).  The recommended upward looking camera is grayscale only, so it might be better to use black nozzles that won't show up in the camera's background.

## Let's get started
1. Install [OpenPnP](http://openpnp.org/downloads/)
2. Replace your [machine.xml](https://github.com/mattthebaker/openpnp-config-chmt/tree/latest) file<br>Download the machine.xml file from the link.  Use it to replace the default machine.xml that comes with OpenPnP.
3. Connect your RS-422 adapter<br>Plug your adapter in to your computer.  Don't worry about connecting it to your machine just yet.  We just want it to enumerate for the computer so we can set the configuration in OpenPnP.
4. Remove your downward looking camera<br>If you bought the replacement drag chain you can just cut your cables out of the chain.  Remember you're supposed to cut the drag chain, not your cables.  You can leave the camera wired in through the machine for now.  We just need it out of the drag chain.  
5. Mount your new downward camera<br>Place your new downward camera in the mount on the head.  Run its USB cable through the x-axis drag chain.  Connect the camera's USB connector to a USB extension cable between the x-axis drag chain and the y-axis drag chain.  Run the USB extension cable through the y-axis drag chain and connect it to your computer.
6. Configure OpenPnP<br>Open OpenPnP and set the COM port for your RS-422 adapter under the settings for the GCode driver.  Configure the settings for the downward looking camera so you can see the video feed in OpenPnP.
7. Adjust your downward camera<br>Adjust how deep the lens is screwed in to your downward camera and where the camera is positioned vertically so the video feed is in focus and has the zoom you want.

## Reflashing the firmware
This is a one-way process.  You can not undo this operation.  If this process fails you will have to buy a replacement control board from Charmhigh.  You will not be able to get the original Charmhigh firmware back on your control board.

8. Take off the machine's base.<br>Make sure your machine is powered off. The metal top of the pick-and-place is a tooling plate probably made of MIC-6 aluminum.  There are multiple socket head screws around the perimeter of the plate that hold it to the base.  Remove all of these socket head screws to gain access to the inside of the machine.
9. Find the programming pins<br>The main control board is at the center of the machine in the front.  It has four pins that are for reprogramming it.  You'll need to make a connection between these pins and your programmer.  It's recommended that you leave a cable connected to these pins and run it out through the opening at the back-left of your machine in case you need to reflash your machine again in the future.<br><br>I have pictures of this that I will add here tomorrow.<br>
10. Download the new firmware [firmware-chmt.bin](https://github.com/mattthebaker/Smoothieware-CHMT)<br>Follow the link. Click the green "Clone or download" button.  Clcik "Download ZIP".  Inside the "FirmwareBin" folder you will find the firmware-chmt.bin file that you will flash on to the control board.
11. Flash the new firmware.<br>Use a tool like [STSW-LINK004](https://my.st.com/content/my_st_com/en/products/development-tools/software-development-tools/stm32-software-development-tools/stm32-programmers/stsw-link004.license=1561391173034.product=STSW-LINK004.version=4.5.0.html) to first update the firmware of your device programmer (if you are using the recommended STLINK-V3SET).  With your connection and machine securely positioned, turn on your machine, then wipe the memory on the Charmhigh control board, then write the firmware-chmt.bin file to the control board.  Your machine will make some scary sounds while this process is occurring.
12. Turn off your machine

## Update things under the hood
13. Install USB extension cables<br>There is a slot at the back-left of the machine that has wires going in through it.  Push the female end of two USB extension cables in through that slot.
14. Install the RS-422 adapter<br>Connect the RS-422 adapter to the right-angle USB cable and then to the other USB extension cable you wired in through the machine's slot.  Use the screw terminals on the RS-422 adapter to connect a cable to the machine's main control board.  Consider using Velcro to mount the RS-422 adapter to the machine's base.
15. Replace the upward looking camera<br>Remove the stock upward looking camera from the mount and replace it with your new camera.  You'll have to take the 4 socket head screws from the right light off to remove the camera mount.  Screw the variable lens in to the camera and run it all the way down the threads.  Run the camera's USB cable as far as you can towards the slot at the back-left of the machine and connect it to one of the extension cables.  Connect the cable to the computer and get the video feed in OpenPnP.  Use the video feed to make sure the camera is oriented in the camera mount in the correct orientation: so the the left side of the video is on the left side and the right side of the video is to the right of the machine.  Unscrew the lens from the camera so that you can access the adjustment screws on the side of the camera mount.  Make sure you can move the adjustment screws through their full movement while in the camera mount.  Tighten down the lock screw to hold the lens to the camera.
16. Consider using electrical tape to protect the exposed electronics on the bottom of the ring light.
17. Adjust the upward camera<br>Mount two screws to hold the camera holder to the machine's base plate.  Put the right light over the camera.  Turn on the right light in OpenPnP by activating UPLED.  Move the head over the camera.  Turn on the vacuum and manually attach your biggest part to the nozzle on the head.  Use this to adjust the zoom and focus on the upward camera's lens.  Fasten down all of the screws when you have everything in place.
18. Cut the cable to the old downward looking cable and remove it.
19. Reconnect the machine's base<br>Tighten the socket head screws to the machine's base completing the modifications.
20. Celebrate


