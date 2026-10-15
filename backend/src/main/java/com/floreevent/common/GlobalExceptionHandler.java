package com.floreevent.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Toutes les erreurs ont la meme forme : {"message": "...", "errors": {"champ": "probleme"}}.
 * Les refus d'acces (403) et l'absence de connexion (401) restent geres par Spring Security.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	public record ApiError(String message, Map<String, String> errors) {
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> business(ApiException exception) {
		if (exception.getField() != null)
			return ResponseEntity.status(exception.getStatus()).body(new ApiError("Certaines informations sont invalides.",
					Map.of(exception.getField(), exception.getMessage())));
		return ResponseEntity.status(exception.getStatus()).body(new ApiError(exception.getMessage(), Map.of()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : exception.getBindingResult().getFieldErrors())
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		return ResponseEntity.badRequest().body(new ApiError("Certaines informations sont invalides.", errors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiError("La requête est mal formée.", Map.of()));
	}
}
