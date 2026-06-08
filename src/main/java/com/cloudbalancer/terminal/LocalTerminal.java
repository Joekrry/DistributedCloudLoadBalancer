package com.cloudbalancer.terminal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class LocalTerminal {

    private static final List<String> ALLOWED = Arrays.asList(
        "mv", "cp", "ls", "mkdir", "ps", "whoami", "tree", "nano"
    );

    public static String executeCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) return "";

        if (!ALLOWED.contains(parts[0])) {
            return "Command not permitted: " + parts[0]
                + "\nAllowed: " + String.join(", ", ALLOWED);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(parts);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");

            int exit = process.waitFor();
            if (exit != 0) output.append("[exit ").append(exit).append("]");
            return output.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
