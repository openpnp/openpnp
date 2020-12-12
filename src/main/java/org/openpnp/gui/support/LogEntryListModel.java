package org.openpnp.gui.support;

import org.pmw.tinylog.Configuration;
import org.pmw.tinylog.LogEntry;
import org.pmw.tinylog.writers.LogEntryValue;
import org.pmw.tinylog.writers.Writer;

import javax.swing.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A LogEntry List Model which contains LogEntries from tinylog
 */
public class LogEntryListModel extends AbstractListModel<LogEntry> implements Writer {

    private List<LogEntry> originalLogEntries = new ArrayList<>();
    private List<LogEntry> filteredLogEntries = new ArrayList<>(originalLogEntries);
    private List<LogEntry> newLogEntries = new ArrayList<>();
    private HashSet<LogEntryFilter> filters = new HashSet<>();

    public static class LogEntryFilter {
        Predicate<LogEntry> filter;

        public LogEntryFilter() {
            this.filter = (logEntry) -> true;
        }

        public LogEntryFilter(Predicate<LogEntry> filter) {
            this.filter = filter;
        }

        Predicate<LogEntry> getFilter() {
            return filter;
        }

        public void setFilter(Predicate<LogEntry> filter) {
            this.filter = filter;
        }
    }

    private static final int LINE_LIMIT = 10000;

    /**
     * Returns a snapshot copy of the original log entries.
     * @return 
     */
    public synchronized List<LogEntry> getOriginalLogEntries() {
        return new ArrayList<>(originalLogEntries);
    }

    /**
     * Returns a snapshot copy of the filtered log entries.
     * @return
     */
    public synchronized List<LogEntry> getFilteredLogEntries() {
        return new ArrayList<>(filteredLogEntries);
    }

    @Override
    public synchronized int getSize() {
        return filteredLogEntries.size();
    }

    @Override
    public synchronized LogEntry getElementAt(int index) {
        // This check is needed to exclude race conditions in heavily threaded logging, i.e. calls to getSize()
        // followed by getElementAt() are not atomic. 
        if (index < filteredLogEntries.size()) {
            return filteredLogEntries.get(index);
        }
        return null;
    }

    public synchronized void addFilter(LogEntryFilter filter) {
        this.filters.add(filter);
        filter();
    }

    public synchronized void removeFilter(LogEntryFilter filter) {
        this.filters.remove(filter);
        filter();
    }

    @Override
    public Set<LogEntryValue> getRequiredLogEntryValues() {
        return EnumSet.of(LogEntryValue.RENDERED_LOG_ENTRY);
    }

    @Override
    public void init(Configuration configuration) throws Exception {

    }

    @Override
    public synchronized void write(LogEntry logEntry) throws Exception {
        newLogEntries.add(logEntry);
    }

    public synchronized void clear() {
        this.originalLogEntries.clear();
        filter();
    }

    public synchronized void filter() {
        // Reduce all filters to a single one and apply it to our logEntries
        this.filteredLogEntries = originalLogEntries.stream().filter(
                filters.stream().map(LogEntryFilter::getFilter).reduce(Predicate::and).orElse(t -> false)
        ).collect(Collectors.toList());

        SwingUtilities.invokeLater(() -> fireContentsChanged(this, 0, filteredLogEntries.size() - 1));
    }

    public synchronized boolean isRefreshNeeded() {
        return !newLogEntries.isEmpty();
    }

    public synchronized void refresh() {
        if (newLogEntries.size() > LINE_LIMIT) {
            // New Log entries alone already surpass the limit. 
            newLogEntries.subList(0, newLogEntries.size() - LINE_LIMIT).clear();
            originalLogEntries.clear();
        }
        else if (originalLogEntries.size() + newLogEntries.size() > LINE_LIMIT) {
            // Make space for the new log entries.
            originalLogEntries.subList(0, Math.max(0, originalLogEntries.size() - newLogEntries.size() - LINE_LIMIT)).clear();
        }
        originalLogEntries.addAll(newLogEntries);
        newLogEntries.clear();
        filter();
    }

    @Override
    public void flush() throws Exception {

    }

    @Override
    public void close() throws Exception {

    }
}
