package org.openpnp.gui.support;

import java.awt.Component;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.PatternSyntaxException;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Part;

class TableUtilsTest {
    private static final int COL_ENABLED = 0;
    private static final int COL_ID = 1;
    private static final int COL_PART = 2;
    private static final int COL_HIDDEN = 3;
    private static final int COL_COMMENTS = 4;

    /**
     * A small stand-in for the placements table: an enabled check box, a String id, a Part
     * rendered by its id, a column that is removed from the view and throws if it is ever read
     * (like the Placed column on the Boards tab), and a free-text column.
     */
    private static class Model extends AbstractTableModel {
        final Object[][] rows;

        Model(Object[][] rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.length;
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
                case COL_ENABLED:
                    return Boolean.class;
                case COL_PART:
                    return Part.class;
                default:
                    return String.class;
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == COL_HIDDEN) {
                throw new IllegalStateException("hidden column must not be searched");
            }
            return rows[row][column];
        }
    }

    private static Part r100k;
    private static Part c100n;

    @BeforeAll
    static void setUpConfiguration() {
        Configuration.initialize();
        r100k = createPart("CRCW0402100KF", "Resistor 100k", 1.15);
        c100n = createPart("GRM155R71C104KA88D", "Capacitor 100nF", 0.5);
    }

    private static Part createPart(String id, String name, double heightMm) {
        Part part = new Part(id);
        part.setName(name);
        part.setHeight(new Length(heightMm, LengthUnit.Millimeters));
        return part;
    }

    private static JTable createTable() {
        Model model = new Model(new Object[][] {
                {true, "R115", r100k, null, ""},
                {true, "R7", r100k, null, "near U1"},
                {false, "C3", c100n, null, ""},
        });
        JTable table = new JTable(model);
        table.setDefaultRenderer(Part.class, new IdentifiableTableCellRenderer<Part>());
        table.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer());
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.removeColumn(columnModel.getColumn(COL_HIDDEN));
        table.setRowSorter(new TableRowSorter<>(model));
        return table;
    }

    /**
     * Applies the search filter to a fresh table and returns the number of rows left visible.
     */
    private static int visibleRowsFor(String searchText) throws Exception {
        int[] result = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createTable();
            @SuppressWarnings("unchecked")
            TableRowSorter<Model> sorter = (TableRowSorter<Model>) table.getRowSorter();
            sorter.setRowFilter(TableUtils.createVisibleColumnsSearchFilter(table, searchText));
            result[0] = table.getRowCount();
        });
        return result[0];
    }

    @Test
    void doesNotReadColumnsRemovedFromTheView() throws Exception {
        // Would throw IllegalStateException from the hidden column if it were searched.
        assertEquals(1, visibleRowsFor("R115"));
    }

    @Test
    void matchesPartByTheIdThatIsDisplayed() throws Exception {
        assertEquals(2, visibleRowsFor("CRCW0402100KF"));
        assertEquals(2, visibleRowsFor("crcw0402"));
    }

    @Test
    void doesNotMatchPartTextThatIsNotDisplayed() throws Exception {
        // Part.toString() includes "name ..., heightUnits Millimeters, height 1.150000, ..."
        // None of that is shown in the table, so it must not produce matches.
        assertEquals(1, visibleRowsFor("115"), "only R115 should match, not the part height");
        assertEquals(0, visibleRowsFor("Resistor"));
        assertEquals(0, visibleRowsFor("Millimeters"));
        assertEquals(0, visibleRowsFor("height"));
    }

    @Test
    void doesNotMatchCheckBoxState() throws Exception {
        assertEquals(0, visibleRowsFor("true"));
        assertEquals(0, visibleRowsFor("false"));
    }

    /**
     * The Feeders tab's check box renderer looks up the feeder in the view row it is asked to
     * paint. Narrowing a search down to no rows and then typing more must not fail because of
     * that (the filter must not call such renderers for rows that are not being painted).
     */
    @Test
    void searchingPastNoMatchesWithARowDependentCheckBoxRenderer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createTable();
            @SuppressWarnings("unchecked")
            TableRowSorter<Model> sorter = (TableRowSorter<Model>) table.getRowSorter();
            table.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    t.getModel().getValueAt(sorter.convertRowIndexToModel(row), COL_ID);
                    return super.getTableCellRendererComponent(t, value, isSelected, hasFocus,
                            row, column);
                }
            });
            for (String text : new String[] {"C", "Cz", "Czz", "C3"}) {
                sorter.setRowFilter(TableUtils.createVisibleColumnsSearchFilter(table, text));
            }
            assertEquals(1, table.getRowCount());
        });
    }

    @Test
    void emptyOrBlankSearchShowsAllRows() throws Exception {
        assertEquals(3, visibleRowsFor(""));
        assertEquals(3, visibleRowsFor("   "));
        assertEquals(3, visibleRowsFor(null));
    }

    @Test
    void searchIsTrimmedCaseInsensitiveAndSupportsRegex() throws Exception {
        assertEquals(1, visibleRowsFor("  NEAR u1 "));
        assertEquals(3, visibleRowsFor("^(R|C)\\d+$"));
    }

    @Test
    void searchesColumnsAfterTheyAreReordered() throws Exception {
        int[] result = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createTable();
            table.moveColumn(COL_COMMENTS - 1, 0);
            @SuppressWarnings("unchecked")
            TableRowSorter<Model> sorter = (TableRowSorter<Model>) table.getRowSorter();
            sorter.setRowFilter(TableUtils.createVisibleColumnsSearchFilter(table, "near"));
            result[0] = table.getRowCount();
        });
        assertEquals(1, result[0]);
    }

    @Test
    void invalidRegexStillThrowsSoCallersKeepThePreviousFilter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createTable();
            PatternSyntaxException e = assertThrows(PatternSyntaxException.class,
                    () -> TableUtils.createVisibleColumnsSearchFilter(table, "C0603(10uF"));
            assertTrue(e.getPattern().contains("C0603"));
        });
    }
}
