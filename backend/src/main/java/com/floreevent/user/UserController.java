package com.floreevent.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.common.ApiException;
import com.floreevent.user.UserDtos.CreateUserRequest;
import com.floreevent.user.UserDtos.UpdateUserRequest;
import com.floreevent.user.UserDtos.UserResponse;

import jakarta.validation.Valid;

/** US-02 : le gerant gere les comptes de ses employes. */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('GERANT')")
public class UserController {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	public UserController(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public List<UserResponse> list() {
		// L'equipe de la boutique, sans les comptes clients du site
		return users.findAllByOrderByActiveDescLastNameAscFirstNameAsc().stream().filter(u -> u.getRole() != Role.CLIENT)
				.map(UserResponse::of).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
		refuseClientRole(request.role());
		if (users.existsByEmailIgnoreCase(request.email().trim()))
			throw ApiException.conflict("Un compte existe déjà avec cet email.");
		User user = new User(request.email(), passwordEncoder.encode(request.password()), request.firstName().trim(),
				request.lastName().trim(), request.phone(), request.role());
		return UserResponse.of(users.save(user));
	}

	@PatchMapping("/{id}")
	@Transactional
	public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
			@AuthenticationPrincipal User current) {
		User user = users.findById(id).orElseThrow(() -> ApiException.notFound("Employé introuvable."));
		// La boutique ne doit jamais rester sans gerant : on ne se retire pas soi-meme ses droits
		boolean self = user.getId().equals(current.getId());
		if (self && (Boolean.FALSE.equals(request.active()) || (request.role() != null && request.role() != Role.GERANT)))
			throw ApiException.badRequest("Vous ne pouvez pas désactiver votre propre compte ni retirer votre rôle de gérant.");
		refuseClientRole(request.role());
		if (user.getRole() == Role.CLIENT) throw ApiException.notFound("Employé introuvable.");
		if (request.firstName() != null) user.setFirstName(request.firstName().trim());
		if (request.lastName() != null) user.setLastName(request.lastName().trim());
		if (request.phone() != null) user.setPhone(request.phone());
		if (request.role() != null) user.setRole(request.role());
		if (request.active() != null) user.setActive(request.active());
		return UserResponse.of(user);
	}

	private static void refuseClientRole(Role role) {
		if (role == Role.CLIENT) throw ApiException.badRequest("Les comptes clients se créent depuis le site.");
	}
}
