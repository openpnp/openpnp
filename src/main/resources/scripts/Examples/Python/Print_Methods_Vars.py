from __future__ import print_function
import sys

# Prints all methods & variables and values of defaultNozzle
def print_methods_vars(element, clip=True):
  print(" ", end=' ')
  for item in dir(element):
    try:
      s1="%s.%s = %s" % (element.name, item, eval("element.%s" % item))
      if clip: s1=s1[:78]
      print(s1, end=' ')
    except:
      print("WARNING ... for", item, end=' ')

print_methods_vars(machine.defaultHead.defaultNozzle)
print("")
