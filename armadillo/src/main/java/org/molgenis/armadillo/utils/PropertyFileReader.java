package org.molgenis.armadillo.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyFileReader {

  public String getTemplateString() throws IOException {
    return loadFromClasspath("config.template.properties");
  }

  public String getUserString() throws IOException {
    return loadFromFileSystem("./config.properties");
  }

  public String loadFromClasspath(String classpathLocation) throws IOException {
    return readFile(classpathLocation);
    //    Resource resource = new ClassPathResource(classpathLocation);
    //    ClassLoader.getResourceAsStream(classpathLocation);
    //    if (!resource.exists()) {
    //      throw new IOException("File not found in classpath: " + classpathLocation);
    //    }
    //    Path path = resource.getFile().toPath();
    //    return new String(Files.readAllBytes(path));
  }

  public String loadFromFileSystem(String filePath) throws IOException {
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      throw new IOException("File not found in file system: " + filePath);
    }
    return new String(Files.readAllBytes(path));
  }

  public void writeFile(String content) {
    try (PrintWriter out = new PrintWriter("./config.properties")) {
      out.println(content);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

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

  public List<Map<String, String>> fromJson(String json) {
    Gson gson = new Gson();
    Type type = new TypeToken<List<Map<String, String>>>() {}.getType();
    return gson.fromJson(json, type);
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
