There are many ways to do translations. 

## Using Eclipse

If you already followed the developer guide and installed Eclipse and WindowBuilder, you can 
translate directly on the dialog editor or use the built-in "Externalized strings" tabular 
editor throughout OpenPnP. 

Be sure to follow the Wiki:
https://github.com/openpnp/openpnp/wiki/Getting-Started-with-Eclipse#translations-in-eclipse

## Standalone i18n Editor
Alternatively, if you don't want to use Eclipse, there is a standalone editor for existing entries:

https://github.com/VSSavin/i18n-editor can be used to edit the translations files.

To import the OpenPnP files:

1. Download and run the tool.
1. Drag the folder openpnp/src/main/resources/org/openpnp into the tool's main window.
1. Set the encoding to UTF-8 in the Settings menu.
1. Click keys to edit their text.
1. Add a new language by selecting Edit -> Add Locale. See
   https://www.oracle.com/technetwork/java/javase/java8locales-2095355.html for a list of
   language codes.

Note: There is an issue where a parent of a nested key is not editable in the tool

OpenPnP will detect your language automatically and use the correct language file. If you want
to change the language you can select "View->Language" from the top menu.
