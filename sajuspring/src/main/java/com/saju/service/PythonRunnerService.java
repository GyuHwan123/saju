package com.saju.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.stereotype.Service;

@Service
public class PythonRunnerService implements PythonClient {

    @Override
    public String calculateSaju() throws Exception {
        return runPython("test.py");
    }

    private String runPython(String scriptName) throws Exception {

        ProcessBuilder pb =
                new ProcessBuilder(
                        "python",
                        "../sajupython/" + scriptName
                );

        Process process = pb.start();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                );

        return reader.readLine();
    }
}