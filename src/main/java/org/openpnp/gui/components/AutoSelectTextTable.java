package org.openpnp.gui.components;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Vector;

import javax.swing.InputMap;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

/**
 * From http://tips4java.wordpress.com/2008/10/20/table-select-all-editor/
 *
 * The RXTable provides some extensions to the default JTable
 *
 * 1) Select All editing - when a text related cell is placed in editing mode the text is selected.
 * Controlled by invoking a "setSelectAll..." method.
 *
 * 2) reorderColumns - static convenience method for reodering table columns
 */
@SuppressWarnings("serial")
public class AutoSelectTextTable extends JTable {
    private boolean isSelectAllForMouseEvent = true;
    private boolean isSelectAllForActionEvent = true;
    private boolean isSelectAllForKeyEvent = true;
    private MouseAdapter sortColumnCycler = null;
    private JPopupMenu headerPopupMenu = null;

    //
    // Constructors
    //
    /**
     * Constructs a default <code>RXTable</code> that is initialized with a default data model, a
     * default column model, and a default selection model.
     */
    public AutoSelectTextTable() {
        this(null, null, null);
    }

    /**
     * Constructs a <code>RXTable</code> that is initialized with <code>dm</code> as the data model,
     * a default column model, and a default selection model.
     *
     * @param dm the data model for the table
     */
    public AutoSelectTextTable(TableModel dm) {
        this(dm, null, null);
    }

    /**
     * Constructs a <code>RXTable</code> that is initialized with <code>dm</code> as the data model,
     * <code>cm</code> as the column model, and a default selection model.
     *
     * @param dm the data model for the table
     * @param cm the column model for the table
     */
    public AutoSelectTextTable(TableModel dm, TableColumnModel cm) {
        this(dm, cm, null);
    }

