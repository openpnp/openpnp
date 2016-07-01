package org.openpnp.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Some basic Collection utilities. Called Collect instead of something like Collections to avoid
 * namespace collisions with java.utils.Collections.
 */
public class Collect {
    /**
     * Create the cartesian product of a list of lists. The results will contain every possible
     * distinct combination of the elements of the input lists.
     * 
     * Example: cartesianProduct(Arrays.asList(Arrays.asList("A", "B"), Arrays.asList("1", "2")))
     * [[A, 1], [A, 2], [B, 1], [B, 2]]
     * 
     * This method specifically allows for nulls in the input elements. Multiple nulls will be
     * counted multiple times.
     * 
     * @param lists
     * @return
     */
    public static <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> results = new ArrayList<>();
        int[] indexes = new int[lists.size()];
        while (indexes[0] < lists.get(0).size()) {
            // Scan across the columns, adding the current element from each list to the current
            // row and then add the row to the results.
            List<T> result = new ArrayList<>();
            for (int column = 0; column < lists.size(); column++) {
                result.add(lists.get(column).get(indexes[column]));
            }
            results.add(result);
            // Increment the column indexes starting from the right. If a column has reached it's
            // limit, reset it to zero and increment the next one to the left, carrying to
            // the beginning as needed.
            for (int i = indexes.length - 1; i >= 0; i--) {
                indexes[i]++;
                if (indexes[i] < lists.get(i).size() || i == 0) {
                    break;
                }
                indexes[i] = 0;
            }
        }
        return results;
    }
}
