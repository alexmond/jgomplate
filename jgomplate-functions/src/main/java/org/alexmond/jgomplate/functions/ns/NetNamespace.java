package org.alexmond.jgomplate.functions.ns;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.alexmond.jgomplate.functions.Values;

/**
 * gomplate {@code net} namespace — IP-address parsing and CIDR arithmetic. Reached from
 * templates as {@code net.ParseAddr}, {@code net.CIDRHost}, … Method names mirror
 * gomplate's Go API (PascalCase).
 *
 * <p>
 * Only the pure, offline functions are implemented: {@code ParseAddr},
 * {@code ParsePrefix}, {@code ParseRange} and the CIDR helpers {@code CIDRHost},
 * {@code CIDRNetmask}, {@code CIDRSubnets}, {@code CIDRSubnetSizes} (faithful ports of
 * gomplate's go-cidr math). The DNS lookups ({@code LookupIP} etc.) are intentionally
 * omitted — they require a live resolver. gomplate gates the CIDR helpers behind
 * {@code --experimental}; jgomplate makes them available directly. Addresses render in
 * Go's canonical form (RFC 5952 for IPv6).
 */
@SuppressWarnings("PMD.MethodNamingConventions") // method names mirror gomplate's Go API
													// (PascalCase)
public final class NetNamespace {

	/**
	 * gomplate {@code net.ParseAddr ip} — validate an IP and return its canonical form.
	 */
	public String ParseAddr(Object ip) {
		return formatAddr(parseIpBytes(Values.toString(ip)));
	}

	/** gomplate {@code net.ParsePrefix cidr} — validate an {@code ip/bits} prefix. */
	public String ParsePrefix(Object prefix) {
		String s = Values.toString(prefix);
		int slash = s.indexOf('/');
		if (slash < 0) {
			throw new IllegalArgumentException("netip.ParsePrefix(" + s + "): no '/'");
		}
		byte[] bytes = parseIpBytes(s.substring(0, slash));
		int bits = Integer.parseInt(s.substring(slash + 1));
		if (bits < 0 || bits > bytes.length * 8) {
			throw new IllegalArgumentException("netip.ParsePrefix(" + s + "): prefix length out of range");
		}
		return formatAddr(bytes) + "/" + bits;
	}

	/** gomplate {@code net.ParseRange lo-hi} — validate an inclusive address range. */
	public String ParseRange(Object range) {
		String s = Values.toString(range);
		int dash = s.indexOf('-');
		if (dash < 0) {
			throw new IllegalArgumentException("invalid IP range " + s + ": no '-'");
		}
		byte[] lo = parseIpBytes(s.substring(0, dash));
		byte[] hi = parseIpBytes(s.substring(dash + 1));
		return formatAddr(lo) + "-" + formatAddr(hi);
	}

	/**
	 * gomplate {@code net.CIDRHost hostnum prefix} — the hostnum-th address in the
	 * prefix.
	 */
	public String CIDRHost(Object hostnum, Object prefix) {
		Prefix p = parsePrefix(Values.toString(prefix));
		int totalBits = p.base.length * 8;
		int hostLen = totalBits - p.bits;
		BigInteger max = BigInteger.ONE.shiftLeft(hostLen).subtract(BigInteger.ONE);
		BigInteger num = BigInteger.valueOf(Values.toLong(hostnum));
		if (num.signum() < 0) {
			num = max.add(num).add(BigInteger.ONE);
		}
		if (num.compareTo(max) > 0 || num.signum() < 0) {
			throw new IllegalArgumentException("prefix of " + p.bits + " does not accommodate a host numbered " + num);
		}
		BigInteger base = toInt(maskBytes(p.base, p.bits));
		return formatAddr(toBytes(base.or(num), p.base.length));
	}

	/**
	 * gomplate {@code net.CIDRNetmask prefix} — the netmask address for the prefix
	 * length.
	 */
	public String CIDRNetmask(Object prefix) {
		Prefix p = parsePrefix(Values.toString(prefix));
		int totalBits = p.base.length * 8;
		BigInteger mask = topMask(p.bits, totalBits);
		return formatAddr(toBytes(mask, p.base.length));
	}

