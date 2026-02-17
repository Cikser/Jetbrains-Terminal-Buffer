package benchmark;

import terminalbuffer.TerminalBuffer;

public class PerformanceBenchmark {

    private static final int ITERATIONS = 1000;

    public static void main(String[] args) {
        System.out.println("=== Terminal Buffer Performance Benchmark ===\n");

        benchmarkWrite();
        benchmarkWriteWide();
        benchmarkInsert();
        benchmarkResize();
    }

    private static void benchmarkWrite() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24, 1000);
        String text = "Hello World! ".repeat(10); // 130 chars

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            buffer.clearScreen();
            buffer.write(text);
        }
        long end = System.nanoTime();

        double ms = (end - start) / 1_000_000.0;
        System.out.printf("Write (narrow chars): %.2f ms total, %.4f ms/iter\n", ms, ms/ITERATIONS);
    }

    private static void benchmarkWriteWide() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24, 1000);
        String text = "中".repeat(40); // 40 wide chars = 80 cells

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            buffer.clearScreen();
            buffer.write(text);
        }
        long end = System.nanoTime();

        double ms = (end - start) / 1_000_000.0;
        System.out.printf("Write (wide chars):   %.2f ms total, %.4f ms/iter\n", ms, ms/ITERATIONS);
    }

    private static void benchmarkInsert() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24, 1000);

        // Popuni buffer
        for (int i = 0; i < 24; i++) {
            buffer.write("A".repeat(80), i, 0);
        }

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            buffer.cursor().set(0, 40);
            buffer.insert("XYZ");
        }
        long end = System.nanoTime();

        double ms = (end - start) / 1_000_000.0;
        System.out.printf("Insert (full buffer): %.2f ms total, %.4f ms/iter\n", ms, ms/ITERATIONS);
    }

    private static void benchmarkResize() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24, 1000);

        // Popuni sa sadržajem
        for (int i = 0; i < 24; i++) {
            buffer.write("Line " + i + " content here", i, 0);
        }

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) { // Manje iteracija jer je skupo
            buffer.resize(40, 24);
            buffer.resize(80, 24);
        }
        long end = System.nanoTime();

        double ms = (end - start) / 1_000_000.0;
        System.out.printf("Resize (narrow/wide): %.2f ms total, %.2f ms/resize\n", ms, ms/ITERATIONS*2);
    }
}