package tr.edu.iyte.esgfx.testgeneration.guitar;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class GuitarExecutionWrapper {

    public static void generateTestsFromEFG(String efgFilePath, String outputDirectory, int sequenceLength) throws Exception {
        
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

        ProcessBuilder processBuilder = new ProcessBuilder(
                java8Path,
                "-cp", guitarLibPath,
                "edu.umd.cs.guitar.testcase.TestCaseGenerator", 
                "-p", "SequenceLengthCoverage",
                "-e", efgFilePath,
                "-l", String.valueOf(sequenceLength),
                "-d", outputDirectory
        );

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                 System.out.println("[GUITAR] " + line); 
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("GUITAR Test Generation failed with exit code: " + exitCode);
        }
    }
}