package org.alexmond.jgomplate.functions.ns;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code crypto} namespace (hash digests). Reached from templates as
 * {@code crypto.SHA256}, etc. Method names mirror gomplate's Go API (PascalCase, with the
 * underscore form {@code SHA512_224} for the truncated SHA-512 variants).
 *
 * <p>
 * Each {@code SHA*} function returns the lowercase hex digest of the input's bytes,
 * matching gomplate. Seed subset: the hash functions only. gomplate's {@code crypto} also
 * has {@code Bcrypt}, {@code PBKDF2}, the RSA/ECDSA/AES helpers, and the {@code *Bytes}
 * digest variants — all follow-up work.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class CryptoNamespace {

	public String SHA1(Object input) {
		return hash("SHA-1", input);
	}

	public String SHA224(Object input) {
		return hash("SHA-224", input);
	}

	public String SHA256(Object input) {
		return hash("SHA-256", input);
	}

	public String SHA384(Object input) {
		return hash("SHA-384", input);
	}

	public String SHA512(Object input) {
		return hash("SHA-512", input);
	}

	public String SHA512_224(Object input) {
		return hash("SHA-512/224", input);
	}

	public String SHA512_256(Object input) {
		return hash("SHA-512/256", input);
	}

	private static String hash(String algorithm, Object input) {
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			return HexFormat.of().formatHex(digest.digest(Values.toBytes(input)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("unsupported hash algorithm: " + algorithm, ex);
		}
	}

}
