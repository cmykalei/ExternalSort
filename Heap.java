import java.io.BufferedReader;
import java.util.Arrays;


/**
 * Heap class.
 * Provides a MIN Heap, with both static and non-static functions.
 */
public class Heap {

    /**
     * Entry class.
     * Implements comparable to override compareTo.
     */
    class Entry implements Comparable<Entry> {

        public final String LINE;
        public BufferedReader reader;

        /**
         * Entry Constructor.
         * @param   line    The first line of plain text from  a file.
         * @param   reader  The BufferedReader that has the file open.
         */
        public Entry(String line, BufferedReader reader) {
            this.LINE = line;
            this.reader = reader;
        }


        /**
         * Overrides compareTo.
         * The LINE for both is 
         * @param   other   The other Entry object to compare to this Entry.
         */
        @Override
        public int compareTo(Entry other) {
            return this.LINE.strip().compareToIgnoreCase(other.LINE.strip());
        }
    }


    Entry[] heap;   /* The array of entries */
    int n;          /* The count of entries */

    /**
     * Heap constructor.
     * Creates a Heap object with an array of size k.
     * @param   k   The amount of entries in this Heap's array.
     */
    public Heap(int k) {
        heap = new Entry[k];
        n = 0; /* Starts count as 0 */
    }


    /**
     * Inserts an entry to the min heap.
     * Uses 'siftUp' after insertion to maintain order.
     * @param   line    The String line to add to this entry.
     * @param   reader  The BufferedReader that has the line's file open.
     */
    public void insert(String line, BufferedReader reader) {
        int i = n;
        heap[i] = new Entry(line, reader); /* Creates an Entry object */
        /* Maintain the order by sifting up, then increment counter */
        siftUp(i);
        n++;
    }


    /**
     * Removes the minimum Entry from this Heap.
     * Uses 'siftDown' after replacing the min to maintain order.
     * @return The Entry at the root of this minimum heap.
     */
    public Entry removeMin() {
        if (n == 0) {
            return null;
        } else {
            Entry min = heap[0];
            n--;

            heap[0] = heap[n];
            siftDown(0);
            return min;
        }
    }


    /**
     * Sift up from the index and swap child with parent if necessary.
     * @param   i   The index to start sifting up from.
     */
    public void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;

            /* Swap if the current should be its parent */
            if (heap[i].compareTo(heap[parent]) < 0) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }


    /**
     * Sift down from the index and swap until min is at root.
     * @param   i   The index to start sifting down from.
     */
    public void siftDown(int i) {
        while (((2 * i) + 1) < n) {
            int l = (2 * i) + 1;
            int r = (2 * i) + 2;
            int min = i;

            /* Check and set if min is left child */
            if (heap[l].compareTo(heap[min]) < 0) {
                min = l;
            }
            /* Check and set if min is right child */
            if ((r < n) && heap[r].compareTo(heap[min]) < 0) {
                min = r;
            }
            /* Stop if actual min is reached */
            if (min == i) {
                break;
            } else {
                /* Swap the actual elements */
                swap(i, min);
                i = min;
            }
        }
    }


    /**
     * @return  True if length n is 0, False otherwise.
     */
    public boolean isEmpty() {
        if (n == 0) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Swaps the specified entries for this Heap.
     * @param   i   Index of Entry to swap with j
     * @param   j   Index of Entry to swap with i
     */
    public void swap(int i, int j) {
        Entry temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }


    /**
     * Swaps the specified entries in a given array.
     * @param   array   The array to modify.
     * @param   i       Index of Entry to swap with j
     * @param   j       Index of Entry to swap with i
     */
    public static void swap(String[] array, int i, int j) {
        String temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }


    /**
     * @param   a   The String to compare with b
     * @param   b   The String being compared to a.
     * @return  The int result of comparing the Strings.
     */
    public static int compare(String a, String b) {
        return a.strip().compareToIgnoreCase(b.strip());
    }


    /**
     * Sorts a given array in place.
     * Uses 'heapify' and 'swap' to iteratively sort the array.
     * @param   array   The array of Strings to sort.
     */
    public static void sort(String[] array) {
        /* Build a max heap first */
        for (int i = (array.length / 2) - 1; i >= 0; i--) {
            heapify(array, array.length, i);
        }

        /* Swaps max with root and heapify */
        for (int j = array.length - 1; j > 0; j--) {
            swap(array, 0, j);
            heapify(array, j, 0);
        }

        /* Re-order for min-heap */
        int m = array.length - 1;
        for (int n = 0; n < m; n++) {
            swap(array, n, m);
            m--;
        }
    }


    /**
     * Heapify's a given array with specified size and index.
     * @param   array   The array to heapify.
     * @param   n       The assumed size of the array.
     * @param   i       The index, and assumed minimum.
     */
    public static void heapify(String[] array, int n, int i) {
        while (true) {
            int min = i;
            int l = (2 * i) + 1;
            int r = (2 * i) + 2;

            /* Check and set if min is left child */
            if (l < n && array[l] != null && array[min] != null && compare(array[l], array[min]) < 0) {
                min = l;
            }
            /* Check and set if min is right child */
            if (r < n && array[r] != null && array[min] != null && compare(array[r], array[min]) < 0) {
                min = r;
            }
            /* Stop if reached actual min */
            if (min == i) {
                break;
            } else {
                /* Swap the actual elements */
                swap(array, i, min);
                i = min;
            }
        }
    }
}
