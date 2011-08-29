package org.openpnp.gui.components;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 *  Class that allows you to add a JSeparator to the ComboBoxModel.
 *
 *  The separator is rendered as a horizontal line. Using the Up/Down arrow
 *  keys will cause the combo box selection to skip over the separator.
 *  If you attempt to select the separator with the mouse, the selection
 *  will be ignored and the drop down will remain open.
 */
public class SeparatorComboBox extends JComboBox implements KeyListener
{
	//  Track key presses and releases

	private boolean released = true;

	//  Track when the separator has been selected
	private boolean separatorSelected = false;

	/**
	 *  Standard constructor. See JComboBox API for details
	 */
	public SeparatorComboBox()
	{
		super();
		init();
	}

	/**
	 *  Standard constructor. See JComboBox API for details
	 */
	public SeparatorComboBox(ComboBoxModel model)
	{
		super(model);
		init();
	}

	/**
	 *  Standard constructor. See JComboBox API for details
	 */
	public SeparatorComboBox(Object[] items)
	{
		super(items);
		init();
	}

	/**
	 *  Standard constructor. See JComboBox API for details
	 */
	public SeparatorComboBox(Vector<?> items)
	{
		super(items);
		init();
	}

	private void init()
	{
		setRenderer( new SeparatorRenderer() );
		addKeyListener(this);
	}

	/**
	 *	Prevent selection of the separator by keyboard or mouse
	 */
	@Override
	public void setSelectedIndex(int index)
	{
		Object value = getItemAt(index);

		//  Attempting to select a separator

		if (value instanceof JSeparator)
		{
			//  If no keys have been pressed then we must be using the mouse.
			//  Prevent selection of the Separator when using the mouse

			if (released)
			{
				separatorSelected = true;
				return;
			}

			//  Skip over the Separator when using the Up/Down keys

			int current = getSelectedIndex();
			index += (index > current) ? 1 : -1;

			if (index == -1 || index >= dataModel.getSize())
				return;
		}

		super.setSelectedIndex(index);
	}

	/**
	 *  Prevent closing of the popup when attempting to select the
	 *  separator with the mouse.
	 */
	@Override
	public void setPopupVisible(boolean visible)
	{
		//  Keep the popup open when the separator was clicked on

		if (separatorSelected)
		{
			separatorSelected = false;
			return;
		}

		super.setPopupVisible(visible);
	}

//
//  Implement the KeyListener interface
//
	public void keyPressed(KeyEvent e)
	{
		released = false;
	}

	public void keyReleased(KeyEvent e)
	{
		released = true;
	}

	public void keyTyped(KeyEvent e) {}

	/**
	 *  Class to render the JSeparator compenent
	 */
	class SeparatorRenderer implements ListCellRenderer
	{
		private ListCellRenderer renderer;
		
		public SeparatorRenderer() {
			renderer = new JComboBox().getRenderer();
		}
		
		public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus)
		{
			if (value instanceof JSeparator) {
				return (JSeparator)value;
			}
			else {
				return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		}
	}
}
