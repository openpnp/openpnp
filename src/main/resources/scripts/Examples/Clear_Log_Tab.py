import os
import inspect
import traceback
import time

import org.openpnp.gui
import javax.swing

# Helper function ... 
# Returns 1st found component  or  None
def findComponentByType(parent, comp_type):
  comp_array=parent.getComponents()
  for ii in range(len(comp_array)):
    if type(comp_array[ii])==comp_type:
      retval=comp_array[ii]
      return retval
  return None

# Remove OLD content of the Log-TAB (like pressing "clear"-Button)
def clearLogPanel():
  try:
    comp=findComponentByType(gui.tabs, org.openpnp.gui.LogPanel)
    comp=findComponentByType(comp, javax.swing.JScrollPane)
    comp.getViewport().getView().setText("")
  except:
    print traceback.format_exc()





# Clear the screen
clearLogPanel()

# Print some information (incl. time stamp) afterwards
src_file=inspect.getfile(inspect.currentframe())
src_dir=os.path.dirname(os.path.abspath(src_file))
print src_dir,
print src_file,
print time.asctime(),
