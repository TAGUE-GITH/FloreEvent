package com.floreevent.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.auth.AuthController.LoginResponse;
import com.floreevent.common.ApiException;
import com.floreevent.common.Terms;
import com.floreevent.customer.Customer;
import com.floreevent.customer.CustomerRepository;
import com.floreevent.security.JwtService;
import com.floreevent.user.Role;
import com.floreevent.user.User;
import com.floreevent.user.UserDtos.UserResponse;
import com.floreevent.user.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** US-06 : inscription d'un client depuis le site ; le compte est relie a une nouvelle fiche client. */
@RestController
public class RegisterController {

	static final String TERMS_REQUIRED = "Vous devez accepter les conditions générales et la politique de confidentialité.";

	public record RegisterRequest(
			@NotBlank(message = "Le prénom est obligatoire.") @Size(max = 80) String firstName,
			@NotBlank(message = "Le nom est obligatoire.") @Size(max = 80) String lastName,
			@NotBlank(message = "L'email est obligatoire.") @Email(message = "Email invalide.") String email,
			@NotBlank(message = "Le téléphone est obligatoire.") @Size(max = 30) String phone,
			@Size(max = 120) String company,
			@NotBlank(message = "Le mot de passe est obligatoire.")
			@Size(min = 8, max = 100, message = "Le mot de passe doit contenir au moins 8 caractères.") String password,
			@NotNull(message = TERMS_REQUIRED) @AssertTrue(message = TERMS_REQUIRED) Boolean acceptTerms) {
	}

	private final UserRepository users;
	private final CustomerRepository customers;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public RegisterController(UserRepository users, CustomerRepository customers, PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.users = users;
		this.customers = customers;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@PostMapping("/api/auth/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Un compte existe déjà avec cet email.");
		String firstName = request.firstName().trim();
		String lastName = request.lastName().trim();
		String company = request.company() == null || request.company().isBlank() ? null : request.company().trim();
		Customer customer = customers.save(new Customer(firstName + " " + lastName, company, request.phone().trim(), email));
		User user = new User(email, passwordEncoder.encode(request.password()), firstName, lastName, customer.getPhone(), Role.CLIENT);
		user.setCustomer(customer);
		user.acceptTerms(Terms.VERSION);
		users.save(user);
		return new LoginResponse(jwtService.generateToken(user.getEmail()), UserResponse.of(user));
	}
}
