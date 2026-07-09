package org.alexmond.jgomplate.functions.ns;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code base64} namespace. Reached from templates as {@code base64.Encode},
 * {@code base64.Decode}, {@code base64.DecodeBytes}. Method names mirror gomplate's Go
 * API (PascalCase).
 *
 * <p>
 * {@code Encode} uses the standard (padded) base64 alphabet. {@code Decode} tries the
 * standard alphabet first and falls back to the URL-safe alphabet, matching gomplate's
 * {@code base64.Decode}.
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class Base64Namespace {

	/** gomplate {@code base64.Encode in} — standard, padded base64. */
	public String Encode(Object in) {
		return Base64.getEncoder().encodeToString(Values.toBytes(in));
	}

	/**
	 * gomplate {@code base64.Decode in} — decode to a UTF-8 string (std, then URL-safe).
	 */
	public String Decode(Object in) {
		return new String(decode(Values.toString(in)), StandardCharsets.UTF_8);
	}

	/**
	 * gomplate {@code base64.DecodeBytes in} — decode to raw bytes (std, then URL-safe).
	 */
	public byte[] DecodeBytes(Object in) {
		return decode(Values.toString(in));
	}

	private static byte[] decode(String in) {
		try {
			return Base64.getDecoder().decode(in);
		}
		catch (IllegalArgumentException standardFailed) {
			// gomplate falls back to the URL-safe alphabet when the standard one fails
			return Base64.getUrlDecoder().decode(in);
		}
	}

}
