from __future__ import print_function

# Get the nozzle
my_nozzle=machine.defaultHead.defaultNozzle

# Run print information regarding nozzle
print("###  Name &  Location of %r  %s" % (my_nozzle.name, "#"*16), end=' ')
print(my_nozzle, end=' ')
print(my_nozzle.name, end=' ')
print(my_nozzle.location)