	/**
	 * gomplate {@code net.CIDRSubnets newbits prefix} — split into 2^newbits equal
	 * subnets.
	 */
	public List<String> CIDRSubnets(Object newbits, Object prefix) {
		Prefix p = parsePrefix(Values.toString(prefix));
		int totalBits = p.base.length * 8;
		int nBits = Values.toInt(newbits);
		if (nBits < 1) {
			throw new IllegalArgumentException("must extend prefix by at least one bit");
		}
		int newPrefixLen = p.bits + nBits;
		if (newPrefixLen > totalBits) {
			throw new IllegalArgumentException("insufficient address space to extend prefix of " + p.bits);
		}
		BigInteger base = toInt(maskBytes(p.base, p.bits));
		long count = 1L << nBits;
		int shift = totalBits - newPrefixLen;
		List<String> out = new ArrayList<>();
		for (long i = 0; i < count; i++) {
			BigInteger subnet = base.or(BigInteger.valueOf(i).shiftLeft(shift));
			out.add(formatAddr(toBytes(subnet, p.base.length)) + "/" + newPrefixLen);
		}
		return out;
	}

	/**
	 * gomplate {@code net.CIDRSubnetSizes newbits… prefix} — pack subnets of varying
	 * sizes.
	 */
	public List<String> CIDRSubnetSizes(Object... args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("wrong number of args: want 2 or more, got " + args.length);
		}
		Prefix network = parsePrefix(Values.toString(args[args.length - 1]));
		int totalBits = network.base.length * 8;
		int[] newbits = new int[args.length - 1];
		for (int i = 0; i < newbits.length; i++) {
			newbits[i] = Values.toInt(args[i]);
		}
		int firstLength = newbits[0] + network.bits;
		Prefix current = previousSubnet(network, firstLength, totalBits);
		List<String> out = new ArrayList<>();
		for (int newbit : newbits) {
			if (newbit < 1) {
				throw new IllegalArgumentException("must extend prefix by at least one bit");
			}
			int length = newbit + network.bits;
			if (length > totalBits) {
				throw new IllegalArgumentException("would extend prefix to " + length + " bits, too long");
			}
			Prefix next = nextSubnet(current, length, totalBits);
			// the new subnet must still fall inside the original network
			BigInteger networkBase = toInt(maskBytes(network.base, network.bits));
			BigInteger nextInNetwork = toInt(maskBytes(next.base, network.bits));
			if (!nextInNetwork.equals(networkBase)) {
				throw new IllegalArgumentException("not enough remaining address space for a prefix of " + length);
			}
			current = next;
			out.add(formatAddr(next.base) + "/" + length);
		}
		return out;
	}

	// --- prefix arithmetic (ports of gomplate's internal/cidr) ---

	private Prefix previousSubnet(Prefix network, int prefixLen, int totalBits) {
		BigInteger base = toInt(maskBytes(network.base, network.bits));
		BigInteger prevIp = base.subtract(BigInteger.ONE);
		BigInteger prevBase = prevIp.and(topMask(prefixLen, totalBits));
		return new Prefix(toBytes(prevBase, network.base.length), prefixLen);
	}

	private Prefix nextSubnet(Prefix current, int prefixLen, int totalBits) {
		BigInteger currentBase = toInt(maskBytes(current.base, current.bits));
		BigInteger currentLast = currentBase.or(hostMask(current.bits, totalBits));
		BigInteger currentSubnetBase = currentLast.and(topMask(prefixLen, totalBits));
		BigInteger last = currentSubnetBase.or(hostMask(prefixLen, totalBits)).add(BigInteger.ONE);
		BigInteger nextBase = last.and(topMask(prefixLen, totalBits));
		return new Prefix(toBytes(nextBase, current.base.length), prefixLen);
	}

	private Prefix parsePrefix(String s) {
		int slash = s.indexOf('/');
		if (slash < 0) {
			throw new IllegalArgumentException("invalid CIDR prefix " + s + ": no '/'");
		}
		byte[] addr = parseIpBytes(s.substring(0, slash));
		int bits = Integer.parseInt(s.substring(slash + 1));
		if (bits < 0 || bits > addr.length * 8) {
			throw new IllegalArgumentException("invalid prefix length in " + s);
		}
		return new Prefix(addr, bits);
	}

	private static BigInteger topMask(int bits, int totalBits) {
		if (bits == 0) {
			return BigInteger.ZERO;
		}
		return BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE).shiftLeft(totalBits - bits);
	}

	private static BigInteger hostMask(int bits, int totalBits) {
		return BigInteger.ONE.shiftLeft(totalBits - bits).subtract(BigInteger.ONE);
	}

	private static byte[] maskBytes(byte[] addr, int bits) {
		int totalBits = addr.length * 8;
		BigInteger masked = toInt(addr).and(topMask(bits, totalBits));
		return toBytes(masked, addr.length);
	}

	private static BigInteger toInt(byte[] bytes) {
		return new BigInteger(1, bytes);
	}

	private static byte[] toBytes(BigInteger value, int len) {
		byte[] out = new byte[len];
		byte[] raw = value.toByteArray();
		int copy = Math.min(raw.length, len);
		for (int i = 0; i < copy; i++) {
			out[len - 1 - i] = raw[raw.length - 1 - i];
		}
		return out;
	}

	// --- IP literal parsing (no DNS) and canonical formatting ---

	private static byte[] parseIpBytes(String s) {
		if (isIpv4Literal(s) || isIpv6Literal(s)) {
			try {
				return InetAddress.getByName(s).getAddress();
			}
			catch (UnknownHostException ex) {
				throw new IllegalArgumentException("invalid IP address: " + s, ex);
			}
		}
		throw new IllegalArgumentException("invalid IP address: " + s);
	}

	private static boolean isIpv4Literal(String s) {
		String[] parts = s.split("\\.", -1);
		if (parts.length != 4) {
			return false;
		}
		for (String part : parts) {
			if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)
					|| Integer.parseInt(part) > 255) {
				return false;
			}
		}
		return true;
	}

	private static boolean isIpv6Literal(String s) {
		return s.indexOf(':') >= 0 && s.chars().allMatch((c) -> Character.digit(c, 16) >= 0 || c == ':');
	}

	private static String formatAddr(byte[] bytes) {
		return (bytes.length == 4) ? formatIpv4(bytes) : formatIpv6(bytes);
	}

	private static String formatIpv4(byte[] b) {
		return (b[0] & 0xff) + "." + (b[1] & 0xff) + "." + (b[2] & 0xff) + "." + (b[3] & 0xff);
	}

	private static String formatIpv6(byte[] b) {
		int[] groups = new int[8];
		for (int i = 0; i < 8; i++) {
			groups[i] = ((b[2 * i] & 0xff) << 8) | (b[2 * i + 1] & 0xff);
		}
		int bestStart = -1;
		int bestLen = 0;
		int runStart = -1;
		int runLen = 0;
		for (int i = 0; i < 8; i++) {
			if (groups[i] == 0) {
				runStart = (runLen == 0) ? i : runStart;
				runLen++;
				if (runLen > bestLen) {
					bestLen = runLen;
					bestStart = runStart;
				}
			}
			else {
				runLen = 0;
			}
		}
		if (bestLen < 2) {
			bestStart = -1;
		}
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < 8; i++) {
			if (i == bestStart) {
				out.append((i == 0) ? "::" : ":");
				i += bestLen - 1;
				continue;
			}
			out.append(Integer.toHexString(groups[i]));
			if (i != 7) {
				out.append(':');
			}
		}
		return out.toString();
	}

	/** A parsed CIDR prefix: the address bytes and the prefix length. */
	private static final class Prefix {

		private final byte[] base;

		private final int bits;

		Prefix(byte[] base, int bits) {
			this.base = base.clone();
			this.bits = bits;
		}

	}

}
