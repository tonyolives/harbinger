package com.harbinger.service.ingest;

import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Cleans a raw, messy street address into a canonical comparison key. It folds case,
 * drops a trailing unit designator ({@code Apt}/{@code Unit}/{@code Ste}/{@code #}),
 * canonicalizes the street-type suffix to one short form, and collapses whitespace.
 * All the address spelling variants the generator emits for one property reduce to a
 * single key (e.g. {@code "123 main st"}).
 *
 * <p>The method is pure and idempotent: already-canonical suffixes map to themselves,
 * so {@code normalize(normalize(x)) == normalize(x)}.
 */
@Component
public class AddressNormalizer {

    /** Strips a trailing unit, e.g. " Apt 4", " Unit 12", " Ste 3", " #7". */
    private static final Pattern UNIT =
            Pattern.compile("\\s+(?:apt|unit|ste|#)\\s*\\d+\\s*$");

    /** Street-type suffix -> canonical short form; canonical forms map to themselves. */
    private static final Map<String, String> SUFFIXES = Map.ofEntries(
            Map.entry("street", "st"), Map.entry("st", "st"),
            Map.entry("avenue", "ave"), Map.entry("ave", "ave"),
            Map.entry("boulevard", "blvd"), Map.entry("blvd", "blvd"),
            Map.entry("road", "rd"), Map.entry("rd", "rd"),
            Map.entry("drive", "dr"), Map.entry("dr", "dr"),
            Map.entry("lane", "ln"), Map.entry("ln", "ln"));

    public String normalize(String rawAddress) {
        if (rawAddress == null) {
            throw new IllegalArgumentException("rawAddress must not be null");
        }

        String address = rawAddress.toLowerCase().trim();
        address = UNIT.matcher(address).replaceAll("");

        // Canonicalize each whole-word street-type token; non-suffix words pass through.
        String[] tokens = address.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(SUFFIXES.getOrDefault(token, token));
        }
        return out.toString();
    }
}
