package org.molgenis.r.rserve;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.List;
import org.molgenis.r.RNamedList;
import org.molgenis.r.RServerResult;
import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;

/** Extract result from Rserve java API. */
@VisibleForTesting
public class RserveResult implements RServerResult {

  private final REXP result;

  public RserveResult(REXP result) {
    this.result = result;
  }

  @Override
  public int length() {
    try {
      return result.length();
    } catch (REXPMismatchException e) {
      return -1;
    }
  }

  @Override
  public byte[] asBytes() {
    if (result.isRaw()) {
      try {
        return result.asBytes();
      } catch (REXPMismatchException e) {
        throw new RExecutionException(e);
      }
    }
    return new byte[0];
  }

  @Override
  public boolean isNumeric() {
    return result.isNumeric();
  }

  @Override
  public double[] asDoubles() {
    if (isNumeric()) {
      try {
        return result.asDoubles();
      } catch (REXPMismatchException e) {
        throw new RExecutionException(e);
      }
    }
    return new double[0];
  }

  @Override
  public boolean isInteger() {
    return result.isInteger();
  }

  @Override
  public int[] asIntegers() {
    if (isInteger() || isLogical()) {
      try {
        return result.asIntegers();
      } catch (REXPMismatchException e) {
        throw new RExecutionException(e);
      }
    }
    return new int[0];
  }

  @Override
  public int asInteger() {
    int[] ints = asIntegers();
    if (ints.length == 0) throw new RExecutionException("Not an integer vector");
    return ints[0];
  }

  @Override
  public boolean isLogical() {
    return result.isLogical();
  }

  @Override
  public boolean asLogical() {
    if (result.isLogical()) {
      REXPLogical logical = (REXPLogical) result;
      return logical.length() > 0 && logical.isTRUE()[0];
    }
    return false;
  }

  @Override
  public boolean isNull() {
    return result == null || result.isNull();
  }

  @Override
  public boolean isString() {
    return result.isString();
  }

  @Override
  public String[] asStrings() {
    try {
      return result.asStrings();
    } catch (REXPMismatchException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public boolean isList() {
    return result.isList();
  }

  @Override
  public List<RServerResult> asList() {
    List<RServerResult> rval = Lists.newArrayList();
    if (result.isList()) {
      try {
        for (Object obj : result.asList()) {
          rval.add(new RserveResult((REXP) obj));
        }
      } catch (REXPMismatchException e) {
        throw new RExecutionException(e);
      }
    }
    return rval;
  }

  @Override
  public boolean isNamedList() {
    try {
      return result.isList() && result.asList().isNamed();
    } catch (REXPMismatchException e) {
      return false;
    }
  }

  @Override
  public RNamedList<RServerResult> asNamedList() {
    try {
      return isNamedList() ? new RserveNamedList(result) : new RserveNamedList((REXP) null);
    } catch (REXPMismatchException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public boolean[] isNA() {
    try {
      return result.isNA();
    } catch (REXPMismatchException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public Object asNativeJavaObject() {
    try {
      return result.asNativeJavaObject();
    } catch (REXPMismatchException e) {
      throw new RExecutionException(e);
    }
  }
}
