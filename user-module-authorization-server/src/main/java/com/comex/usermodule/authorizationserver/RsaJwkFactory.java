package com.comex.usermodule.authorizationserver;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import org.springframework.util.StringUtils;

import com.nimbusds.jose.jwk.RSAKey;

import lombok.extern.slf4j.Slf4j;

/**
 * Builds the RSA signing key of the authorization server from configuration, or generates one.
 */
@Slf4j
final class RsaJwkFactory {

	private static final int GENERATED_KEY_SIZE = 2048;

	private RsaJwkFactory() {
	}

	static RSAKey create(AuthorizationServerProperties.Jwk jwk) {
		KeyPair keyPair;
		if (StringUtils.hasText(jwk.getPrivateKeyPem())) {
			keyPair = load(jwk.getPrivateKeyPem());
		} else {
			log.warn("No {}.jwk.private-key-pem configured: generating an ephemeral RSA key. Tokens issued before a "
				+ "restart will become invalid.", AuthorizationServerProperties.PREFIX);
			keyPair = generate();
		}
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		try {
			String kid = StringUtils.hasText(jwk.getKid())
				? jwk.getKid()
				: new RSAKey.Builder(publicKey).build().computeThumbprint().toString();
			return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(kid).build();
		} catch (Exception e) {
			throw new IllegalStateException("Could not build the authorization server signing key", e);
		}
	}

	private static KeyPair load(String pem) {
		if (pem.contains("BEGIN RSA PRIVATE KEY")) {
			throw new IllegalArgumentException(AuthorizationServerProperties.PREFIX + ".jwk.private-key-pem must be a "
				+ "PKCS#8 key (BEGIN PRIVATE KEY); convert with 'openssl pkcs8 -topk8 -nocrypt'");
		}
		String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyFactory
				.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
			RSAPublicKey publicKey = (RSAPublicKey) keyFactory
				.generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
			return new KeyPair(publicKey, privateKey);
		} catch (Exception e) {
			throw new IllegalArgumentException(
				AuthorizationServerProperties.PREFIX + ".jwk.private-key-pem is not a valid PKCS#8 RSA key", e);
		}
	}

	private static KeyPair generate() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(GENERATED_KEY_SIZE);
			return generator.generateKeyPair();
		} catch (Exception e) {
			throw new IllegalStateException("Could not generate an RSA key", e);
		}
	}
}
