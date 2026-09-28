import java.util.Arrays;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * XSort class.
 * Demonstrates the use of external sorting algorithms on plain text.
 * 
 * The functions used for this program are placed in order below main.
 * Main will create initial runs, and the way they're sorted and merged is
 * optionally by iteration or recursion.
 * 
 * Lines of data are sorted using a heap, where heap also has an entry that
 * implements comparable. This means that I could chose how to compare the lines
 * without changing the chars.
 */
public class XSort {

    /* This will create a subdir for the temp merging files */
    private static final File TEMP_DIR = new File(".", "temp");

    /**
     * Entry point.
     * Starts the XSort program given input and arguments.
     * @param   args    The run size, and optionally the k factor.
     */
    public static void main(String[] args) {
        try {
            /* Check if any arguments were provided */
            if (args.length < 1) {
                throw new IllegalArgumentException("Run size was not provided.");
            }

            /* Check if first argument is within bounds */
            int runSize = Integer.parseInt(args[0]);
            if (runSize < 64 || 1024 < runSize) {
                throw new IllegalArgumentException("Incorrect argument for <runSize>");
            }

            int k = 2; /* The default k-way factor */
            /* If the second is provided then set accordingly */
            if (args.length > 1) {
                k = Integer.parseInt(args[1]);
                k = Math.min(k, 12);
            }

            /* Create temp directory */
            TEMP_DIR.mkdir();

            /* Try-with block opens reader and writer to auto-close */
            try (
                BufferedReader stdin =
                    new BufferedReader(new InputStreamReader(System.in));
                BufferedWriter stdout =
                    new BufferedWriter(new OutputStreamWriter(System.out));
            ) {
                /*
                 * Create an array of initial runs from standard input.
                 *
                 * The files will be put into TEMP_DIR, and this function will
                 * return the array that points to each one.
                 */
                File[] runs = createInitialRuns(stdin, runSize);

                /*
                 * Perform the sort-merge on the initial runs.
                 *
                 * I wrote two functions: 
                 * 1.   One that performs a recursive heap-sort-merge with what
                 *      I thought was the formal algorithim for a balanced
                 *      k-way merge-sort.
                 *
                 * 2.   The alternative performs a 'balanced' merge using a
                 *      fixed amount of temp files for input/output and iterates
                 *      until complete.
                 * 
                 * I've left the balancedMerge commented out because I prefer
                 * the sortMergeKWay. Not only was it easier to wrap my head
                 * around but I think it's more efficient. The only con is that
                 * it's recursive (recursion is scary) but I think it's brought
                 * be outta my comfort zone.
                 */
                File result = sortMergeKWay(runs, k);   /* Preferred */
                //File result = balancedMerge(runs, k);      /* 2-way merge */

                /*
                 * Direct result from temporary file to standard output.
                 */
                //XSort.writeOutput(result, stdout);          /* As is */
                XSort.writeOutputClean(result, stdout);     /* Cleaned */

                stdout.flush();
            } catch (IOException e) {
                System.out.println("\tIOException: " + e.getMessage());
                System.exit(1);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\tIllegalArgumentException: " + e.getMessage() + "\nUsage: cat MobyDick.txt | java XSort 512 > Result.txt\n");
        } catch (Exception e) {
            System.out.println("\tUnexpected Exception: " + e.getMessage() + "\n");
        } finally {
            TEMP_DIR.delete();
            System.exit(0);
        }
    }

    /**
     * Creates the initial runs as temporary files.
     * @param   input       The BufferedReader for reading the input to sort.
     * @param   runSize     The length of each run, the count of lines.
     */
    public static File[] createInitialRuns(BufferedReader input, int runSize) throws IOException {
        /* Array of predicted size runs — maybe a silly idea but it works.
         * e.g., if runSize is 64, then 8 initial runs for 2-4 way merge
         * seems appriopriate, and then size will also be doubled if the
         * limit is reached.
         */
        File[] runs = new File[runSize / 8];
        int n = 0; /* Counter for actual size */

        /* Outer loop to create String arrays for blocks of data */
        while (true) {
            String[] block = new String[runSize];
            int i = 0; /* Counter for lines */

            /* Nested loop to store each line of a String array */
            while (i < runSize) {
                String line = XSort.readInput(input);
                if (line == null) {
                    break; /* Exit nested loop when EOF */
                } else {
                    block[i] = line;
                    i++;
                }
            }

            /* Exit if no lines left */
            if (i == 0) {
                break;
            }

            /* Copy to actual size of array if shorter */
            if (i < runSize) {
                block = Arrays.copyOf(block, i);
            }

            Heap.sort(block); /* Use heapsort (in place) on the lines */

            /* Create a new temporary file for the block of data */
            File file = XSort.createTempFile("createInitialRuns");
            BufferedWriter fw = new BufferedWriter(new FileWriter(file));

            /* Write each line from the block array of String lines */
            for (int j = 0; j < block.length; j++) {
                fw.write(block[j]);
            }
    
            fw.flush();
            fw.close();

            runs[n] = file; /* Add this file to the runs */
            n++;

            /* Here is where we double run array slots if needed */
            if (n == runs.length) {
                File[] runsCopy = new File[runs.length * 2];
                for (int m = 0; m < runs.length; m++) {
                    runsCopy[m] = runs[m];
                }
                runs = runsCopy;
            }
        }
        /* Then trim the array of runs before returning */
        return Arrays.copyOf(runs, n);
    }


    /**
     * Performs balanced-merge sort.
     * @param   runs    The array of run Files to sort.
     */
    public static File balancedMerge(File[] runs, int k) throws IOException {
        File[] queue = Arrays.copyOf(runs, runs.length);
        int size = runs.length;

        /* Loop until only one file remaining */
        while (size > 1) {
            int n = 0; /* Actual file counter */
            File[] next = new File[(size + k - 1) / k]; /* Count of groups */

            /* For the current size, merge each subgroup */
            for (int i = 0; i < size; i += k) {
                int m = Math.min(k, size - i);
                File[] group = new File[m];
                for (int j = 0; j < m; j++) {
                    group[j] = queue[i + j];
                }

                /* If group is merged, then put into next run queue */
                if (m == 1) {
                    next[n] = group[0];
                } else {
                    next[n] = sortMerge(group); /* Call sortMerge on this group */
                    for (int j = 0; j < m; j++) {
                        group[j].delete(); /* Delete the old files! */
                    }
                }
                n++;
            }

            queue = next;
            size = n;
        }
        return queue[0];
    }


    /**
     * Performs sort-merge by k.
     * @param   runs    The array of run Files to sort and merge.
     * @param   k       The k factor to merge files by.
     * @return  The file that was merged from the runs.
     */
    public static File sortMergeKWay(File[] runs, int k) throws IOException {
        /* If there's only one left then return the file. */
        if (runs.length == 1) {
            return runs[0];
        } else {
            k = Math.min(k, runs.length);
            /* Create array with respect to k for the merged files */
            File[] merged = new File[(runs.length + k -1) / k];
            int n = 0; /* Counter for new set of runs */

            /* Loop through runs array and create subgroups of k to merge */
            for (int i = 0; i < runs.length; i+=k) {
                int count = Math.min(i + k, runs.length);
                File[] group = Arrays.copyOfRange(runs, i, count);

                /* Merge-sort the group and put the file in the new array */
                merged[n] = XSort.sortMerge(group);
                n++;

                /* Delete the individual files in the group */
                for (File f : group) {
                    f.delete();
                }
            }
            return XSort.sortMerge(merged);
        }
    }


    /**
     * Performs sort-merge with a min heap.
     * @param   runs    The array of run Files to heapsort and merge.
     * @return  The file that was merged from the runs.
     */
    private static File sortMerge(File[] runs) throws IOException {
        /* Sorts the given runs using a heap and file readers */
        Heap heap = new Heap(runs.length);
        BufferedReader[] readers = new BufferedReader[runs.length];

        /* Insert a line to the heap with the file reader for that file */
        for (int i = 0; i < runs.length; i++) {
            readers[i] = new BufferedReader(new FileReader(runs[i]));
            String line = XSort.readInput(readers[i]);
            if (line != null) {
                heap.insert(line, readers[i]); /* Uses sift-up logic */
            }
        }

        /* Create a new temporary file for the merged files */
        File file = XSort.createTempFile("sortMerge");
        BufferedWriter fw = new BufferedWriter(new FileWriter(file));
        /* Remove min from heap and write the line to a file */
        while (!heap.isEmpty()) {
            Heap.Entry entry = heap.removeMin();
            fw.write(entry.LINE);

            /* If the next line exists then insert with the same reader */
            String nextLine = XSort.readInput(entry.reader);
            if (nextLine != null) {
                heap.insert(nextLine, entry.reader);
            } else {
                entry.reader.close(); /* Close it once it's done */
            }
        }
        /* Clean up resources... */
        fw.flush();
        fw.close();
        for (BufferedReader reader : readers) {
            if (reader != null) {
                reader.close();
            }
        }

        /* Delete original runs files */
        for (File f : runs) {
            f.delete();
        }

        return file; /* The merged file */
    }


    /**
     * Reads input as a line given a reader.
     * Important: This cleans double-spaces but can be edited to preserve them!
     * @param   input  The BufferedReader to read input from.
     * @return  The String line read from the input.
     */
    private static String readInput(BufferedReader input) throws IOException {
        StringBuilder str = new StringBuilder();

        int c = input.read();
        if (c == -1) {
            return null;
        } else {
            while (c != -1) {
                str.append((char) c);
                if (c == '\n') {
                    break;
                } else {
                    c = input.read();
                }
            }

            String line = str.toString();
            line = line.replaceAll(" {2,}", ""); /* Comment this out if not allowed */
            return line;
        }
    }


    /**
     * Writes output as lines from a given a file.
     * @param   file    The File to read from and send to writer.
     * @param   out     The BufferedWriter to send output to.
     */
    private static void writeOutput(File file, BufferedWriter out) throws IOException {
        /* Try with a new reader to open the file */
        try (
            BufferedReader reader =
                new BufferedReader(new FileReader(file))
        ) {
            /* The file is read by char and not by line. This is to preserve
             * the hidden characters. Maybe it's more overhead, but not
             * sure if that's the focus here!
             */
            int c;
            while ((c = reader.read()) != -1) {
                out.write(c);
            }
        } finally {
            out.flush();
            file.delete(); /* Delete the temporary file */
        }
    }

    /**
     * Writes output as cleaned from a given a file.
     * @param   file    The File to read from and send to writer.
     * @param   out     The BufferedWriter to send output to.
     */
    private static void writeOutputClean(File file, BufferedWriter out) throws IOException {
        try (
            BufferedReader reader = new BufferedReader(new FileReader(file))
        ) {
            String last = "";
            String line;

            while (true) {
                line = readInput(reader).strip(); /* Cheeky strip */
                if (line == null) {
                    break;
                } else {
                    last = last.strip();
                    /* If it's not a double blank line then write it out */
                    if (!(last == line && last.isEmpty())) {
                        out.write(line);
                        out.newLine(); /* Gotta write newLine for this one */
                    }
                    last = line;
                }
            }
        } finally {
            out.flush();
            file.delete(); /* Comment this out to keep final merged file */
        }
    }

    /**
     * Creates a temporary file in this temp directory.
     * @param   filename    The String name of temporary file.
     * @return  The File object in the temporary directory.
     */
    private static File createTempFile(String filename) throws IOException {
        File file = File.createTempFile(filename, ".txt", TEMP_DIR);
        return file;
    }


    /**
     * Gets count of files that aren't empty.
     * @param   files   The array of run Files to check.
     */
    private static int getActiveFileCount(File[] files) {
        int count = 0;
        /* This feels inefficient for the amount of times it 
         * has to be called but not sure how else to do this!
         */
        for (File f : files) {
            if (f.length() > 0) {
                count++;
            } else {
                f.delete();
            }
        }
        return count;
    }
}
