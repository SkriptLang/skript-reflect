package com.btk5h.skriptmirror;

/**
 * Exception thrown when the user tries to use a certain class, but that class is not imported.
 * Currently only used in the {@link Descriptor} class.
 */
public class ImportNotFoundException extends Exception {

	private final String userType;

	public ImportNotFoundException(String userType) {
		super("Import not found: " + userType);
		this.userType = userType;
	}

	public String getUserType() {
		return userType;
	}

}
