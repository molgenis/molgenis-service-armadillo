package org.molgenis.r.service;

import java.util.List;
import java.util.Set;
import org.molgenis.r.model.RPackage;
import org.rosuda.REngine.Rserve.RConnection;

public interface PackageService {

  List<RPackage> getInstalledPackages(RConnection connection);

  void loadPackages(RConnection connection, Set<String> pkg);
}
