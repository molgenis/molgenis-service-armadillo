package org.molgenis.armadillo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.molgenis.armadillo.metadata.FileDetails;
import org.molgenis.armadillo.utils.PropertyFileReader;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class SettingsService {
  public static final String CONFIG_FILE = "CONFIG_FILE";
  private final Path root = Paths.get("uploads");

  public String fetchSettings() {
    PropertyFileReader propertyFileReader = new PropertyFileReader();
    String content = "nothing found";
    try {
      String template = propertyFileReader.readFile("/config.template.properties");

      // FIXME: why can I not load existing file?!?
      //      content = propertyFileReader.readFile("./config.properties");
      content = template;
      List<Map<String, String>> props = propertyFileReader.parseProperties(content);
      content = propertyFileReader.toJson(props);
    } catch (IOException | IllegalArgumentException ignored) {
      ignored.printStackTrace();
    }
    return content;
  }

  public FileDetails storeSettings(MultipartFile file) { // throws IOException {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("Cannot store empty file");
    }

    try {
      if (!Files.exists(root)) {
        Files.createDirectories(root);
      }

      Path resolve = root.resolve(file.getOriginalFilename());
      file.transferTo(resolve);

      // Create and return a FileDetails object
      FileDetails fileDetails =
          FileDetails.create(CONFIG_FILE, CONFIG_FILE, "text/plain", CONFIG_FILE, "", -1, -1);

      //            fileDetails.setFileName(file.getOriginalFilename());
      //            fileDetails.setFileSize(file.getSize());
      //            fileDetails.setFilePath(resolve.toString());

      return fileDetails;
    } catch (IOException e) {
      //            throw new RuntimeException("Failed to store file", e);
    }
    return FileDetails.create(CONFIG_FILE, CONFIG_FILE, "text/plain", CONFIG_FILE, "", -1, -1);
  }
}
