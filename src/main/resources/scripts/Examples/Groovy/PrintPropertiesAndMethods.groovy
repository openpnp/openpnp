package scripts.Examples.Groovy

import org.openpnp.spi.Machine

DEFAULT_CLIP_MODE = ClipMode::clipped(120)

// Prints all methods & variables and values of an element.
def print_properties(def element, ClipMode clip) {
    element.properties.each({ key, value ->
        String formattedString = "${element.name}.${key} = ${value}"
        printWithClip(formattedString, clip)
    })
}

def print_methods(def element, ClipMode clip) {
    element.class.metaClass.methods.each {method ->
        printWithClip(method.toString(), clip)
    }
}

def object = ((Machine)machine).defaultHead.defaultNozzle
print_properties(object, DEFAULT_CLIP_MODE)
print_methods(object, DEFAULT_CLIP_MODE)

//
// formatting helper code
//

import groovy.transform.MapConstructor
import static Clip.*

private void printWithClip(String string, ClipMode clip) {
    String toPrint

    switch (clip.mode) {
        case Unclipped:
            toPrint = string
            break
        case Clipped:
            toPrint = string.take(clip.width)
            break
    }
    println(toPrint)
}

enum Clip {
    Clipped,
    Unclipped
}

@MapConstructor
class ClipMode {
    Clip mode
    int width

    static ClipMode clipped(int width) {
        new ClipMode([mode: Clipped, width: width])
    }

    static ClipMode unclipped() {
        new ClipMode([mode: Unclipped])
    }
}
