package com.harbinger.service.ingest;

import org.springframework.stereotype.Component;

/**
 * Cleans a raw, messy person name into a canonical comparison key. It folds case,
 * strips punctuation, reorders {@code "Last, First"} into {@code "First Last"}, and
 * collapses whitespace, so the full-name spelling variants the generator emits
 * reduce to one key (e.g. {@code "john smith"}).
 *
 * <p>It is deliberately conservative: an initial-only form such as {@code "J. Smith"}
 * normalizes to {@code "j smith"} and is not expanded to the full first name —
 * reconciling that with {@code "john smith"} is Phase 3's fuzzy-matching job, not
 * normalization's.
 */
@Component
public class NameNormalizer {

    public String normalize(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("rawName must not be null");
        }

        String name = rawName.trim();

        // "Last, First" -> "First Last": reorder around the first comma so the
        // comma style matches the plain-order styls after the rest of the pipeline.
        int comma = name.indexOf(',');
        if (comma >= 0) {
            String last = name.substring(0, comma);
            String first = name.substring(comma + 1);
            name = first + " " + last;
        }

        return name
                .toLowerCase()
                .replace(".", "")
                .replace(",", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
