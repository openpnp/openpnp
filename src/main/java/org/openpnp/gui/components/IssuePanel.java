package org.openpnp.gui.components;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;
import org.openpnp.model.Solutions.Issue.DoubleProperty;
import org.openpnp.model.Solutions.Issue.IntegerProperty;
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
            JPanel panelMultiChoice = new JPanel();
            panelMultiChoice.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
            panel.add(panelMultiChoice, "4, "+(formRow*2)+", fill, fill");
            panelMultiChoice.setLayout(new FormLayout(new ColumnSpec[] {
                    new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("70dlu", true), Sizes.constant("150dlu", true)), 1),},
                    new RowSpec[] {
                            FormSpecs.DEFAULT_ROWSPEC,}));

            JLabel lbl = new JLabel(issue.getExtendedDescription());
            panelMultiChoice.add(lbl, "1, 1");
            lbl.setIcon(issue.getExtendedIcon());
            lbl.setIconTextGap(20);
            
            // Consume the row
            formRow++;
        }
        for (Solutions.Issue.CustomProperty property : issue.getProperties()) {
            if (property instanceof IntegerProperty) {
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
                lblMultiChoice.addMouseListener(new MouseListener() {
                    private boolean beginClick;

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (beginClick) {
                            radioButton.setSelected(true);
                        }
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        beginClick = true;
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
                        radioButton.setSelected(true);
                    }
                });
                // Consume the row
                formRow++;
            }
        }
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
