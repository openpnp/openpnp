# Get the nozzle
my_nozzle=machine.defaultHead.defaultNozzle

# Run load command for each nozzle tip
for nt in my_nozzle.getNozzleTips():
  print "###  %r  %s" % (nt.name, "#"*16),
  my_nozzle.loadNozzleTip(nt),
