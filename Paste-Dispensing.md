Paste dispensing is a new feature in OpenPnP that is not yet complete. To enable it you must manually edit your [machine.xml](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located).

In your machine.xml file find the job processors section. It may look something like this:

```
      <pnp-job-processor class="org.openpnp.machine.reference.ReferencePnpJobProcessor" park-when-complete="false"/>
```

Add the following line so that the section now looks like:

```
      <pnp-job-processor class="org.openpnp.machine.reference.ReferencePnpJobProcessor" park-when-complete="false"/>
      <paste-dispense-job-processor class="org.openpnp.machine.reference.ReferencePasteDispenseJobProcessor" park-when-complete="false"/>
```

After making this change, when you start OpenPnP you will see a new tab in the Job panel called Solder Paste.
