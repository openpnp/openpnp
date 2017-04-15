import sys

# Prints all methods & variables and values of defaultNozzle
def print_methods_vars(element, clip=True):
  print " ",
  for item in dir(element):
    try:
      s1="%s.%s = %s" % (element.name, item, eval("element.%s" % item))
      if clip: s1=s1[:78]
      print s1,
    except:
      print "WARNING ... for", item,

print_methods_vars(machine.defaultHead.defaultNozzle)
