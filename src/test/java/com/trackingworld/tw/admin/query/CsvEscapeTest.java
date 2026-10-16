package com.trackingworld.tw.admin.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvEscapeTest {

    @Test
    void quotesSeparatorsAndQuotes() {
        assertThat(QueryService.escapeCsv("a,b")).isEqualTo("\"a,b\"");
        assertThat(QueryService.escapeCsv("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(QueryService.escapeCsv("line\nbreak")).isEqualTo("\"line\nbreak\"");
        assertThat(QueryService.escapeCsv("plain")).isEqualTo("plain");
        assertThat(QueryService.escapeCsv(null)).isEmpty();
    }

    @Test
    void neutralisesFormulasButKeepsPhoneNumbersAndNegatives() {
        assertThat(QueryService.escapeCsv("=1+1")).isEqualTo("'=1+1");
        assertThat(QueryService.escapeCsv("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(QueryService.escapeCsv("+cmd|' /C calc'!A0")).startsWith("'+cmd");
        assertThat(QueryService.escapeCsv("-abc")).isEqualTo("'-abc");
        assertThat(QueryService.escapeCsv("+923001234567")).isEqualTo("+923001234567");
        assertThat(QueryService.escapeCsv("-12.5")).isEqualTo("-12.5");
        assertThat(QueryService.escapeCsv("-.5")).isEqualTo("-.5");
    }
}
