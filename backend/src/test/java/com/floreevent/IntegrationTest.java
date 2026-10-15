package com.floreevent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.floreevent.payment.PaymentGateway;
import com.floreevent.quote.Geocoder;
import com.floreevent.security.JwtService;
import com.floreevent.user.Role;
import com.floreevent.user.User;
import com.floreevent.user.UserRepository;

/**
 * Base des tests d'integration : application complete (API, securite, base H2 ou PostgreSQL).
 * Chaque test tourne dans une transaction annulee a la fin : la base reste propre.
 */
// Pas de compte gerant cree au demarrage pendant les tests : chaque test cree ses propres comptes
@SpringBootTest(properties = { "app.bootstrap.email=", "app.stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET })
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTest {

	protected static final String PASSWORD = "motdepasse-solide-2026";
	/** Secret du webhook Stripe des tests : les tests signent les messages comme Stripe. */
	protected static final String WEBHOOK_SECRET = "whsec_test_floreevent";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected UserRepository users;

	/** Localisation des adresses simulee : les tests n'appellent jamais le vrai service de geocodage. */
	@MockitoBean
	protected Geocoder geocoder;

	/** Paiement par carte simule : les tests n'appellent jamais Stripe. */
	@MockitoBean
	protected PaymentGateway gateway;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private int counter;

	protected User createUser(Role role) {
		counter++;
		return createUser(role, role.name().toLowerCase() + counter + "@floreevent.test");
	}

	protected User createUser(Role role, String email) {
		return users.save(new User(email, passwordEncoder.encode(PASSWORD), "Test", role.name(), "690000000", role));
	}

	/** Jeton JWT d'un nouvel employe du role demande. */
	protected String tokenFor(Role role) {
		return tokenFor(createUser(role));
	}

	protected String tokenFor(User user) {
		return jwtService.generateToken(user.getEmail());
	}

	protected ResultActions call(MockHttpServletRequestBuilder request, String token) throws Exception {
		if (token != null) request.header("Authorization", "Bearer " + token);
		return mockMvc.perform(request);
	}

	protected ResultActions getAs(String token, String url, Object... params) throws Exception {
		return call(get(url, params), token);
	}

	protected ResultActions postAs(String token, String url, String json, Object... params) throws Exception {
		return call(post(url, params).contentType(MediaType.APPLICATION_JSON).content(json), token);
	}

	protected ResultActions putAs(String token, String url, String json, Object... params) throws Exception {
		return call(put(url, params).contentType(MediaType.APPLICATION_JSON).content(json), token);
	}

	protected ResultActions patchAs(String token, String url, String json, Object... params) throws Exception {
		return call(patch(url, params).contentType(MediaType.APPLICATION_JSON).content(json), token);
	}

	protected ResultActions deleteAs(String token, String url, Object... params) throws Exception {
		return call(delete(url, params), token);
	}

	/** Identifiant renvoye par une creation (champ « id » de la reponse). */
	protected long idOf(ResultActions result) throws Exception {
		return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

	/** Petite image valide au format demande (« png » ou « jpg »), en data URL comme l'envoie le site. */
	protected static String photo(String format) throws Exception {
		var out = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), format, out);
		return "data:image/" + (format.equals("jpg") ? "jpeg" : format) + ";base64," + Base64.getEncoder().encodeToString(out.toByteArray());
	}
}
