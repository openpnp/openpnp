# Get the nozzle
my_nozzle=machine.defaultHead.defaultNozzle

# Just unload the nozzle tip
print "###  Unload nozzle tip %r" % my_nozzle.name,
my_nozzle.unloadNozzleTip()
