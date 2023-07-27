package org.molgenis.r.service;

import java.util.List;
import java.util.Set;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.model.RPackage;

public interface PackageService {

  List<RPackage> getInstalledPackages(RServerConnection connection);

  void loadPackages(RServerConnection connection, Set<String> pkg);
}
