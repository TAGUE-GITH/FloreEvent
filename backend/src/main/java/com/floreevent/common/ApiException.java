package com.floreevent.common;

import org.springframework.http.HttpStatus;

/** Erreur metier avec un message en francais destine a l'utilisateur. */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	/** Champ du formulaire concerne, quand l'erreur porte sur un seul champ. */
	private final String field;

	public ApiException(HttpStatus status, String message) {
		this(status, message, null);
	}

	private ApiException(HttpStatus status, String message, String field) {
		super(message);
		this.status = status;
		this.field = field;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getField() {
		return field;
	}

	/** Regle qui depend de plusieurs champs : l'erreur s'affiche sous le champ concerne, comme une validation. */
	public static ApiException invalidField(String field, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, message, field);
	}

	public static ApiException badRequest(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, message);
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, message);
	}

	public static ApiException conflict(String message) {
		return new ApiException(HttpStatus.CONFLICT, message);
	}

	public static ApiException unauthorized(String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, message);
	}
}
