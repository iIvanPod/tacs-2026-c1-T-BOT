package com.tacs.tp1c2026.session;

/** Se lanza desde una herramienta cuando el usuario no tiene sesión iniciada. */
public class NotLoggedInException extends RuntimeException {

	public NotLoggedInException(String message) {
		super(message);
	}
}
