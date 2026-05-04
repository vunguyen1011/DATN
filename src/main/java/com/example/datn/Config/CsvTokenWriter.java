package com.example.datn.Config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvTokenWriter {

    public static void writeTokens(List<String> tokens, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("jwt_token\n");
            for (String token : tokens) {
                writer.write(token + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendToken(String token, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(token + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}