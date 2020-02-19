package org.molgenis.datashield.exceptions;

import org.rosuda.REngine.Rserve.RserveException;

import java.io.IOException;

public class DatashieldNoSessionException extends RuntimeException
{
	public DatashieldNoSessionException(String message, RserveException err) {
		super(message, err);
	}
}