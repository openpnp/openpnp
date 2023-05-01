/*
 * Copyright (C) 2021 <mark@makr.zone>
 * inspired and based on work
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openpnp.util.UiUtils;
import org.openpnp.util.XmlSerialize;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

public class MarkupTextPane extends JScrollPane {
    private JPanel textSizer;
    private JLabel textPane;
    private URI uri;
    private String markupText;

    public MarkupTextPane() { 
        textSizer = new JPanel();
        textSizer.setLayout(new FormLayout(new ColumnSpec[] {
                new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("150dlu", true), Sizes.constant("200dlu", true)), 1),},
                new RowSpec[] {
                        FormSpecs.DEFAULT_ROWSPEC,}));
        textPane = new JLabel();
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (uri != null) {
                    // HACK: can't truly pinpoint hyperlinks in the text, so this is usually
                    // just a link to the original online version, where links can be followed up.
                    UiUtils.browseUri(uri.toString());
                }
            }
        });
        textSizer.add(textPane, "1, 1");
        setViewportView(textSizer);
        getVerticalScrollBar().setUnitIncrement(16);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public String getText() {
        return markupText;
    }

    public void setText(String text) {
        this.markupText = text;
        String html = XmlSerialize.convertMarkupToHtml(text);
        textPane.setText(html);
    }

    public String getPlainText() {
        return textPane.getText();
    }

    public void setPlainText(String text) {
        textPane.setText(text);
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
