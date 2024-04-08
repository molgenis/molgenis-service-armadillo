package org.molgenis.armadillo.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.molgenis.armadillo.utils.PropertyFileReader;
import org.molgenis.armadillo.utils.Utils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class SettingsService {
  public static final String CONFIG_FILE = "CONFIG_FILE";
  private final Path root = Paths.get("uploads");

  public String fetchSettings() {
    PropertyFileReader propertyFileReader = new PropertyFileReader();
    String content = "nothing found";
    try {
      String template = propertyFileReader.getTemplateString();
      String user = propertyFileReader.getUserString();

      // FIXME: why can I not load existing file?!?
      //      content = propertyFileReader.readFile("./config.properties");
      content = template;
      List<Map<String, String>> props = propertyFileReader.parseProperties(template);
      content = propertyFileReader.toJson(props);
    } catch (IOException | IllegalArgumentException ignored) {
      ignored.printStackTrace();
    }
    return content;
  }

  public void storeSettings(String json) { // throws IOException {
    String result = "# Settings written " + Utils.getServerTime();

    PropertyFileReader propertyFileReader = new PropertyFileReader();
    List<Map<String, String>> list = propertyFileReader.fromJson(json);

    for (Map<String, String> map : list) {
      String key = "";
      String value = "";
      for (Map.Entry<String, String> entry : map.entrySet()) {
        if (entry.getKey().equals("key")) {
          key = entry.getValue();
        }
        if (entry.getKey().equals("value")) {
          value = entry.getValue();
        }
      }
      if (!key.isEmpty() && !value.isEmpty()) {
        result += "\n" + key + "=" + value;
        key = "";
        value = "";
      }
    }
    System.out.println(result);
    propertyFileReader.writeFile(result);
    ArmadilloServiceApplication.restart();
  }
}
