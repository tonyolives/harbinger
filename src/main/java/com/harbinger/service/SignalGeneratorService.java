package com.harbinger.service;

import com.harbinger.model.RawSignal;
import com.harbinger.model.Source;
import com.harbinger.model.SignalType;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;

/**
 * Produces a deterministic stream of synthetic, deliberately-messy
 * {@link RawSignal}s. Given the same seed it emits exactly the same list, and
 * every signal belonging to one synthetic homeowner shares a {@code trueOwnerId}
 * while carrying different name/address spellings — the noise later phases must
 * normalize and resolve.
 *
 * <p>All randomness flows from a single {@link Random} seeded by the caller, and
 * "now" comes from an injected {@link Clock}, so output is fully reproducible.
 */
@Service
public class SignalGeneratorService {

    /** Window over which a homeowner's signals are spread, ending at the clock's instant. */
    private static final int OBSERVED_WINDOW_SECONDS = 30 * 24 * 60 * 60;

    private static final int NAME_STYLE_COUNT = 6;
    private static final int ADDRESS_STYLE_COUNT = 6;

    /**
     * {@code {formal, nickname}} first-name pairs for hard mode. The two forms are not
     * prefix-similar, so fuzzy matching can't bridge them without a lookup table — exactly
     * the kind of noise that makes a rules-based resolver miss (lowering recall).
     */
    private static final List<String[]> NICKNAMES = List.of(
            new String[] {"Robert", "Bob"},
            new String[] {"William", "Bill"},
            new String[] {"Margaret", "Peggy"},
            new String[] {"Richard", "Dick"},
            new String[] {"James", "Jim"},
            new String[] {"John", "Jack"});

    private final Clock clock;

    public SignalGeneratorService(Clock clock) {
        this.clock = clock;
    }

    /**
     * @param seed            determinism seed; same seed ⇒ identical output
     * @param ownerCount      number of distinct synthetic homeowners (≥ 1)
     * @param signalsPerOwner messy signals emitted per homeowner (≥ 1)
     * @return signals grouped owner-by-owner, each owner's variants sharing one {@code trueOwnerId}
     */
    public List<RawSignal> generate(long seed, int ownerCount, int signalsPerOwner) {
        return generate(seed, ownerCount, signalsPerOwner, false);
    }

    /**
     * Same as {@link #generate(long, int, int)} but optionally toggles <b>hard mode</b>.
     * In hard mode each owner's signals alternate between a formal first name and its
     * nickname (e.g. "Robert" / "Bob"), keeping the same last name and address. Fuzzy
     * matching can't reunite nicknames, so the owner splits into multiple clusters and
     * resolution recall drops below 1.0 — a deliberate, realistic failure case for demos.
     *
     * @param hardMode inject nickname noise the rules-based resolver cannot handle
     */
    public List<RawSignal> generate(long seed, int ownerCount, int signalsPerOwner, boolean hardMode) {
        if (ownerCount < 1) {
            throw new IllegalArgumentException("ownerCount must be at least 1");
        }
        if (signalsPerOwner < 1) {
            throw new IllegalArgumentException("signalsPerOwner must be at least 1");
        }

        Random random = new Random(seed);
        Faker faker = new Faker(random);
        Instant anchor = clock.instant();

        List<RawSignal> signals = new ArrayList<>(ownerCount * signalsPerOwner);
        for (int owner = 0; owner < ownerCount; owner++) {
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String cleanAddress = faker.address().buildingNumber() + " " + faker.address().streetName();

            // Derived from the seeded Random (not UUID.randomUUID, which is unseeded) so it's reproducible.
            UUID trueOwnerId = new UUID(random.nextLong(), random.nextLong());

            List<Integer> nameStyles = shuffledStyles(NAME_STYLE_COUNT, random);
            List<Integer> addressStyles = shuffledStyles(ADDRESS_STYLE_COUNT, random);

            // Guarded so easy mode consumes no extra randomness — its output stays identical.
            String[] nickname = hardMode ? NICKNAMES.get(random.nextInt(NICKNAMES.size())) : null;

            for (int j = 0; j < signalsPerOwner; j++) {
                // Distinct styles per signal (wrapping if asked for more than the style count)
                // guarantee the "messy variants" property regardless of seed.
                String signalFirstName = (nickname == null) ? firstName : nickname[j % 2];
                String messyName = perturbName(signalFirstName, lastName, nameStyles.get(j % NAME_STYLE_COUNT));
                String messyAddress = perturbAddress(cleanAddress, addressStyles.get(j % ADDRESS_STYLE_COUNT), random);
                Source source = Source.values()[random.nextInt(Source.values().length)];
                SignalType signalType = SignalType.values()[random.nextInt(SignalType.values().length)];
                Instant observedAt = anchor.minusSeconds(random.nextInt(OBSERVED_WINDOW_SECONDS));

                signals.add(new RawSignal(messyName, messyAddress, source, signalType, observedAt, trueOwnerId));
            }
        }
        return signals;
    }

    private static List<Integer> shuffledStyles(int count, Random random) {
        List<Integer> styles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            styles.add(i);
        }
        Collections.shuffle(styles, random);
        return styles;
    }

    private static String perturbName(String first, String last, int style) {
        return switch (style) {
            case 1 -> first.charAt(0) + " " + last;          // "J Smith"
            case 2 -> last + ", " + first;                   // "Smith, John"
            case 3 -> (first + " " + last).toLowerCase();    // "john smith"
            case 4 -> (first + " " + last).toUpperCase();    // "JOHN SMITH"
            case 5 -> first.charAt(0) + ". " + last;         // "J. Smith"
            default -> first + " " + last;                   // "John Smith"
        };
    }

    private static String perturbAddress(String cleanAddress, int style, Random random) {
        return switch (style) {
            case 1 -> abbreviateSuffix(cleanAddress);            // "Street" -> "St"
            case 2 -> cleanAddress.toLowerCase();
            case 3 -> cleanAddress.toUpperCase();
            case 4 -> cleanAddress + " Apt " + (random.nextInt(99) + 1);
            case 5 -> cleanAddress.replaceFirst(" ", "  ");      // doubled space
            default -> cleanAddress;
        };
    }

    private static String abbreviateSuffix(String address) {
        return address
                .replace("Street", "St")
                .replace("Avenue", "Ave")
                .replace("Boulevard", "Blvd")
                .replace("Road", "Rd")
                .replace("Drive", "Dr")
                .replace("Lane", "Ln");
    }
}
