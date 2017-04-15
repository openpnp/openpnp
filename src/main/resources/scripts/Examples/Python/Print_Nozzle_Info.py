# Get the nozzle
my_nozzle=machine.defaultHead.defaultNozzle

# Run print information regarding nozzle
print "###  Name &  Location of %r  %s" % (my_nozzle.name, "#"*16),
print my_nozzle,
print my_nozzle.name,
print my_nozzle.location
