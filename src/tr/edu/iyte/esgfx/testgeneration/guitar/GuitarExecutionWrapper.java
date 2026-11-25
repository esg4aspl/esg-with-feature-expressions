package tr.edu.iyte.esgfx.testgeneration.guitar;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class GuitarExecutionWrapper {

    /**
     * Peak resident set size, in kilobytes, of the GUITAR process started by
     * the last call of generateTestsFromEFG, or -1 where it cannot be read.
     * A separate thread reads VmHWM of /proc/<pid>/status while the process
     * runs; VmHWM never decreases, so the last value read is the peak up to
     * that moment. Nothing is added to the command line of GUITAR.
     */
    private static volatile long lastChildPeakRssKb = -1;

    public static long getLastChildPeakRssKb() {
        return lastChildPeakRssKb;
    }

    private static Thread watchPeakRss(Process process) {
        final File status = new File("/proc/" + process.pid() + "/status");
        lastChildPeakRssKb = -1;
        if (!status.exists()) {
            return null;
        }
        Thread watcher = new Thread(() -> {
            while (process.isAlive()) {
                try (BufferedReader reader = new BufferedReader(new java.io.FileReader(status))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("VmHWM:")) {
                            long value = Long.parseLong(line.substring(6).replace("kB", "").trim());
                            if (value > lastChildPeakRssKb) {
                                lastChildPeakRssKb = value;
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    // The process ended between the check and the read.
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        watcher.setDaemon(true);
        watcher.start();
        return watcher;
    }

    /** Path of the Java 8 runtime that GUITAR needs. */
    private static String java8Path() {
        String java8Path = "java";
        String osName = System.getProperty("os.name").toLowerCase();
        String envJava8 = System.getenv("JAVA_8_EXE");
        if (envJava8 != null && !envJava8.trim().isEmpty()) {
            java8Path = envJava8;
        } else if (osName.contains("linux")) {
            File linuxJava8 = new File("/usr/lib/jvm/java-8-openjdk-amd64/bin/java");
            if (linuxJava8.exists()) {
                java8Path = linuxJava8.getAbsolutePath();
            }
        } else if (osName.contains("mac")) {
            java8Path = "/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/bin/java";
        }
        return java8Path;
    }

    private static String guitarClasspath() {
        File libDir = new File(System.getProperty("user.dir"), "guitar-libs");
        File[] jars = libDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            throw new RuntimeException("CRITICAL ERROR: No .jar files found in: " + libDir.getAbsolutePath());
        }
        StringBuilder cpBuilder = new StringBuilder();
        for (File jar : jars) {
            cpBuilder.append(jar.getAbsolutePath()).append(File.pathSeparator);
        }
        return cpBuilder.toString();
    }

    /**
     * Fixed cost of starting GUITAR: the generator is started without
     * arguments, so it loads its classes, prints its usage and ends. Returns
     * the median of 'repeats' runs in milliseconds.
     */
    public static double measureStartupMs(int repeats) throws Exception {
        double[] times = new double[repeats];
        for (int i = 0; i < repeats; i++) {
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(java8Path());
            addHeapLimit(command);
            command.add("-cp");
            command.add(guitarClasspath());
            command.add("edu.umd.cs.guitar.testcase.TestCaseGenerator");
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            long start = System.nanoTime();
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // output not needed
                }
            }
            process.waitFor();
            times[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        java.util.Arrays.sort(times);
        return repeats % 2 == 1 ? times[repeats / 2] : (times[repeats / 2 - 1] + times[repeats / 2]) / 2.0;
    }

    /** Heap limit (GUITAR_XMX, for example 4g) and further JVM options (GUITAR_JVM_FLAGS) of the GUITAR process. */
    private static void addHeapLimit(java.util.List<String> command) {
        String limit = System.getenv("GUITAR_XMX");
        if (limit != null && !limit.trim().isEmpty()) {
            command.add("-Xmx" + limit.trim());
        }
        // Further options of the GUITAR JVM, separated by spaces (GUITAR_JVM_FLAGS).
        String flags = System.getenv("GUITAR_JVM_FLAGS");
        if (flags != null && !flags.trim().isEmpty()) {
            for (String flag : flags.trim().split("\\s+")) {
                command.add(flag);
            }
        }
    }

    public static void generateTestsFromEFG(String efgFilePath, String outputDirectory, int sequenceLength) throws Exception {
    	File efgFileAbsolute = new File(efgFilePath);
    	File outDirAbsolute = new File(outputDirectory);
    	
        File outDir = new File(outputDirectory);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        String projectRoot = System.getProperty("user.dir");
        File libDir = new File(projectRoot, "guitar-libs");
        
        if (!libDir.exists() || !libDir.isDirectory()) {
            throw new RuntimeException("CRITICAL ERROR: GUITAR libs folder not found at: " + libDir.getAbsolutePath());
        }

        StringBuilder cpBuilder = new StringBuilder();
        File[] jars = libDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        
        if (jars != null && jars.length > 0) {
            for (File jar : jars) {
                cpBuilder.append(jar.getAbsolutePath()).append(File.pathSeparator);
            }
        } else {
            throw new RuntimeException("CRITICAL ERROR: No .jar files found in: " + libDir.getAbsolutePath());
        }
        
        String guitarLibPath = cpBuilder.toString();

        String java8Path = "java"; 
        String osName = System.getProperty("os.name").toLowerCase();
        
        String envJava8 = System.getenv("JAVA_8_EXE");
        
        if (envJava8 != null && !envJava8.trim().isEmpty()) {
            java8Path = envJava8;
        } else if (osName.contains("linux")) {
            File linuxJava8 = new File("/usr/lib/jvm/java-8-openjdk-amd64/bin/java");
            if (linuxJava8.exists()) {
                java8Path = linuxJava8.getAbsolutePath();
            }
        } else if (osName.contains("mac")) {
            java8Path = "/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/bin/java"; 
        }
        
        System.out.println("[DEBUG] Java Path: " + java8Path);
        System.out.println("[DEBUG] Classpath: " + guitarLibPath);

        java.util.List<String> command = new java.util.ArrayList<>();
        command.add(java8Path);
        addHeapLimit(command);
        command.add("-cp");
        command.add(guitarLibPath);
        command.add("edu.umd.cs.guitar.testcase.TestCaseGenerator");
        command.add("-p");
        command.add("SequenceLengthCoverage");
        command.add("-e");
        command.add(efgFileAbsolute.getAbsolutePath());
        command.add("-l");
        command.add(String.valueOf(sequenceLength));
        command.add("-d");
        command.add(outDirAbsolute.getAbsolutePath());
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        processBuilder.directory(outDir);
        
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        Thread rssWatcher = watchPeakRss(process);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                 System.out.println("[GUITAR] " + line); 
            }
        }

        int exitCode = process.waitFor();
        if (rssWatcher != null) {
            rssWatcher.interrupt();
        }
        if (exitCode != 0) {
            throw new RuntimeException("GUITAR Test Generation failed with exit code: " + exitCode);
        }
    }
}