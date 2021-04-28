package org.openpnp.gui.components;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

public class IssuePanel extends JPanel {
    final Solutions.Issue issue;
    final ReferenceMachine machine;
    private JScrollPane scrollPane;
    private JPanel panel;
    private JLabel lblSubject;
    private JTextArea subjectText;
    private JLabel lblIssue;
    private JLabel lblSolution;
    private JTextArea issueText;
    private JTextArea solutionText;
    private JPanel panel_1;
    private JPanel panel_2;
    private JPanel panel_3;

    public IssuePanel(Issue issue, ReferenceMachine machine) {
        super();
        setBorder(null);
        this.issue = issue;
        this.machine = machine;
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
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),}));

        lblSubject = new JLabel("Subject");
        panel.add(lblSubject, "2, 2, right, center");

        panel_1 = new JPanel();
        panel_1.setBorder(UIManager.getBorder("TextField.border"));
        panel.add(panel_1, "4, 2, fill, fill");
        panel_1.setLayout(new FormLayout(new ColumnSpec[] {
                new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("70dlu", true), Sizes.constant("150dlu", true)), 1),},
                new RowSpec[] {
                        FormSpecs.DEFAULT_ROWSPEC,}));

        subjectText = new JTextArea(" ");
        panel_1.add(subjectText, "1, 1, fill, default");
        subjectText.setBackground(lblSubject.getBackground());
        subjectText.setEditable(false);
        subjectText.setFont(UIManager.getFont("Label.font"));      
        subjectText.setWrapStyleWord(true);  
        subjectText.setLineWrap(true);

        lblIssue = new JLabel("Issue");
        panel.add(lblIssue, "2, 4, right, center");

        panel_2 = new JPanel();
        panel_2.setBorder(UIManager.getBorder("TextField.border"));
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
        panel_3.setBorder(UIManager.getBorder("TextField.border"));
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
        initDataBindings();
    }
    protected void initDataBindings() {
        BeanProperty<Issue, String> issueBeanProperty_1 = BeanProperty.create("issue");
        BeanProperty<JTextArea, String> jLabelBeanProperty = BeanProperty.create("text");
        AutoBinding<Issue, String, JTextArea, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, issue, issueBeanProperty_1, issueText, jLabelBeanProperty);
        autoBinding_1.bind();
        //
        BeanProperty<Issue, String> issueBeanProperty_2 = BeanProperty.create("solution");
        AutoBinding<Issue, String, JTextArea, String> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, issue, issueBeanProperty_2, solutionText, jLabelBeanProperty);
        autoBinding_2.bind();
        // For some strange reason this does not work
//        BeanProperty<Issue, String> issueBeanProperty = BeanProperty.create("subject.subjectText");
//        BeanProperty<JTextArea, String> jTextAreaBeanProperty = BeanProperty.create("text");
//        AutoBinding<Issue, String, JTextArea, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, issue, issueBeanProperty, subjectText, jTextAreaBeanProperty);
//        autoBinding.bind();
        subjectText.setText(issue.getSubject().getSubjectText());
    }
}
