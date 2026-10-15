package com.floreevent.contact;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.floreevent.common.ApiException;
import com.floreevent.mail.EmailService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** US-21 : formulaire de contact du site ; les messages arrivent dans l'espace de gestion et par email. */
@RestController
@Transactional
public class ContactController {

	public record ContactRequest(
			@NotBlank(message = "Indiquez votre nom.") @Size(max = 120, message = "120 caractères maximum.") String name,
			@NotBlank(message = "Indiquez votre email.") @Email(message = "Cet email n'est pas valide.") @Size(max = 160) String email,
			@Size(max = 30, message = "30 caractères maximum.") String phone,
			LocalDate eventDate,
			@NotBlank(message = "Écrivez votre message.") @Size(max = 2000, message = "2000 caractères maximum.") String message) {
	}

	public record ContactResponse(Long id, String name, String email, String phone, LocalDate eventDate, String message, boolean handled,
			Instant createdAt) {

		static ContactResponse of(ContactMessage m) {
			return new ContactResponse(m.getId(), m.getName(), m.getEmail(), m.getPhone(), m.getEventDate(), m.getMessage(), m.isHandled(),
					m.getCreatedAt());
		}
	}

	private final ContactMessageRepository messages;
	private final EmailService emails;
	private final String companyEmail;

	ContactController(ContactMessageRepository messages, EmailService emails, @Value("${app.company.email}") String companyEmail) {
		this.messages = messages;
		this.emails = emails;
		this.companyEmail = companyEmail;
	}

	@PostMapping("/api/contact")
	@ResponseStatus(HttpStatus.CREATED)
	public ContactResponse send(@Valid @RequestBody ContactRequest request) {
		String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
		var message = messages.save(new ContactMessage(request.name().trim(), request.email().trim(), phone, request.eventDate(),
				request.message().trim()));
		emails.send(companyEmail, "Nouveau message de " + message.getName(), "Nouveau message depuis le site :",
				List.of(message.getMessage(), "De : " + message.getName() + " · " + message.getEmail() + (phone == null ? "" : " · " + phone)
						+ (message.getEventDate() == null ? "" : " · événement le " + message.getEventDate())),
				null, null, null);
		return ContactResponse.of(message);
	}

	@GetMapping("/api/contact-messages")
	@PreAuthorize("hasAnyRole('GERANT', 'COMMERCIAL')")
	@Transactional(readOnly = true)
	public List<ContactResponse> list() {
		return messages.findAllByOrderByHandledAscCreatedAtDesc().stream().map(ContactResponse::of).toList();
	}

	@PostMapping("/api/contact-messages/{id}/handled")
	@PreAuthorize("hasAnyRole('GERANT', 'COMMERCIAL')")
	public ContactResponse handled(@PathVariable Long id) {
		ContactMessage message = messages.findById(id).orElseThrow(() -> ApiException.notFound("Message introuvable."));
		message.markHandled();
		return ContactResponse.of(message);
	}
}
