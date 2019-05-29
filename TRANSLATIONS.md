https://github.com/jcbvm/i18n-editor can be used to edit the translations files.

To import the OpenPnP files:

1. Download and run the tool.
2. Drag the folder openpnp/src/main/resources/org/openpnp into the tool's main window.
3. Click keys to edit their text.
4. Add a new language by selecting Edit -> Add Locale. See
   https://www.oracle.com/technetwork/java/javase/java8locales-2095355.html for a list of
   language codes.

Note: There is an issue where a parent of a nested key is not editable in the tool
   
OpenPnP will detect your language automatically and use the correct language file. If you want
to change the language you can select "View->Language" from the top menu.

TODO: Decide on Menu.File or Menu.File.Title. Menu.File is not editable in i18n editor.
