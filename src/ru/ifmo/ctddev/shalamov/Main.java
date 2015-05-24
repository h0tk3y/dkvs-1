package ru.ifmo.ctddev.shalamov;

public class Main {

    public static void main(String[] args) {
        for (int i = 0; i < 3; ++i) {
            new Thread(new Node(i)).start();
        }
    }
}


//starting new process:
//            String javaHome = System.getProperty("java.home");
//            String javaBin = javaHome +
//                    File.separator + "bin" +
//                    File.separator + "java";
//            String classpath = System.getProperty("java.class.path");
//            String className = Node.class.getCanonicalName();
//
//            ProcessBuilder builder = new ProcessBuilder(
//                    javaBin, "-cp", classpath, className);
//
//            try {
//                Process process = builder.start();
//                process.waitFor();
//                System.out.println("code: " + process.exitValue());
//                System.out.println(process.getErrorStream().toString());
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//            }