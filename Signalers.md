# Sound Signaler
A sound signaler will signal certain states of the Machine or a JobProcessor. If e.g. a placement or pickup fails it will play an error sound. It can also play a sound when the Job is complete.

## Configuration
The SoundSignaler can be configured in the machine.xml

      <signalers>
         <signaler class="org.openpnp.machine.reference.signaler.SoundSignaler" id="b7980099-1992-42b4-b38a-2f8ee6b52eb8" name="Sound Signaler" enable-error-sound="true" enable-finished-sound="true"/>
      </signalers>

### Disabling Sounds
If you want to disable the sounds you can set the tho attributes on the the SoundSignaler to false:
`enable-error-sound="false"`
`enable-success-sound="false"`

Replacing the sounds with own sound files will be possible later on.