    /**
     * Constructs a <code>RXTable</code> that is initialized with <code>dm</code> as the data model,
     * <code>cm</code> as the column model, and <code>sm</code> as the selection model. If any of
     * the parameters are <code>null</code> this method will initialize the table with the
     * corresponding default model. The <code>autoCreateColumnsFromModel</code> flag is set to false
     * if <code>cm</code> is non-null, otherwise it is set to true and the column model is populated
     * with suitable <code>TableColumns</code> for the columns in <code>dm</code>.
     *
     * @param dm the data model for the table
     * @param cm the column model for the table
     * @param sm the row selection model for the table
     */
    public AutoSelectTextTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);

        //Add a keystroke to de-select all rows of the table (in Windows this would be Ctrl-Shift-A)
        InputMap im = getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        //Should use getMenuShortcutKeyMaskEx here but it is not supported in Java 8
        final int CMD_BTN = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK | CMD_BTN),
                "clearSelection" );
    }

    /**
     * Constructs a <code>RXTable</code> with <code>numRows</code> and <code>numColumns</code> of
     * empty cells using <code>DefaultTableModel</code>. The columns will have names of the form
     * "A", "B", "C", etc.
     *
     * @param numRows the number of rows the table holds
     * @param numColumns the number of columns the table holds
     */
    public AutoSelectTextTable(int numRows, int numColumns) {
        this(new DefaultTableModel(numRows, numColumns));
    }

    /**
     * Constructs a <code>RXTable</code> to display the values in the <code>Vector</code> of
     * <code>Vectors</code>, <code>rowData</code>, with column names, <code>columnNames</code>. The
     * <code>Vectors</code> contained in <code>rowData</code> should contain the values for that
     * row. In other words, the value of the cell at row 1, column 5 can be obtained with the
     * following code:
     * <p>
     *
     * <pre>
     * ((Vector) rowData.elementAt(1)).elementAt(5);
     * </pre>
     * <p>
     *
     * @param rowData the data for the new table
     * @param columnNames names of each column
     */
    public AutoSelectTextTable(Vector rowData, Vector columnNames) {
        this(new DefaultTableModel(rowData, columnNames));
    }

    /**
     * Constructs a <code>RXTable</code> to display the values in the two dimensional array,
     * <code>rowData</code>, with column names, <code>columnNames</code>. <code>rowData</code> is an
     * array of rows, so the value of the cell at row 1, column 5 can be obtained with the following
     * code:
     * <p>
     *
     * <pre>
     *  rowData[1][5];
     * </pre>
     * <p>
     * All rows must be of the same length as <code>columnNames</code>.
     * <p>
     *
     * @param rowData the data for the new table
     * @param columnNames names of each column
     */
    public AutoSelectTextTable(final Object[][] rowData, final Object[] columnNames) {
        super(rowData, columnNames);
    }

    //
    // Overridden methods
    //
    /*
     * Override to provide Select All editing functionality
     */
    public boolean editCellAt(int row, int column, EventObject e) {
        TableCellEditor editor = getCellEditor(row, column);
        if (!isCellSelected(row, column)) {
            // do not show editor when clicked cell from outside. First select cell and next open editor
            return false;
        }
        boolean result = super.editCellAt(row, column, e);

        if (isSelectAllForMouseEvent || isSelectAllForActionEvent || isSelectAllForKeyEvent) {
            selectAll(e);
        }

        return result;
    }

    /*
     * Select the text when editing on a text related cell is started
     */
    private void selectAll(EventObject e) {
        final Component editor = getEditorComponent();

        if (editor == null || !(editor instanceof JTextComponent)) {
            return;
        }

        if (e == null) {
            ((JTextComponent) editor).selectAll();
            return;
        }

        // Typing in the cell was used to activate the editor

        if (e instanceof KeyEvent && isSelectAllForKeyEvent) {
            ((JTextComponent) editor).selectAll();
            return;
        }

        // F2 was used to activate the editor

        if (e instanceof ActionEvent && isSelectAllForActionEvent) {
            ((JTextComponent) editor).selectAll();
            return;
        }

        // A mouse click was used to activate the editor.
        // Generally this is a double click and the second mouse click is
        // passed to the editor which would remove the text selection unless
        // we use the invokeLater()

        if (e instanceof MouseEvent && isSelectAllForMouseEvent) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ((JTextComponent) editor).selectAll();
                }
            });
        }
    }

    //
    // Newly added methods
    //
    /*
     * Sets the Select All property for for all event types
     */
    public void setSelectAllForEdit(boolean isSelectAllForEdit) {
        setSelectAllForMouseEvent(isSelectAllForEdit);
        setSelectAllForActionEvent(isSelectAllForEdit);
        setSelectAllForKeyEvent(isSelectAllForEdit);
    }

    /*
     * Set the Select All property when editing is invoked by the mouse
     */
    public void setSelectAllForMouseEvent(boolean isSelectAllForMouseEvent) {
        this.isSelectAllForMouseEvent = isSelectAllForMouseEvent;
    }

    /*
     * Set the Select All property when editing is invoked by the "F2" key
     */
    public void setSelectAllForActionEvent(boolean isSelectAllForActionEvent) {
        this.isSelectAllForActionEvent = isSelectAllForActionEvent;
    }

    /*
     * Set the Select All property when editing is invoked by typing directly into the cell
     */
    public void setSelectAllForKeyEvent(boolean isSelectAllForKeyEvent) {
        this.isSelectAllForKeyEvent = isSelectAllForKeyEvent;
    }

    // public void changeSelection(final int row, final int column, boolean toggle, boolean extend)
    // {
    // super.changeSelection(row, column, toggle, extend);
    // editCellAt(row, column);
    // transferFocus();
    // }

    @Override
    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        super.setRowSorter(sorter);
        // Change the sorting order clicking to work with three states, ASCENDING>DESCENDING>UNSORTED instead of two.
        if (sortColumnCycler == null) {
            sortColumnCycler = new MouseAdapter() {

                private List<SortKey> lastSortKeys = new ArrayList<>();

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        int column = convertColumnIndexToModel(getTableHeader().columnAtPoint(e.getPoint()));
                        SortOrder sortOrder = SortOrder.ASCENDING;
                        for (RowSorter.SortKey sortKey : lastSortKeys) {
                            if (column == sortKey.getColumn()) {
                                switch (sortKey.getSortOrder()) {
                                    case UNSORTED:
                                        sortOrder = SortOrder.ASCENDING;
                                        break;
                                    case ASCENDING:
                                        sortOrder = SortOrder.DESCENDING;
                                        break;
                                    case DESCENDING:
                                        sortOrder = SortOrder.UNSORTED;
                                        break;
                                }
                                break;
                            }
                        }
                        // Create the new sort keys list starting with the newly clicked one.
                        List<SortKey> sortKeys = new ArrayList<>();
                        RowSorter.SortKey sortKeyClicked = new RowSorter.SortKey(column, sortOrder);
                        sortKeys.add(sortKeyClicked);
                        // Add the rest.
                        for (RowSorter.SortKey sortKey : lastSortKeys) {
                            if (column != sortKey.getColumn()) {
                                sortKeys.add(sortKey);
                            }
                        }
                        RowSorter<?> sorter = getRowSorter();
                        sorter.setSortKeys(sortKeys);
                        this.lastSortKeys = sortKeys;
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        // Right-click: show popup menu
                        AutoSelectTextTable.this.showHeaderPopupMenu(e);
                    }
                }
            };
        }
        getTableHeader().removeMouseListener(sortColumnCycler);
        getTableHeader().addMouseListener(sortColumnCycler);
    }

    /**
     * Shows the header popup menu at the specified mouse event location.
     * This creates a default popup menu with common table operations if none is set.
     *
     * @param e the mouse event that triggered the popup
     */
    private void showHeaderPopupMenu(MouseEvent e) {
        if (headerPopupMenu == null) {
            createDefaultHeaderPopupMenu();
        }

        if (headerPopupMenu != null) {
            int column = getTableHeader().columnAtPoint(e.getPoint());
            if (column >= 0) {
                // Update menu items based on the clicked column if needed
                updateHeaderPopupMenu(column);
                headerPopupMenu.show(getTableHeader(), e.getX(), e.getY());
            }
        }
    }

    /**
     * Creates a default header popup menu with common table operations.
     */
    private void createDefaultHeaderPopupMenu() {
        headerPopupMenu = new JPopupMenu();

        JMenuItem sortAscending = new JMenuItem("Sort Ascending");
        sortAscending.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sortColumnAtPopup(SortOrder.ASCENDING);
            }
        });
        headerPopupMenu.add(sortAscending);

        JMenuItem sortDescending = new JMenuItem("Sort Descending");
        sortDescending.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sortColumnAtPopup(SortOrder.DESCENDING);
            }
        });
        headerPopupMenu.add(sortDescending);

        JMenuItem clearSort = new JMenuItem("Clear Sort");
        clearSort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sortColumnAtPopup(SortOrder.UNSORTED);
            }
        });
        headerPopupMenu.add(clearSort);

        headerPopupMenu.addSeparator();

        JMenuItem autoResize = new JMenuItem("Auto Resize Column");
        autoResize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoResizeColumnAtPopup();
            }
        });
        headerPopupMenu.add(autoResize);

        JMenuItem autoResizeAll = new JMenuItem("Auto Resize All Columns");
        autoResizeAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoResizeAllColumns();
            }
        });
        headerPopupMenu.add(autoResizeAll);
    }

    private int lastPopupColumn = -1;

    /**
     * Updates the popup menu state based on the column that was right-clicked.
     *
     * @param column the column index in view coordinates
     */
    private void updateHeaderPopupMenu(int column) {
        lastPopupColumn = column;
    }

    /**
     * Sorts the column that was right-clicked when the popup was shown.
     *
     * @param sortOrder the sort order to apply
     */
    private void sortColumnAtPopup(SortOrder sortOrder) {
        if (lastPopupColumn >= 0 && getRowSorter() != null) {
            int modelColumn = convertColumnIndexToModel(lastPopupColumn);
            List<SortKey> sortKeys = new ArrayList<>();
            if (sortOrder != SortOrder.UNSORTED) {
                sortKeys.add(new RowSorter.SortKey(modelColumn, sortOrder));
            }
            getRowSorter().setSortKeys(sortKeys);
        }
    }

    /**
     * Auto-resizes the column that was right-clicked when the popup was shown.
     */
    private void autoResizeColumnAtPopup() {
        if (lastPopupColumn >= 0) {
            autoResizeColumn(lastPopupColumn);
        }
    }

    /**
     * Auto-resizes a specific column to fit its content.
     *
     * @param columnIndex the column index in view coordinates
     */
    private void autoResizeColumn(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= getColumnCount()) {
            return;
        }

        int maxWidth = 0;

        // Check header width
        Component headerRenderer = getTableHeader().getDefaultRenderer()
            .getTableCellRendererComponent(this, getColumnModel().getColumn(columnIndex).getHeaderValue(),
                false, false, 0, columnIndex);
        maxWidth = Math.max(maxWidth, headerRenderer.getPreferredSize().width);

        // Check cell widths
        for (int row = 0; row < getRowCount(); row++) {
            Component cellRenderer = getCellRenderer(row, columnIndex)
                .getTableCellRendererComponent(this, getValueAt(row, columnIndex),
                    false, false, row, columnIndex);
            maxWidth = Math.max(maxWidth, cellRenderer.getPreferredSize().width);
        }

        // Add some padding
        maxWidth += 10;

        getColumnModel().getColumn(columnIndex).setPreferredWidth(maxWidth);
    }

    /**
     * Auto-resizes all columns to fit their content.
     */
    private void autoResizeAllColumns() {
        for (int i = 0; i < getColumnCount(); i++) {
            autoResizeColumn(i);
        }
    }

    /**
     * Sets a custom popup menu for the table header.
     *
     * @param popupMenu the popup menu to use, or null to use the default menu
     */
    public void setHeaderPopupMenu(JPopupMenu popupMenu) {
        this.headerPopupMenu = popupMenu;
    }

    /**
     * Gets the current header popup menu.
     *
     * @return the header popup menu, or null if none is set
     */
    public JPopupMenu getHeaderPopupMenu() {
        return headerPopupMenu;
    }

    //
    // Static, convenience methods
    //
    /**
     * Convenience method to order the table columns of a table. The columns are ordered based on
     * the column names specified in the array. If the column name is not found then no column is
     * moved. This means you can specify a null value to preserve the current order of a given
     * column.
     *
     * @param table the table containing the columns to be sorted
     * @param columnNames an array containing the column names in the order they should be displayed
     */
    public static void reorderColumns(JTable table, Object... columnNames) {
        TableColumnModel model = table.getColumnModel();

        for (int newIndex = 0; newIndex < columnNames.length; newIndex++) {
            try {
                Object columnName = columnNames[newIndex];
                int index = model.getColumnIndex(columnName);
                model.moveColumn(index, newIndex);
            }
            catch (IllegalArgumentException e) {
            }
        }
    }
} // End of Class RXTable
