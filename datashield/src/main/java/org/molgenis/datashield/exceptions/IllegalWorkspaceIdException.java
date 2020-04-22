package org.molgenis.datashield.exceptions;

import static java.lang.String.format;

public class IllegalWorkspaceIdException extends RuntimeException
{
	public IllegalWorkspaceIdException(String id) {
		super(format("Workspace id: '%s' is not supported.. Please use only letters, numbers, dashes or underscores.", id));
	}

}
