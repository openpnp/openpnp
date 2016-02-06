A place to put community sourced package definitions. You can export a package definition from OpenPnP by clicking the "Copy" button in the Packages panel.

When adding a package here, please use the following format:

    ## SOT-23
    ```
    <package id="SOT-23">
       <outline units="Millimeters"/>
       <footprint units="Millimeters" body-width="2.9" body-height="1.3">
          <pad name="1" x="-1.0" y="-1.1" width="0.8" height="0.8" rotation="0.0" roundness="0.0"/>
          <pad name="2" x="1.0" y="-1.1" width="0.8" height="0.8" rotation="0.0" roundness="0.0"/>
          <pad name="3" x="0.0" y="1.1" width="0.8" height="0.8" rotation="0.0" roundness="0.0"/>
       </footprint>
    </package>
    ```

To use one of these packages in your system just copy the definition to your clipboard and hit the "Paste" button in the Packages panel.

# Packages

## R0805
```
<package id="R0805" description="R0805">
   <outline units="Millimeters"/>
   <footprint units="Millimeters" body-width="0.0" body-height="0.0">
      <pad name="1" x="-0.95" y="0.0" width="1.3" height="1.5" rotation="90.0" roundness="0.0"/>
      <pad name="2" x="0.95" y="0.0" width="1.3" height="1.5" rotation="90.0" roundness="0.0"/>
   </footprint>
</package>
```

## SOT-23
```
<package id="SOT-23">
   <outline units="Millimeters"/>
   <footprint units="Millimeters" body-width="2.9" body-height="1.3">
      <pad name="1" x="-1.0" y="-1.1" width="0.8" height="0.8" rotation="0.0" roundness="0.0"/>
      <pad name="2" x="1.0" y="-1.1" width="0.8" height="0.8" rotation="0.0" roundness="0.0"/>
      <pad name="3" x="0.0" y="1.1" width="0.8" height="0.8" rotation="0.0" roundness="0.0"/>
   </footprint>
</package>
```
