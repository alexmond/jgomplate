package org.alexmond.jgomplate.functions.ns;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.mindrot.jbcrypt.BCrypt;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code crypto} namespace (hash digests). Reached from templates as
 * {@code crypto.SHA256}, etc. Method names mirror gomplate's Go API (PascalCase, with the
 * underscore form {@code SHA512_224} for the truncated SHA-512 variants).
 *
 * <p>
 * Each {@code SHA*} function returns the lowercase hex digest of the input's bytes,
 * matching gomplate. {@code Bcrypt} and {@code PBKDF2} are also implemented — gomplate
 * declares them variadic, but they are exposed here as fixed-arity overloads (gotmpl4j
 * matches methods by exact parameter count) so the common call shapes work. Still
 * follow-up: the RSA/ECDSA/AES helpers, {@code WPAPSK}, and the {@code *Bytes} digest
 * variants.
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

	/** gomplate {@code crypto.Bcrypt input} — bcrypt hash with the default cost (10). */
	public String Bcrypt(Object input) {
		return BCrypt.hashpw(Values.toString(input), BCrypt.gensalt());
	}

	/** gomplate {@code crypto.Bcrypt cost input} — bcrypt hash with an explicit cost. */
	public String Bcrypt(Object cost, Object input) {
		return BCrypt.hashpw(Values.toString(input), BCrypt.gensalt(Values.toInt(cost)));
	}

	/**
	 * gomplate {@code crypto.PBKDF2 password salt iter keylen} — PBKDF2 derived key
	 * (lowercase hex) using the default HMAC-SHA1, as gomplate does when no hash is
	 * given.
	 * @param password the password bytes
	 * @param salt the salt bytes
	 * @param iter the iteration count
	 * @param keylen the derived-key length in bytes
	 * @return the derived key as lowercase hex
	 */
	public String PBKDF2(Object password, Object salt, Object iter, Object keylen) {
		return pbkdf2(password, salt, iter, keylen, "SHA1");
	}

	/**
	 * gomplate {@code crypto.PBKDF2 password salt iter keylen hash} — PBKDF2 with an
	 * explicit HMAC hash ({@code SHA1}/{@code SHA224}/{@code SHA256}/{@code SHA384}/
	 * {@code SHA512}).
	 * @param password the password bytes
	 * @param salt the salt bytes
	 * @param iter the iteration count
	 * @param keylen the derived-key length in bytes
	 * @param hash the HMAC hash name
	 * @return the derived key as lowercase hex
	 */
	public String PBKDF2(Object password, Object salt, Object iter, Object keylen, Object hash) {
		return pbkdf2(password, salt, iter, keylen, Values.str(hash));
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

	private static String pbkdf2(Object password, Object salt, Object iter, Object keylen, String hashName) {
		String algorithm = "PBKDF2WithHmac" + pbkdf2Hash(hashName);
		try {
			PBEKeySpec spec = new PBEKeySpec(Values.toString(password).toCharArray(), Values.toBytes(salt),
					Values.toInt(iter), Values.toInt(keylen) * 8);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
			return HexFormat.of().formatHex(factory.generateSecret(spec).getEncoded());
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("PBKDF2 failed: " + ex.getMessage(), ex);
		}
	}

	private static String pbkdf2Hash(String name) {
		// gomplate accepts SHA1 / SHA-1 / etc.; normalise to the JCA
		// "PBKDF2WithHmacSHAxxx"
		// suffix.
		String normalized = name.toUpperCase(Locale.ROOT).replace("-", "");
		return switch (normalized) {
			case "SHA1", "SHA224", "SHA256", "SHA384", "SHA512" -> normalized;
			default -> throw new IllegalArgumentException("unsupported PBKDF2 hash: " + name);
		};
	}

}
