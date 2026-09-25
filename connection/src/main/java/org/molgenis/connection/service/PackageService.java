package org.molgenis.connection.service;

import java.util.List;
import java.util.Set;
import org.molgenis.connection.datashield.RServerConnection;
import org.molgenis.connection.model.RPackage;

public interface PackageService {

  List<RPackage> getInstalledPackages(RServerConnection connection);

  void loadPackages(RServerConnection connection, Set<String> pkg);
}
