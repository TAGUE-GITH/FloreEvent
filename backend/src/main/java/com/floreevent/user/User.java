package com.floreevent.user;

import java.time.Instant;

import com.floreevent.customer.Customer;
import com.floreevent.image.Image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Employe de FloreEvent (le mot de passe est stocke chiffre avec BCrypt). */
@Entity
@Table(name = "app_user")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	/** Photo de profil (facultative). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "avatar_id")
	private Image avatar;

	/** Fiche client des comptes CLIENT (ventes, reparations, formations a son nom). */

	/** Compte client : relie a sa fiche client (reservations, factures). */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@Column(name = "terms_accepted_at")
	private Instant termsAcceptedAt;

	@Column(name = "terms_version")
	private String termsVersion;

	protected User() {
	}

	public User(String email, String passwordHash, String firstName, String lastName, String phone, Role role) {
		this.email = email.trim().toLowerCase();
		this.passwordHash = passwordHash;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phone = phone;
		this.role = role;
	}

	public String getFullName() {
		return firstName + " " + lastName;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	/** Compte de la demonstration publique (adresse fictive). */
	public boolean isDemo() {
		return email.toLowerCase().endsWith(com.floreevent.mail.EmailService.DEMO_DOMAIN);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Image getAvatar() {
		return avatar;
	}

	public void setAvatar(Image avatar) {
		this.avatar = avatar;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	/** Conditions generales et politique de confidentialite acceptees (version lue et date). */
	public void acceptTerms(String version) {
		termsVersion = version;
		termsAcceptedAt = Instant.now();
	}

	public Instant getTermsAcceptedAt() {
		return termsAcceptedAt;
	}

	public String getTermsVersion() {
		return termsVersion;
	}
}
