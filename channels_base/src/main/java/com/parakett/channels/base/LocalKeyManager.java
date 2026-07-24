package com.parakett.channels.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class LocalKeyManager {

    public LocalKeyManager() {

    }

    public String getKeyValue(String key) throws IOException {
        String homeDir = System.getProperty("user.home");
        File file = new File(homeDir + File.separatorChar + ".fskeys");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int index = line.indexOf("=");
                    String lineKey = line.substring(0, index);
                    if (lineKey.equals(key)) {
                        String value = line.substring(index + 1);
                        return value;
                    }
                }
            }

        }

        return null;
    }

    public void writeToKeys(String key, String value) throws IOException {
        StringBuilder sb = new StringBuilder();
        String homeDir = System.getProperty("user.home");
        File file = new File(homeDir + File.separatorChar + ".fskeys");
        if (file.exists()) {
            boolean keyFound = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int index = line.indexOf("=");
                    String lineKey = line.substring(0, index);
                    if (lineKey.equals(key)) {
                        keyFound = true;
                        line = key + "=" + value;
                    }
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(line);
                }
            }
            if (!keyFound) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(key).append("=").append(value);
            }
            // Delete file
            file.delete();
        } else {
            sb.append(key).append("=").append(value);
        }

        // Create file and write to it.
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(sb.toString().getBytes());
        }

    }

}
