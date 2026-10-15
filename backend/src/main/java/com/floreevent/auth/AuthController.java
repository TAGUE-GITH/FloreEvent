package com.floreevent.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.common.ApiException;
import com.floreevent.security.JwtService;
import com.floreevent.user.User;
import com.floreevent.user.UserDtos.UserResponse;
import com.floreevent.user.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** US-01 : connexion des employes. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	public record LoginRequest(@NotBlank(message = "L'email est obligatoire.") String email,
			@NotBlank(message = "Le mot de passe est obligatoire.") String password) {
	}

	public record LoginResponse(String token, UserResponse user) {
	}

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		// Meme message que l'email soit inconnu ou le mot de passe faux : on ne revele pas quels comptes existent
		User user = users.findByEmailIgnoreCase(request.email().trim())
				.filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
				.orElseThrow(() -> ApiException.unauthorized("Email ou mot de passe incorrect."));
		if (!user.isActive())
			throw ApiException.unauthorized("Ce compte a été désactivé. Contactez le gérant.");
		return new LoginResponse(jwtService.generateToken(user.getEmail()), UserResponse.of(user));
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal User user) {
		return UserResponse.of(user);
	}
}
