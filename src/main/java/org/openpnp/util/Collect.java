package org.openpnp.util;

import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * Concatenate two arrays. 
     * 
     * @param first
     * @param second
     * @return
     */
    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Concatenate two byte arrays. 
     * 
     * @param first
     * @param second
     * @return
     */
    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
    
    /**
     * Generate all combinations with the specified number of the elements taken from the given 
     * list. For example, all combinations of 3 elements taken from [1, 2, 3, 4, 5] yields: 
     * [[1, 2, 3], [1, 2, 4], [1, 2, 5], [1, 3, 4], [1, 3, 5], [1, 4, 5], [2, 3, 4], [2, 3, 5], 
     * [2, 4, 5], [3, 4, 5]]. The relative ordering of the elements in each combination is the same
     * as they appeared in the original list.
     * @param list - the list of elements whose combinations are to be formed
     * @param number - the number of elements in each combination
     * @return - the list of combinations or an empty list if number > list.size()
     */
    public static <T> List<List<T>> allCombinationsOfSize(List<T> list, int number) {
        //Setup a list for the results
        List<List<T>> ret = new ArrayList<>();
        
        //Return an empty list if no combinations of the requested size are possible
        if (number > list.size()) {
            return ret;
        }

        //Setup an array of indices that point to the elements for the first combination of elements
        //to be created.  This is just the element indices 0, 1, 2, ... number-1
        int[] indices = new int[number];
        for (int i = 0; i < number; i++) {
            indices[i] = i;
        };
        
        //Add the first combination to the list
        ret.add(extractSublist(list, indices));
        
        //Loop to generate all the remaining combinations
        int idxToIncrement = 0;
        while (true) {
            //Find the farthest right index that can be incremented
            for (int i = number-1; i >= -1; i--) {
                if (i < 0) {
                    //None can be incremented so return the list of generated combinations
                    return ret;
                }
                if (indices[i] < list.size()-number+i) {
                    idxToIncrement = i;
                    break;
                }
            }; 
            
            //Increment the selected index and fill all the indices to the right of it each with
            //the next higher value
            indices[idxToIncrement]++;
            for (int i = idxToIncrement+1; i < number; i++) {
                indices[i] = indices[i-1] + 1; 
            }
            
            //Add the next combination to the list
            ret.add(extractSublist(list, indices));
        }
        
    }
    
    /**
     * Extracts a sublist of elements from a list
     * @param list - the list of elements
     * @param indices - the indices of the elements to extract
     * @return the sublist of elements with the elements in the same order as indices
     */
    public static <T> List<T> extractSublist(List<T> list, int ... indices) {
        List<T> ret = new ArrayList<>();
        for (int i=0; i<indices.length; i++) {
            ret.add(list.get(indices[i]));
        }
        return ret;
}
