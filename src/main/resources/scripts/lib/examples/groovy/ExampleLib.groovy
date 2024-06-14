// 'package' is relative to the 'lib' directory.
package examples.groovy

import groovy.transform.ToString

// just a simple example library/class to be used by the UseLib example.
@ToString
class ExampleLib {
    String string1 = ""
    int int1 = 0
    List<String> list1 = []
    Map<String, Object> map1 = [:]

    void increment(int amount) {
        int1 += amount
    }

    void append(String other) {
        string1 += other
    }

    void add(String other) {
        list1 << other
    }

    void set(String key, Object value) {
        map1[key] = value
    }
}


