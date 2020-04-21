package org.molgenis.r.service;

import java.util.List;
import org.molgenis.r.model.RPackage;
import org.rosuda.REngine.Rserve.RConnection;

public interface PackageService {

  List<RPackage> getInstalledPackages(RConnection connection);
}
