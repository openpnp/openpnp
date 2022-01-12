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

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Length;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;
import org.openpnp.model.Solutions.Issue.ActionProperty;
import org.openpnp.model.Solutions.Issue.DoubleProperty;
import org.openpnp.model.Solutions.Issue.IntegerProperty;
import org.openpnp.model.Solutions.Issue.LengthProperty;
import org.openpnp.model.Solutions.Issue.MultiLineTextProperty;
import org.openpnp.model.Solutions.Issue.StringProperty;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

public class IssuePanel extends JPanel {
    private static final int SLIDER_MAX = 10000;
    private static final int MAX_MULTIPLE_CHOICE = 10;
    final Solutions.Issue issue;
    final ReferenceMachine machine;
    private JScrollPane scrollPane;
    private JPanel panel;
    private JLabel lblSubject;
    private JLabel subjectText;
    private JLabel lblIssue;
    private JLabel lblSolution;
    private JTextArea issueText;
    private JTextArea solutionText;
    private JPanel panel_1;
    private JPanel panel_2;
    private JPanel panel_3;
    private final ButtonGroup buttonGroup = new ButtonGroup();

    public IssuePanel(Issue issue, ReferenceMachine machine) {
        super();
        this.issue = issue;
        this.machine = machine;

        // Calculate the needed rows from the issue properties
        final int rowsFixed = 4;
        int rowCount = getDynamicRows(rowsFixed);

        setBorder(null);
        setLayout(new BorderLayout(0, 0));

        scrollPane = new JScrollPane();
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        panel = new JPanel();
        scrollPane.setViewportView(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("70dlu", true), Sizes.constant("150dlu", true)), 1),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,},
                issue == null ? new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC, 
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, 
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, 
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, 
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, 
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, 
                        FormSpecs.DEFAULT_ROWSPEC,
        }
        : dynamicRowspec(rowCount)));

        lblSubject = new JLabel("Subject");
        panel.add(lblSubject, "2, 2, right, center");

        panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.add(panel_1, "4, 2, fill, fill");
        panel_1.setLayout(new FormLayout(new ColumnSpec[] {
                new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("70dlu", true), Sizes.constant("150dlu", true)), 1),},
                new RowSpec[] {
                        FormSpecs.DEFAULT_ROWSPEC,}));

        subjectText = new JLabel(" ");
        panel_1.add(subjectText, "1, 1, fill, default");
        subjectText.setBackground(lblSubject.getBackground());

        lblIssue = new JLabel("Issue");
        panel.add(lblIssue, "2, 4, right, center");

        panel_2 = new JPanel();
        panel_2.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.add(panel_2, "4, 4, fill, fill");
        panel_2.setLayout(new FormLayout(new ColumnSpec[] {
                new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("70dlu", true), Sizes.constant("150dlu", true)), 1),},
                new RowSpec[] {
                        FormSpecs.DEFAULT_ROWSPEC,}));

        issueText = new JTextArea(" ");
        panel_2.add(issueText, "1, 1");
        issueText.setBackground(lblSubject.getBackground());
        issueText.setEditable(false);
        issueText.setFont(UIManager.getFont("Label.font"));      
        issueText.setWrapStyleWord(true);  
        issueText.setLineWrap(true);

        lblSolution = new JLabel("Solution");
        panel.add(lblSolution, "2, 6, right, center");

        panel_3 = new JPanel();
        panel_3.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.add(panel_3, "4, 6, fill, fill");
        panel_3.setLayout(new FormLayout(new ColumnSpec[] {
                new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("70dlu", true), Sizes.constant("150dlu", true)), 1),},
                new RowSpec[] {
                        FormSpecs.DEFAULT_ROWSPEC,}));

        solutionText = new JTextArea(" ");
        panel_3.add(solutionText, "1, 1");
        solutionText.setBackground(lblSubject.getBackground());
        solutionText.setEditable(false);
        solutionText.setFont(UIManager.getFont("Label.font"));
        solutionText.setWrapStyleWord(true);  
        solutionText.setLineWrap(true);

        buildDynamicPart(rowsFixed);

        initDataBindings();
    }

    public int getDynamicRows(int formRow) {
        formRow += (issue.getExtendedDescription() == null) ? 0 : 1; 
        formRow += issue.getProperties().length;
        formRow += issue.getChoices().length;
        return formRow;
    }

    private void buildDynamicPart(int formRow) {
        if (issue.getExtendedDescription() != null) {
            JPanel panelControl = new JPanel();
            panelControl.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
            panel.add(panelControl, "4, "+(formRow*2)+", fill, fill");
            panelControl.setLayout(new FormLayout(new ColumnSpec[] {
                    new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("70dlu", true), Sizes.constant("150dlu", true)), 1),},
                    new RowSpec[] {
                            FormSpecs.DEFAULT_ROWSPEC,}));

            JLabel lbl = new JLabel(issue.getExtendedDescription());
            setClipboardHandler(lbl);
            panelControl.add(lbl, "1, 1");
            lbl.setIcon(issue.getExtendedIcon());
            lbl.setIconTextGap(20);

            // Consume the row
            formRow++;
        }
        for (Solutions.Issue.CustomProperty property : issue.getProperties()) {
            if (property instanceof StringProperty) {
                StringProperty stringProperty = (StringProperty) property;
                JLabel lbl = new JLabel(property.getLabel());
                lbl.setToolTipText(property.getToolTip());
                panel.add(lbl, "2, "+(formRow*2)+", right, default");
                if (property instanceof MultiLineTextProperty) {
                    JPanel subPanel = new JPanel();
                    subPanel.setLayout(new BorderLayout());
                    JScrollPane scrollPane = new JScrollPane();
                    subPanel.add(scrollPane);
                    JTextArea textField = new JTextArea();
                    textField.setRows(4);
                    scrollPane.setViewportView(textField);
                    textField.getDocument().addDocumentListener(new DocumentListener() {
                        public void changedUpdate(DocumentEvent e) {
                            stringProperty.set(textField.getText());
                        }
                        public void removeUpdate(DocumentEvent e) {
                            stringProperty.set(textField.getText());
                        }
                        public void insertUpdate(DocumentEvent e) {
                            stringProperty.set(textField.getText());
                        }
                    });
                    String val = stringProperty.get();
                    textField.setText(val);
                    textField.setToolTipText(property.getToolTip());
                    textField.setEnabled(issue.getState() == Solutions.State.Open);
                    panel.add(subPanel, "4, "+(formRow*2)+",fill, fill");
                    if (stringProperty.getSuggestions() != null) {
//                        lbl = new JLabel("Suggestions");
//                        lbl.setToolTipText(property.getToolTip());
//                        panel.add(lbl, "2, "+(formRow*2)+", right, default");
                        JPanel suggestPanel = new JPanel();
                        suggestPanel.setLayout(new BorderLayout());
                        JLabel lbl2 = new JLabel("Templates: ");
                        lbl2.setToolTipText(stringProperty.getSuggestionToolTip());
                        suggestPanel.add(lbl2, BorderLayout.WEST);
                        JComboBox comboBox = new JComboBox(stringProperty.getSuggestions());
                        comboBox.setMaximumRowCount(20);
                        setAutoSuggestions(comboBox, stringProperty);
                        comboBox.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                textField.setText((String) comboBox.getSelectedItem());
                            }
                        });
                        comboBox.setToolTipText(stringProperty.getSuggestionToolTip());
                        comboBox.setEnabled(issue.getState() == Solutions.State.Open);
                        suggestPanel.add(comboBox);
                        subPanel.add(suggestPanel, BorderLayout.NORTH);
                    }
                }
                else if (stringProperty.getSuggestions() != null) {
                    JComboBox comboBox = new JComboBox(stringProperty.getSuggestions());
                    comboBox.setEditable(true);
                    comboBox.setMaximumRowCount(20);
                    setAutoSuggestions(comboBox, stringProperty);
                    comboBox.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            stringProperty.set((String) comboBox.getSelectedItem());
                        }
                    });
                    String val = stringProperty.get();
                    comboBox.setSelectedItem(val);
                    comboBox.setToolTipText(stringProperty.getSuggestionToolTip());
                    comboBox.setEnabled(issue.getState() == Solutions.State.Open);
                    panel.add(comboBox, "4, "+(formRow*2)+", left, default");
                }
                else {
                    JTextField textField = new JTextField();
                    textField.getDocument().addDocumentListener(new DocumentListener() {
                        public void changedUpdate(DocumentEvent e) {
                            stringProperty.set(textField.getText());
                        }
                        public void removeUpdate(DocumentEvent e) {
                            stringProperty.set(textField.getText());
                        }
                        public void insertUpdate(DocumentEvent e) {
                            stringProperty.set(textField.getText());
                        }
                    });
                    String val = stringProperty.get();
                    textField.setText(val);
                    textField.setToolTipText(property.getToolTip());
                    textField.setEnabled(issue.getState() == Solutions.State.Open);
                    panel.add(textField, "4, "+(formRow*2)+", left, default");
                }
            }
            else if (property instanceof IntegerProperty) {
                IntegerProperty intProperty = (IntegerProperty) property;
                JLabel lbl = new JLabel(property.getLabel());
                lbl.setToolTipText(property.getToolTip());
                panel.add(lbl, "2, "+(formRow*2)+", right, default");
                JSpinner spinner = new JSpinner();
                spinner.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        int value = (int) spinner.getValue();
                        intProperty.set(value);
                        int newValue = intProperty.get();
                        if (newValue != value) {
                            spinner.setValue(newValue);
                        }
                    }
                });
                int val = intProperty.get();
                spinner.setModel(new SpinnerNumberModel(val, (int)Math.min(val, intProperty.getMin()), (int)Math.max(val, intProperty.getMax()), 1));
                spinner.setToolTipText(property.getToolTip());
                spinner.setEnabled(issue.getState() == Solutions.State.Open);
                panel.add(spinner, "4, "+(formRow*2)+", left, default");
            }
            else if (property instanceof DoubleProperty) {
                DoubleProperty doubleProperty = (DoubleProperty) property;
                JLabel lbl = new JLabel(property.getLabel());
                lbl.setToolTipText(property.getToolTip());
                panel.add(lbl, "2, "+(formRow*2)+", right, default");
                JSlider slider = new JSlider(JSlider.HORIZONTAL,
                        0, SLIDER_MAX, getSliderValue(doubleProperty));
                slider.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        int value = (int) slider.getValue();
                        doubleProperty.set(doubleProperty.getMin() + value*(doubleProperty.getMax() - doubleProperty.getMin())/SLIDER_MAX);
                        int newValue = getSliderValue(doubleProperty);
                        if (newValue != value) {
                            slider.setValue(newValue);
                        }
                    }
                });
                double val = doubleProperty.get();
                slider.setToolTipText(property.getToolTip());
                slider.setEnabled(issue.getState() == Solutions.State.Open);
                panel.add(slider, "4, "+(formRow*2)+", left, default");
            }
            else if (property instanceof LengthProperty) {
                LengthProperty lengthProperty = (LengthProperty) property;
                JLabel lbl = new JLabel(lengthProperty.getLabel());
                lbl.setToolTipText(lengthProperty.getToolTip());
                panel.add(lbl, "2, "+(formRow*2)+", right, default");
                JTextField textField = new JTextField();
                textField.setToolTipText(lengthProperty.getToolTip());
                textField.setEnabled(issue.getState() == Solutions.State.Open);
                textField.setColumns(10);
                LengthConverter lengthConverter = new LengthConverter();
                textField.setText(lengthConverter.convertForward(lengthProperty.get()));
                textField.getDocument().addDocumentListener(new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        changedText();
                    }
                    public void removeUpdate(DocumentEvent e) {
                        changedText();
                    }
                    public void insertUpdate(DocumentEvent e) {
                        changedText();
                    }

                    public void changedText() {
                        String text = textField.getText();
                        Length length = lengthConverter.convertReverse(text);
                        lengthProperty.set(length);
                    }
                });
                panel.add(textField, "4, "+(formRow*2)+", left, default");
            }
            else if (property instanceof ActionProperty) {
                ActionProperty lengthProperty = (ActionProperty) property;
                JLabel lbl = new JLabel(lengthProperty.getLabel());
                lbl.setToolTipText(lengthProperty.getToolTip());
                panel.add(lbl, "2, "+(formRow*2)+", right, default");
                JButton button = new JButton(lengthProperty.get());
                button.setToolTipText(lengthProperty.getToolTip());
                button.setEnabled(issue.getState() == Solutions.State.Open);
                panel.add(button, "4, "+(formRow*2)+", left, default");
            }
            // Consume the row
            formRow++;
        }
        for (Solutions.Issue.Choice choice : issue.getChoices()) {
            if (choice != null) {
                if (issue.getChoice() == null) {
                    // Set first choice as default.
                    issue.setChoice(choice.getValue());
                }
                final JRadioButton radioButton = new JRadioButton("");
                buttonGroup.add(radioButton);
                panel.add(radioButton, "2, "+(formRow*2)+", right, default");
                radioButton.setSelected(issue.getChoice() == choice.getValue());
                radioButton.setEnabled(issue.getState() == Solutions.State.Open);
                radioButton.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        if (radioButton.isSelected()) {
                            issue.setChoice(choice.getValue());
                        }
                    }
                });

                JPanel panelMultiChoice = new JPanel();
                panelMultiChoice.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
                panel.add(panelMultiChoice, "4, "+(formRow*2)+", fill, fill");
                panelMultiChoice.setLayout(new FormLayout(new ColumnSpec[] {
                        new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("70dlu", true), Sizes.constant("150dlu", true)), 1),},
                        new RowSpec[] {
                                FormSpecs.DEFAULT_ROWSPEC,}));

                JLabel lblMultiChoice = new JLabel(choice.getDescription());
                panelMultiChoice.add(lblMultiChoice, "1, 1");
                lblMultiChoice.setIcon(choice.getIcon());
                lblMultiChoice.setIconTextGap(20);
                setClipboardHandler(lblMultiChoice);
                lblMultiChoice.addMouseListener(new MouseListener() {
                    private boolean beginClick;

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (beginClick && SwingUtilities.isLeftMouseButton(e)) {
                            radioButton.setSelected(true);
                        }
                        beginClick = false;
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            beginClick = true;
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        beginClick = false;
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            radioButton.setSelected(true);
                        }
                    }
                });
                // Consume the row
                formRow++;
            }
        }
    }

    protected void setAutoSuggestions(JComboBox comboBox, StringProperty stringProperty) {
        comboBox.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                comboBox.setModel(new DefaultComboBoxModel<>(stringProperty.getSuggestions()));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
            
        });
    }

    public void setClipboardHandler(JLabel lbl) {
        lbl.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Crude
                    String text = lbl.getText();
                    String noHTMLString = text
                            .replaceAll("\\<br.*?\\>", "\n")
                            .replaceAll("\\</p\\>", "\n")
                            .replaceAll("\\</h.*?\\>", "\n")
                            .replaceAll("\\</li\\>", "\n")
                            .replaceAll("\\</tr\\>", "\n")
                            .replaceAll("\\<.*?\\>", " ")
                            .replaceAll("  ", " ")
                            .replaceAll("  ", " ")
                            .replaceAll("\n ", "\n")
                            .trim();
                    Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(noHTMLString), null);
                    lbl.setEnabled(!lbl.isEnabled());
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Thread.sleep(300);
                        }
                        catch (InterruptedException e1) {}
                        lbl.setEnabled(!lbl.isEnabled());
                    });
                }
            }
        });
    }

    public int getSliderValue(DoubleProperty doubleProperty) {
        return (int) Math.round((doubleProperty.get() - doubleProperty.getMin())*SLIDER_MAX/(doubleProperty.getMax() - doubleProperty.getMin()));
    }

    private RowSpec[] dynamicRowspec(int rows) {
        RowSpec[] rowspec = new RowSpec[rows*2];
        for (int i = 0; i < rows*2; i+=2) {
            rowspec[i] = FormSpecs.RELATED_GAP_ROWSPEC;
            rowspec[i+1] = FormSpecs.DEFAULT_ROWSPEC;
        }
        return rowspec;
    }

    protected void initDataBindings() {
        // For some strange reason this does not work:
        //      BeanProperty<Issue, String> issueBeanProperty = BeanProperty.create("subject.subjectText");
        //      BeanProperty<JTextArea, String> jTextAreaBeanProperty = BeanProperty.create("text");
        //      AutoBinding<Issue, String, JTextArea, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, issue, issueBeanProperty, subjectText, jTextAreaBeanProperty);
        //      autoBinding.bind();
        // So bind it statically, it is final anyway. 
        subjectText.setText(issue.getSubject().getSubjectText());
        subjectText.setIcon(issue.getSubject().getSubjectIcon());

        BeanProperty<Issue, String> issueBeanProperty_1 = BeanProperty.create("issue");
        BeanProperty<JTextArea, String> jLabelBeanProperty = BeanProperty.create("text");
        AutoBinding<Issue, String, JTextArea, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, issue, issueBeanProperty_1, issueText, jLabelBeanProperty);
        autoBinding_1.bind();
        //
        BeanProperty<Issue, String> issueBeanProperty_2 = BeanProperty.create("solution");
        AutoBinding<Issue, String, JTextArea, String> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, issue, issueBeanProperty_2, solutionText, jLabelBeanProperty);
        autoBinding_2.bind();

        UiUtils.messageBoxOnExceptionLater(() -> {
            issue.activate();
        });
    }
}
