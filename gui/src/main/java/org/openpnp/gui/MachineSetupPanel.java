/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.prefs.Preferences;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.model.Configuration;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.PropertySheetHolder.PropertySheet;
import org.openpnp.spi.WizardConfigurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JTabbedPane;

@SuppressWarnings("serial")
public class MachineSetupPanel extends JPanel implements WizardContainer {
	private final static Logger logger = LoggerFactory
			.getLogger(MachineSetupPanel.class);

	private static final String PREF_DIVIDER_POSITION = "MachineSetupPanel.dividerPosition";
	private static final int PREF_DIVIDER_POSITION_DEF = -1;

	private JTextField searchTextField;

	private Preferences prefs = Preferences
			.userNodeForPackage(MachineSetupPanel.class);
	private JTree tree;
	private JTabbedPane tabbedPane;

	public MachineSetupPanel() {
		setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.EAST);

		JLabel lblSearch = new JLabel("Search");
		panel_1.add(lblSearch);

		searchTextField = new JTextField();
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                search();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                search();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                search();
            }
        });
		panel_1.add(searchTextField);
		searchTextField.setColumns(15);

		final JSplitPane splitPane = new JSplitPane();
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION,
				PREF_DIVIDER_POSITION_DEF));
		splitPane.addPropertyChangeListener("dividerLocation",
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						prefs.putInt(PREF_DIVIDER_POSITION,
								splitPane.getDividerLocation());
					}
				});
		add(splitPane, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane();
        splitPane.setLeftComponent(scrollPane);
        
        tree = new JTree();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        scrollPane.setViewportView(tree);
        
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        splitPane.setRightComponent(tabbedPane);
        
        tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                PropertySheetConfigurableTreeNode node = (PropertySheetConfigurableTreeNode) tree.getLastSelectedPathComponent();

                tabbedPane.removeAll();
                
                if (node != null) {
                    PropertySheet[] propertySheets = node.obj.getPropertySheets();
                    if (propertySheets != null) {
                        for (PropertySheet propertySheet : propertySheets) {
                            String title = propertySheet.getPropertySheetTitle();
                            JPanel panel = propertySheet.getPropertySheetPanel();
                            if (title == null) {
                                title = "Untitled";
                            }
                            if (panel != null) {
                                tabbedPane.add(title, panel);
                            }
                        }
                    }
                }
                
                revalidate();
                repaint();
            }
        });
        
        Configuration.get().addListener(new ConfigurationListener() {
            @Override
            public void configurationLoaded(Configuration configuration)
                    throws Exception {
            }
            
            @Override
            public void configurationComplete(Configuration configuration)
                    throws Exception {
                tree.setModel(new DefaultTreeModel(new PropertySheetConfigurableTreeNode(Configuration.get().getMachine(), null)));
                for (int i = 0; i < tree.getRowCount(); i++) {
                    tree.expandRow(i);
                }
            }
        });
	}

	private void search() {
	}

	@Override
	public void wizardCompleted(Wizard wizard) {
		Configuration.get().setDirty(true);
	}

	@Override
	public void wizardCancelled(Wizard wizard) {
	}
	
    public class PropertySheetConfigurableTreeNode implements TreeNode {
        private final PropertySheetHolder obj;
        private final TreeNode parent;
        private final ArrayList<PropertySheetConfigurableTreeNode> children = new ArrayList<PropertySheetConfigurableTreeNode>();
        
        public PropertySheetConfigurableTreeNode(PropertySheetHolder obj, TreeNode parent) {
            this.obj = obj;
            this.parent = parent;
            PropertySheetHolder[] children = obj.getChildPropertySheetHolders();
            if (children != null) {
                for (PropertySheetHolder child : children) {
                    this.children.add(new PropertySheetConfigurableTreeNode(child, this));
                }
            }
        }
        
        @Override
        public TreeNode getChildAt(int childIndex) {
            return children.get(childIndex);
        }

        @Override
        public int getChildCount() {
            return children.size();
        }

        @Override
        public TreeNode getParent() {
            return parent;
        }

        @Override
        public int getIndex(TreeNode node) {
            return children.indexOf(node);
        }

        @Override
        public boolean getAllowsChildren() {
            return children.size() > 0;
        }

        @Override
        public boolean isLeaf() {
            return children.size() < 1;
        }

        @Override
        public Enumeration children() {
            return Collections.enumeration(children);
        }

        @Override
        public String toString() {
            return obj.getPropertySheetHolderTitle();
        }
    }	
}