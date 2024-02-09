In order for the linux install to work, you need to first install java 8. In a terminal:  
`sudo add-apt-repository ppa:webupd8team/java`  
`sudo apt-get update`  
`sudo apt-get install oracle-java8-installer`  
`sudo apt-get install oracle-java8-set-default`  

Note, if the packages cannot be found, try:
```
sudo apt-get install default-jre
```

After installing java 8, run the .deb package in package manager, press install and it should install.

On some operating systems, there will be no start menu shortcut, if so navigate to `opt -> openpnp` and you should find `openpnp.sh`

To add a launcher shortcut (in Ubuntu) run:

```
sudo cp /opt/openpnp/OpenPnP.desktop  /usr/share/applications/
```
