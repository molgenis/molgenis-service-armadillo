package org.molgenis.r.service;

import java.util.List;
import org.molgenis.r.model.Package;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public interface PackageService {

  List<Package> getInstalledPackages(RConnection connection)
      throws RserveException, REXPMismatchException;
}
