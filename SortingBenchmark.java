import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Sorting algorithm benchmark for the Data Structures & Algorithms practical activity.
 * Default input size: 100,000 integers.
 */
public class SortingBenchmark {
    private static final int DEFAULT_SIZE = 100_000;
    private static final long DATA_SEED = 3_168_057L;
    private static final String[] CASE_NAMES = {
            "Random", "Reverse", "Partial (80%)", "Sorted"
    };

    @FunctionalInterface
    private interface Sorter {
        void sort(int[] values);
    }

    private record Algorithm(String name, String complexity, Sorter sorter) { }

    public static void main(String[] args) throws IOException {
        int n = parseSize(args);
        Map<String, int[]> testCases = createTestCases(n);
        Algorithm[] algorithms = {
                new Algorithm("Selection Sort", "O(N^2)", SortingBenchmark::selectionSort),
                new Algorithm("Insertion Sort", "O(N^2)", SortingBenchmark::insertionSort),
                new Algorithm("Bubble Sort", "O(N^2)", SortingBenchmark::bubbleSort),
                new Algorithm("Merge Sort", "O(N log N)", SortingBenchmark::mergeSort),
                new Algorithm("Quick Sort", "Average O(N log N)", SortingBenchmark::quickSort)
        };

        warmUp(algorithms);
        long[][] results = new long[algorithms.length][CASE_NAMES.length];

        for (int a = 0; a < algorithms.length; a++) {
            for (int c = 0; c < CASE_NAMES.length; c++) {
                int[] workingCopy = Arrays.copyOf(testCases.get(CASE_NAMES[c]), n);
                long start = System.nanoTime();
                algorithms[a].sorter().sort(workingCopy);
                long end = System.nanoTime();
                results[a][c] = (end - start) / 1_000L;
                if (!isSorted(workingCopy)) {
                    throw new IllegalStateException(algorithms[a].name() + " failed on " + CASE_NAMES[c]);
                }
            }
        }

        printResults(algorithms, results, n);
        writeCsv(algorithms, results, n, Path.of("results.csv"));
        System.out.println("\nCSV saved to: " + Path.of("results.csv").toAbsolutePath());
    }

    private static int parseSize(String[] args) {
        if (args.length == 0) return DEFAULT_SIZE;
        if (args.length != 2 || !args[0].equals("--n")) {
            throw new IllegalArgumentException("Usage: java SortingBenchmark [--n <size>]");
        }
        int n = Integer.parseInt(args[1]);
        if (n < 2) throw new IllegalArgumentException("Input size must be at least 2.");
        return n;
    }

    private static Map<String, int[]> createTestCases(int n) {
        Map<String, int[]> cases = new LinkedHashMap<>();
        Random random = new Random(DATA_SEED);

        int[] randomValues = new int[n];
        for (int i = 0; i < n; i++) randomValues[i] = random.nextInt(Math.max(n * 10, 1));
        cases.put(CASE_NAMES[0], randomValues);

        int[] reversed = new int[n];
        for (int i = 0; i < n; i++) reversed[i] = n - i;
        cases.put(CASE_NAMES[1], reversed);

        int[] partial = new int[n];
        for (int i = 0; i < n; i++) partial[i] = i;
        int suffixStart = (int) (n * 0.80);
        for (int i = n - 1; i > suffixStart; i--) {
            int j = suffixStart + random.nextInt(i - suffixStart + 1);
            int temp = partial[i];
            partial[i] = partial[j];
            partial[j] = temp;
        }
        cases.put(CASE_NAMES[2], partial);

        int[] sorted = new int[n];
        for (int i = 0; i < n; i++) sorted[i] = i;
        cases.put(CASE_NAMES[3], sorted);
        return cases;
    }

    private static void warmUp(Algorithm[] algorithms) {
        Random random = new Random(DATA_SEED + 1);
        int[] sample = new int[2_000];
        for (int i = 0; i < sample.length; i++) sample[i] = random.nextInt(10_000);
        for (Algorithm algorithm : algorithms) {
            algorithm.sorter().sort(Arrays.copyOf(sample, sample.length));
        }
    }

    private static void selectionSort(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) {
                if (a[j] < a[min]) min = j;
            }
            if (min != i) swap(a, i, min);
        }
    }

    private static void insertionSort(int[] a) {
        for (int i = 1; i < a.length; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    private static void bubbleSort(int[] a) {
        for (int end = a.length - 1; end > 0; end--) {
            boolean swapped = false;
            for (int j = 0; j < end; j++) {
                if (a[j] > a[j + 1]) {
                    swap(a, j, j + 1);
                    swapped = true;
                }
            }
            if (!swapped) break;
        }
    }

    private static void mergeSort(int[] a) {
        mergeSort(a, new int[a.length], 0, a.length - 1);
    }

    private static void mergeSort(int[] a, int[] buffer, int left, int right) {
        if (left >= right) return;
        int middle = left + (right - left) / 2;
        mergeSort(a, buffer, left, middle);
        mergeSort(a, buffer, middle + 1, right);
        if (a[middle] <= a[middle + 1]) return;
        int i = left, j = middle + 1, k = left;
        while (i <= middle && j <= right) {
            buffer[k++] = (a[i] <= a[j]) ? a[i++] : a[j++];
        }
        while (i <= middle) buffer[k++] = a[i++];
        while (j <= right) buffer[k++] = a[j++];
        System.arraycopy(buffer, left, a, left, right - left + 1);
    }

    private static void quickSort(int[] a) {
        quickSort(a, 0, a.length - 1);
    }

    private static void quickSort(int[] a, int low, int high) {
        while (low < high) {
            int pivot = medianOfThree(a[low], a[low + (high - low) / 2], a[high]);
            int lt = low, i = low, gt = high;
            while (i <= gt) {
                if (a[i] < pivot) swap(a, lt++, i++);
                else if (a[i] > pivot) swap(a, i, gt--);
                else i++;
            }
            // Recurse into the smaller side and iterate over the larger side.
            if (lt - low < high - gt) {
                quickSort(a, low, lt - 1);
                low = gt + 1;
            } else {
                quickSort(a, gt + 1, high);
                high = lt - 1;
            }
        }
    }

    private static int medianOfThree(int x, int y, int z) {
        if (x < y) return (y < z) ? y : Math.max(x, z);
        return (x < z) ? x : Math.max(y, z);
    }

    private static void swap(int[] a, int i, int j) {
        int temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    private static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }

    private static void printResults(Algorithm[] algorithms, long[][] results, int n) {
        System.out.println("Sorting Algorithms Benchmark | N = " + n + " | Time = microseconds (µs)");
        System.out.printf("%-18s %15s %15s %18s %15s%n", "Algorithm", "Random", "Reverse", "Partial (80%)", "Sorted");
        for (int a = 0; a < algorithms.length; a++) {
            System.out.printf("%-18s %15d %15d %18d %15d%n", algorithms[a].name(),
                    results[a][0], results[a][1], results[a][2], results[a][3]);
        }
    }

    private static void writeCsv(Algorithm[] algorithms, long[][] results, int n, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("Algorithm,Complexity,InputSize,Case,TimeMicroseconds\n");
            for (int a = 0; a < algorithms.length; a++) {
                for (int c = 0; c < CASE_NAMES.length; c++) {
                    writer.write(String.format("%s,%s,%d,%s,%d%n", algorithms[a].name(),
                            algorithms[a].complexity(), n, CASE_NAMES[c], results[a][c]));
                }
            }
        }
    }
}
