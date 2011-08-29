package org.openpnp.gui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JComponent;

@SuppressWarnings("serial")
public class BoardView extends JComponent {
	public BoardView() {
		setOpaque(true);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Insets ins = getInsets();
		int width = getWidth() - ins.left - ins.right;
		int height = getHeight() - ins.top - ins.bottom;
		g.setColor(Color.black);
		g.fillRect(ins.left, ins.top, width, height);
	}
}
