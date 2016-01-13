Paste dispensing is a new feature in OpenPnP that is not yet complete. To enable it you must manually edit your [machine.xml](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located).

In your machine.xml file find the `<job-processors>` section. It may look something like this:

```
      <job-processors class="java.util.HashMap">
         <job-processor type="PickAndPlace">
            <job-processor class="org.openpnp.machine.reference.ReferenceJobProcessor" demo-mode="false">
               <job-planner class="org.openpnp.planner.SimpleJobPlanner"/>
            </job-processor>
         </job-processor>
      </job-processors>
```

Add a new `<job-processor>` element so that the section now looks like:

```
      <job-processors class="java.util.HashMap">
         <job-processor type="PickAndPlace">
            <job-processor class="org.openpnp.machine.reference.ReferenceJobProcessor" demo-mode="false">
               <job-planner class="org.openpnp.planner.SimpleJobPlanner"/>
            </job-processor>
         </job-processor>
         <job-processor type="SolderPaste">
            <job-processor class="org.openpnp.machine.reference.ReferenceSolderPasteJobProcessor"/>
         </job-processor>
      </job-processors>
```
Note that nothing in the PickAndPlace section was changed, only the SolderPaste section was added.

After making this change, when you start OpenPnP you will see a new tab in the Job panel called Solder Paste.

