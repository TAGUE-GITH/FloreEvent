package com.floreevent.payment;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * R8 : verification de l'en-tete Stripe-Signature (« t=horodatage,v1=signature »). La signature est un HMAC-SHA256 de
 * « horodatage.contenu » avec le secret du webhook ; un message trop ancien est refuse (rejeu).
 */
public final class StripeSignature {

	static final long TOLERANCE_SECONDS = 300;

	private StripeSignature() {
	}

	public static boolean isValid(String payload, String header, String secret, Instant now) {
		if (payload == null || header == null || secret == null || secret.isBlank()) return false;
		Long timestamp = null;
		var signatures = new java.util.ArrayList<String>();
		for (String part : header.split(",")) {
			String[] pair = part.trim().split("=", 2);
			if (pair.length != 2) continue;
			if (pair[0].equals("t")) {
				try {
					timestamp = Long.parseLong(pair[1]);
				} catch (NumberFormatException e) {
					return false;
				}
			} else if (pair[0].equals("v1")) signatures.add(pair[1]);
		}
		if (timestamp == null || signatures.isEmpty()) return false;
		if (Math.abs(now.getEpochSecond() - timestamp) > TOLERANCE_SECONDS) return false;
		byte[] expected = sign(timestamp + "." + payload, secret).getBytes(StandardCharsets.US_ASCII);
		return signatures.stream().anyMatch(s -> MessageDigest.isEqual(expected, s.getBytes(StandardCharsets.US_ASCII)));
	}

	/** Signature hexadecimale (aussi utilisee par les tests pour imiter Stripe). */
	public static String sign(String signedPayload, String secret) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException(e);
		}
	}
}
