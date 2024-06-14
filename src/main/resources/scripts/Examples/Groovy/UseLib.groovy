package scripts.Examples.Groovy

import examples.groovy.ExampleLib

def instance = new ExampleLib()

instance.append("hello")
instance.append("world")

instance.increment(2)

instance.set("greeting", "hello world")

instance.add("this")
instance.add("that")
instance.add("another")

String message = instance
assert(message == 'examples.groovy.ExampleLib(helloworld, 2, [this, that, another], [greeting:hello world])')
println(message)
