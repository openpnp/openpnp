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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.CameraPanel;
import org.openpnp.gui.components.MachineControlsPanel;

@SuppressWarnings("serial")
public abstract class MainFrameUi extends JFrame {
	/*
	 * TODO define accelerators and mnemonics
	 * openJobMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
	 * Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	 */
	protected JPanel contentPane;
	protected MachineControlsPanel machineControlsPanel;
	protected CameraPanel cameraPanel;
	protected JTable boardsTable;
	protected JTable partsTable;
	protected JLabel lblStatus;
	protected JPanel panelBottom;
	protected CardLayout panelBottomCardLayout;

	public MainFrameUi() {
		createUi();
	}
	
	protected abstract void openJob();

	protected abstract void startPauseResumeJob();
	
	protected abstract void stepJob();

	protected abstract void stopJob();
	
	protected abstract void orientBoard();
	
	private void createUi() {
		setBounds(100, 100, 1280, 1024);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.setAction(newJobAction);
		mnFile.add(mntmNew);

		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.setAction(openJobAction);
		mnFile.add(mntmOpen);

		mnFile.addSeparator();

		JMenuItem mntmClose = new JMenuItem("Close");
		mntmClose.setAction(closeJobAction);
		mnFile.add(mntmClose);

		mnFile.addSeparator();

		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.setAction(saveJobAction);
		mnFile.add(mntmSave);

		JMenuItem mntmSaveAs = new JMenuItem("Save As");
		mntmSaveAs.setAction(saveJobAsAction);
		mnFile.add(mntmSaveAs);

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

		JMenuItem mntmAddBoard = new JMenuItem("Add Board");
		mntmAddBoard.setAction(addBoardAction);
		mnEdit.add(mntmAddBoard);

		JMenuItem mntmDeleteBoard = new JMenuItem("Delete Board");
		mntmDeleteBoard.setAction(deleteBoardAction);
		mnEdit.add(mntmDeleteBoard);

		JMenuItem mntmEnableBoard = new JMenuItem("Enable Board");
		mntmEnableBoard.setAction(enableDisableBoardAction);
		mnEdit.add(mntmEnableBoard);

		mnEdit.addSeparator();

		JMenuItem mntmMoveBoardUp = new JMenuItem("Move Board Up");
		mntmMoveBoardUp.setAction(moveBoardUpAction);
		mnEdit.add(mntmMoveBoardUp);

		JMenuItem mntmMoveBoardDown = new JMenuItem("Move Board Down");
		mntmMoveBoardDown.setAction(moveBoardDownAction);
		mnEdit.add(mntmMoveBoardDown);

		mnEdit.addSeparator();

		JMenuItem mntmSetBoardLocation = new JMenuItem("Set Board Location");
		mntmSetBoardLocation.setAction(orientBoardAction);
		mnEdit.add(mntmSetBoardLocation);

		JMenu mnJob = new JMenu("Job Control");
		menuBar.add(mnJob);

		JMenuItem mntmNewMenuItem = new JMenuItem("Start Job");
		mntmNewMenuItem.setAction(startPauseResumeJobAction);
		mnJob.add(mntmNewMenuItem);

		JMenuItem mntmStepJob = new JMenuItem("Step Job");
		mntmStepJob.setAction(stepJobAction);
		mnJob.add(mntmStepJob);

		JMenuItem mntmStopJob = new JMenuItem("Stop Job");
		mntmStopJob.setAction(stopJobAction);
		mnJob.add(mntmStopJob);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JSplitPane splitPaneTopBottom = new JSplitPane();
		splitPaneTopBottom.setBorder(null);
		splitPaneTopBottom.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPaneTopBottom.setContinuousLayout(true);
		contentPane.add(splitPaneTopBottom, BorderLayout.CENTER);
		
		JPanel panelTop = new JPanel();
		splitPaneTopBottom.setLeftComponent(panelTop);
		panelTop.setLayout(new BorderLayout(0, 0));
		
				JPanel panelLeftColumn = new JPanel();
				panelTop.add(panelLeftColumn, BorderLayout.WEST);
				FlowLayout flowLayout = (FlowLayout) panelLeftColumn.getLayout();
				flowLayout.setVgap(0);
				flowLayout.setHgap(0);
				
						JPanel panel = new JPanel();
						panelLeftColumn.add(panel);
						panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
						
								machineControlsPanel = new MachineControlsPanel();
								machineControlsPanel.setBorder(new TitledBorder(null,
										"Machine Controls", TitledBorder.LEADING, TitledBorder.TOP,
										null, null));
								panel.add(machineControlsPanel);
								
										cameraPanel = new CameraPanel();
										panelTop.add(cameraPanel, BorderLayout.CENTER);
										cameraPanel.setBorder(new TitledBorder(null, "Cameras",
												TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		panelBottom = new JPanel();
		splitPaneTopBottom.setRightComponent(panelBottom);
		panelBottom.setLayout(panelBottomCardLayout = new CardLayout(0, 0));

		JPanel panelJob = new JPanel();
		panelBottom.add(panelJob, "Job");
		panelJob.setBorder(new TitledBorder(null, "Job", TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		panelJob.setLayout(new BorderLayout(0, 0));

		partsTable = new JTable();
		JScrollPane partsTableScroller = new JScrollPane(partsTable);

		boardsTable = new JTable();
		JScrollPane boardsTableScroller = new JScrollPane(boardsTable);
		boardsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		

		JPanel panelRight = new JPanel();
		panelRight.setLayout(new BorderLayout());
		panelRight.add(partsTableScroller);

		JPanel panelLeft = new JPanel();
		panelLeft.setLayout(new BorderLayout());
		
		JPanel panelJobControl = new JPanel();
		panelLeft.add(panelJobControl, BorderLayout.WEST);
		panelJobControl.setLayout(new BoxLayout(panelJobControl, BoxLayout.Y_AXIS));
		
		JButton btnStart = new JButton("Start");
		btnStart.setAction(startPauseResumeJobAction);
		btnStart.setPreferredSize(new Dimension(80, 80));
		btnStart.setFocusable(false);
		panelJobControl.add(btnStart);
		
		JButton btnStep = new JButton(stepJobAction);
		btnStep.setPreferredSize(new Dimension(80, 80));
		btnStep.setFocusable(false);
		panelJobControl.add(btnStep);
		
		JButton btnStop = new JButton("Stop");
		btnStop.setAction(stopJobAction);
		btnStop.setPreferredSize(new Dimension(80, 80));
		btnStop.setFocusable(false);
		panelJobControl.add(btnStop);
		
		Component glue = Box.createGlue();
		panelJobControl.add(glue);
		panelLeft.add(boardsTableScroller);

		JSplitPane splitPaneLeftRight = new JSplitPane();
		splitPaneLeftRight.setBorder(null);
		panelJob.add(splitPaneLeftRight);
		splitPaneLeftRight.setContinuousLayout(true);
		splitPaneLeftRight.setDividerLocation(350);
		splitPaneLeftRight.setLeftComponent(panelLeft);
		splitPaneLeftRight.setRightComponent(panelRight);
		splitPaneTopBottom.setDividerLocation(700);
		
		lblStatus = new JLabel(" ");
		lblStatus.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		contentPane.add(lblStatus, BorderLayout.SOUTH);
	}
	
	protected Action stopJobAction = new AbstractAction("Stop") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			stopJob();
		}
	};

	protected Action startPauseResumeJobAction = new AbstractAction("Start") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			startPauseResumeJob();
		}
	};

	protected Action stepJobAction = new AbstractAction("Step") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			stepJob();
		}
	};

	protected Action openJobAction = new AbstractAction("Open Job...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			openJob();
		}
	};

	protected Action closeJobAction = new AbstractAction("Close Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	protected Action newJobAction = new AbstractAction("New Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	protected Action saveJobAction = new AbstractAction("Save Job") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	protected Action saveJobAsAction = new AbstractAction("Save Job As...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	protected Action addBoardAction = new AbstractAction("Add Board...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	protected Action moveBoardUpAction = new AbstractAction("Move Board Up") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	protected Action moveBoardDownAction = new AbstractAction("Move Board Down") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	protected Action orientBoardAction = new AbstractAction("Set Board Location") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			orientBoard();
		}
	};

	protected Action deleteBoardAction = new AbstractAction("Delete Board") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};

	protected Action enableDisableBoardAction = new AbstractAction("Enable Board") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
		}
	};
}
