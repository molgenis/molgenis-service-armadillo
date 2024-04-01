package org.molgenis.armadillo.utils;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyFileReader {

  public String readFile(String filePath) throws IOException, IllegalArgumentException {
    StringBuilder contentBuilder = new StringBuilder();

    try {
      // Get the file as a stream
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);

      // Check if the file was found
      if (inputStream == null) {
        throw new IllegalArgumentException("File not found! " + filePath);
      } else {
        // Use a BufferedReader to read the file
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
          String line;

          // Read the file line by line
          while ((line = reader.readLine()) != null) {
            // Append each line to the StringBuilder
            contentBuilder.append(line).append("\n");
          }
        }
      }
    } catch (IOException e) {
      throw new IOException("File not found on path: " + filePath);
    }

    return contentBuilder.toString();
  }

  public String toJson(List<Map<String, String>> list) {
    Gson gson = new Gson();

    return gson.toJson(list);
  }

  public List<Map<String, String>> parseProperties(String content) {
    List<Map<String, String>> result = new ArrayList<>();
    String[] lines = content.split("\n");
    Map<String, String> property = new HashMap<>();
    StringBuilder description = new StringBuilder();
    String step = "0";
    for (String line : lines) {
      if (line.startsWith("##")) {
        property.put("step", step);
        String comment = line.substring(2).trim();
        if (comment.contains(":")) {
          String[] keyValue = comment.split(":", 2);
          String key = keyValue[0].trim();
          String value = keyValue[1].trim();
          property.put(key, value);
          if (key.equals("step")) {
            step = value;
          }
        } else {
          description.append(comment).append("\\n");
        }
      } else if (line.contains("=")) {
        String[] keyValue = line.split("=", 2);
        property.put("key", keyValue[0].trim());
        property.put("value", keyValue[1].trim());
        property.put("description", description.toString());
        description.setLength(0); // reset description for next property
        System.out.println(property);
        result.add(property);
        property = new HashMap<>(); // reset property for next key-value pairs
      }
    }
    return result;
  }

  String mergeProperyFiles(String source, String template) {
    return source;
  }
}
