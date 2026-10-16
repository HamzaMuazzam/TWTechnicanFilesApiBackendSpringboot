package com.trackingworld.tw.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNormalizerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "03001234567",
            "3001234567",
            "923001234567",
            "+923001234567",
            "0092 300-1234567",
            "+92 (300) 123-4567",
            "0300-123 4567",
            " 03001234567 ",
            "0092-300-1234567",
            "92 300 1234567"
    })
    void normalisesAllAcceptedFormatsToE164(String raw) {
        assertThat(PhoneNormalizer.normalize(raw)).contains("+923001234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "+", "0300", "+92300", "00000000000", "+1 555 0100"})
    @NullAndEmptySource
    void rejectsUnparseableOrInvalidNumbers(String raw) {
        assertThat(PhoneNormalizer.normalize(raw)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"+447911123456", "00447911123456"})
    void keepsForeignNumbersWhenInternationalPrefixPresent(String raw) {
        assertThat(PhoneNormalizer.normalize(raw)).contains("+447911123456");
    }
}
