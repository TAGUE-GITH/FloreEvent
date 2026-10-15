package com.floreevent.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.common.ApiException;
import com.floreevent.image.Image;
import com.floreevent.image.ImageService;
import com.floreevent.user.UserDtos.UserResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Espace personnel de chaque utilisateur (employe ou client) : informations, mot de passe, photo. */
@RestController
@RequestMapping("/api/me")
@Transactional
public class ProfileController {

	public record ProfileRequest(
			@NotBlank(message = "Le prénom est obligatoire.") @Size(max = 80) String firstName,
			@NotBlank(message = "Le nom est obligatoire.") @Size(max = 80) String lastName,
			@Size(max = 30) String phone) {
	}

	public record PasswordRequest(
			@NotBlank(message = "Saisissez votre mot de passe actuel.") String currentPassword,
			@NotBlank(message = "Saisissez le nouveau mot de passe.")
			@Size(min = 8, max = 100, message = "Le mot de passe doit contenir au moins 8 caractères.") String newPassword) {
	}

	public record AvatarRequest(@NotBlank(message = "Choisissez une photo.") String dataUrl) {
	}

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final ImageService images;

	public ProfileController(UserRepository users, PasswordEncoder passwordEncoder, ImageService images) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.images = images;
	}

	/** Le principal vient du jeton : on recharge le compte dans la transaction pour le modifier. */
	private User reload(User principal) {
		return users.findById(principal.getId()).orElseThrow(() -> ApiException.notFound("Compte introuvable."));
	}

	@PatchMapping
	public UserResponse update(@Valid @RequestBody ProfileRequest request, @AuthenticationPrincipal User principal) {
		User user = reload(principal);
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setPhone(request.phone());
		// Un client garde sa fiche client a jour (l'equipe voit son vrai nom et son numero)
		if (user.getCustomer() != null && request.phone() != null && !request.phone().isBlank())
			user.getCustomer().update(user.getFullName(), user.getCustomer().getCompany(), request.phone());
		return UserResponse.of(user);
	}

	@PostMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@Valid @RequestBody PasswordRequest request, @AuthenticationPrincipal User principal) {
		User user = reload(principal);
		if (user.isDemo()) throw ApiException.badRequest("Le mot de passe des comptes de démonstration ne peut pas être changé.");
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash()))
			throw ApiException.badRequest("Le mot de passe actuel est incorrect.");
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
	}

	@PutMapping("/avatar")
	public UserResponse setAvatar(@Valid @RequestBody AvatarRequest request, @AuthenticationPrincipal User principal) {
		User user = reload(principal);
		Image previous = user.getAvatar();
		user.setAvatar(images.save(request.dataUrl()));
		users.flush();
		images.delete(previous);
		return UserResponse.of(user);
	}

	@DeleteMapping("/avatar")
	public UserResponse removeAvatar(@AuthenticationPrincipal User principal) {
		User user = reload(principal);
		Image previous = user.getAvatar();
		user.setAvatar(null);
		users.flush();
		images.delete(previous);
		return UserResponse.of(user);
	}
}
