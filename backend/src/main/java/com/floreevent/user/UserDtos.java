package com.floreevent.user;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.floreevent.image.Image;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Donnees echangees avec l'API pour les comptes des employes (jamais le mot de passe chiffre). */
public final class UserDtos {

	private UserDtos() {
	}

	/** avatarUrl est absent quand il n'y a pas de photo. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record UserResponse(Long id, String email, String firstName, String lastName, String fullName, String phone,
			Role role, boolean active, Instant createdAt, String avatarUrl) {

		public static UserResponse of(User user) {
			return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
					user.getFullName(), user.getPhone(), user.getRole(), user.isActive(), user.getCreatedAt(),
					Image.url(user.getAvatar()));
		}
	}

	public record CreateUserRequest(
			@NotBlank(message = "L'email est obligatoire.") @Email(message = "Email invalide.") String email,
			@NotBlank(message = "Le prénom est obligatoire.") @Size(max = 80) String firstName,
			@NotBlank(message = "Le nom est obligatoire.") @Size(max = 80) String lastName,
			@Size(max = 30) String phone,
			@NotNull(message = "Choisissez un rôle.") Role role,
			@NotBlank(message = "Le mot de passe est obligatoire.")
			@Size(min = 8, max = 100, message = "Le mot de passe doit contenir au moins 8 caractères.") String password) {
	}

	/** Modification partielle : seuls les champs envoyes changent. */
	public record UpdateUserRequest(
			@Size(min = 1, max = 80) String firstName,
			@Size(min = 1, max = 80) String lastName,
			@Size(max = 30) String phone,
			Role role,
			Boolean active) {
	}
}
