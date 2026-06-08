package com.harbinger.model;

/**
 * Mock skip-trace contact for a {@link Homeowner}. Deliberately and obviously fake:
 * the phone sits in the reserved fictional {@code 555-0100}–{@code 555-0199} range and
 * the email uses the unresolvable {@code .invalid} TLD. {@code mock} is always
 * {@code true} here — there is no real contact path in this demo, and the flag makes
 * that explicit to anything downstream.
 */
public record ContactInfo(
        String phone,
        String email,
        boolean mock
) {
    public ContactInfo {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phone must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
    }
}
