package org.openpnp.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    
    /**
     * Computes a list of index ranges to remove and a list of index ranges to insert that updates 
     * one list to match another list without disturbing the elements that are common to both lists. 
     * Useful for updating combo boxes and tables when items are inserted or removed without 
     * disturbing the currently selected item.
     * 
     * <pre>
     * <code>As an example, suppose:
     *      listToUpdate = [3, 5, 7, 8, 10, 11, 12, 17, 18, 20, 21, 26, 33, 36]
     *      targetList = [2, 5, 9, 11, 14, 17, 22, 23, 24, 27, 28, 31, 32, 37, 38]
     *      
     *      Running this method results in:
     *      indicesToRemove = [ [13, 8], [6, 6], [4, 2], [0, 0] ]
     *      indicesToInsert = [ [0, 0], [2, 2], [4, 4], [6, 14] ]
     *      
     *      The removals need to be performed first and in the order specified in indicesToRemove:
     *      Removing elements 13 down to 9 results in listToUpdate = [3, 5, 7, 8, 10, 11, 12, 17],
     *      removing element 6 results in listToUpdate = [3, 5, 7, 8, 10, 11, 17],
     *      removing elements 4 down to 2 results in listToUpdate = [3, 5, 11, 17],
     *      and finally removing element 0 results in listToUpdate = [5, 11, 17].
     *      
     *      Next, the insertions need to be performed and in the order specified in indicesToInsert:
     *      Inserting element 0 (2) results in listToUpdate = [2, 5, 11, 17],
     *      inserting element 2 (9) results in listToUpdate = [2, 5, 9, 11, 17],
     *      inserting element 4 (14) results in listToUpdate = [2, 5, 9, 11, 14, 17],
     *      and finally inserting elements 6 to 14 (22, 23, 24, 27, 28, 31, 32, 37, 38) results in 
     *      listToUpdate = [2, 5, 9, 11, 14, 17, 22, 23, 24, 27, 28, 31, 32, 37, 38].
     *      
     *      Note, listToUpdate now contains exactly the same elements as newList and that the 
     *      elements that we already in both lists (5, 11, and 17) were not touched.
     * </code>
     * </pre>
     * @param listToModify - the list that is to be updated. Must contain unique items and must be 
     * sorted in the same order as targetList.
     * @param targetList - the list that listToModify is to be updated to match. Must contain unique 
     * items and must be sorted in the same order as listToModify.
     * @param indicesToRemove - a list of index ranges that are to be removed from listToModify. 
     * Each entry in this list is a two element array of indices <b>[a, b]</b> where <b>a >= b</b>. 
     * All elements in the range <b>a down to b</b> (inclusive) should be removed from the 
     * listToModify. The list of index ranges is sorted highest to lowest so that the removals can 
     * be performed in the order specified (descending) without risk of causing changes to the 
     * indices of elements that are yet to be removed. Note that all removals must to be performed 
     * in the order specified (descending) and before any insertions specified by indicesToInsert.
     * @param indicesToInsert - a list of index ranges that are of the elements in targetList that 
     * are to be inserted into listToModify. Each entry of this list is a two element array of 
     * indices <b>[a, b]</b> where <b>a <= b</b>. All elements in the range <b>a to b</b> 
     * (inclusive) need to be copied from the targetList and inserted into the listToModify. The 
     * list of index ranges is sorted lowest to highest so that the insertion point in listToModify 
     * is the same as the index of the item in targetList. Note that all insertions must be 
     * performed in the order specified (ascending) and only after all removals specified by 
     * indicesToRemove.
     */
    public static <T> void computeInPlaceUpdateIndices(List<T> listToModify, List<T> targetList, 
            List<int[]> indicesToRemove, List<int[]> indicesToInsert) {
        
        indicesToRemove.clear();
        indicesToInsert.clear();
        
        //Find those items that need to be removed from the old list
        List<Integer> idxToRemove = new ArrayList<>();
        if (listToModify != null) {
            for (int idx = 0; idx<listToModify.size(); idx++) {
                if (targetList == null || !targetList.contains(listToModify.get(idx))) {
                    idxToRemove.add(idx);
                }
            }
        }
        
        //The items to be removed should be removed from highest index to lowest so as to not 
        //disturb the indices of those items that are yet to be removed
        Collections.reverse(idxToRemove);
        
        //Group the indices into contiguous ranges
        boolean starting = true;
        int a = 0;
        int b = 0;
        for (int idx : idxToRemove) {
            if (starting) {
                a = idx;
                b = idx;
                starting = false;
            }
            else if (idx == b-1) {
                b = idx;
            }
            else if (idx < b-1) {
                indicesToRemove.add(new int[] {a, b});
                a = idx;
                b = idx;
            }
        }
        if (!starting) {
            indicesToRemove.add(new int[] {a, b});
        }
        
        //Find those items that need to be added to the list
        List<Integer> idxToAdd = new ArrayList<>();
        if (targetList != null) {
            for (int idx=0; idx<targetList.size(); idx++) {
                if (listToModify == null || !listToModify.contains(targetList.get(idx))) {
                    idxToAdd.add(idx);
                }
            }
            
            //Group the indices into contiguous ranges
            starting = true;
            for (int idx : idxToAdd) {
                if (starting) {
                    a = idx;
                    b = idx;
                    starting = false;
                }
                else if (idx == b+1) {
                    b = idx;
                }
                else if (idx > b+1) {
                    indicesToInsert.add(new int[] {a, b});
                    a = idx;
                    b = idx;
                }
            }
            if (!starting) {
                indicesToInsert.add(new int[] {a, b});
            }
        }
    }
}
