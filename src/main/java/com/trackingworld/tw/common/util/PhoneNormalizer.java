package com.trackingworld.tw.common.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Optional;

/**
 * Normalises phone numbers to E.164 using libphonenumber with PK as the default region.
 * Accepts 03001234567, 3001234567, 923001234567, +923001234567, 0092 300-1234567 and the same with
 * spaces / dashes / parentheses.
 */
public final class PhoneNormalizer {

    public static final String DEFAULT_REGION = "PK";

    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();

    private PhoneNormalizer() {
    }

    public static Optional<String> normalize(String raw) {
        return normalize(raw, DEFAULT_REGION);
    }

    public static Optional<String> normalize(String raw, String defaultRegion) {
        if (raw == null) {
            return Optional.empty();
        }
        String cleaned = raw.trim();
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        // Strip everything except digits and a leading plus: "(0300) 123-4567" -> "03001234567"
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (c == '+' && digits.length() == 0) {
                digits.append(c);
            }
        }
        String candidate = digits.toString();
        if (candidate.isEmpty() || "+".equals(candidate)) {
            return Optional.empty();
        }
        if (candidate.startsWith("00")) {
            candidate = "+" + candidate.substring(2);
        }
        String regionDigits = defaultRegion == null ? DEFAULT_REGION : defaultRegion;
        if (!candidate.startsWith("+")) {
            int countryCode = UTIL.getCountryCodeForRegion(regionDigits);
            String cc = String.valueOf(countryCode);
            // "923001234567" (country code without plus) -> "+923001234567"
            if (candidate.startsWith(cc) && candidate.length() > cc.length() + 6) {
                Optional<String> asInternational = parse("+" + candidate, regionDigits);
                if (asInternational.isPresent()) {
                    return asInternational;
                }
            }
        }
        return parse(candidate, regionDigits);
    }

    private static Optional<String> parse(String candidate, String region) {
        try {
            Phonenumber.PhoneNumber number = UTIL.parse(candidate, region);
            if (!UTIL.isValidNumber(number)) {
                return Optional.empty();
            }
            return Optional.of(UTIL.format(number, PhoneNumberUtil.PhoneNumberFormat.E164));
        } catch (NumberParseException e) {
            return Optional.empty();
        }
    }
}
