package com.cisnebranco.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.cisnebranco.service.ServiceTypeService.generateCode;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceTypeGenerateCodeTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "Banho,                          BANHO",
            "Tosa Tesoura,                   TOSA_TESOURA",
            "Tosa Máquina Lâmina 4,          TOSA_MAQUINA_LAMINA_4",
            "Hidratação,                     HIDRATACAO",
            "Desembolo,                      DESEMBOLO",
            "  Banho  ,                      BANHO",
            "Tosa   ---   Tesoura,           TOSA_TESOURA",
            "Tosa Maquina Lamina Especial,   TOSA_MAQUINA_LAMINA_ESPECIAL",
    })
    void generateCode_knownInputs(String name, String expected) {
        assertThat(generateCode(name.strip())).isEqualTo(expected.strip());
    }

    @Test
    void generateCode_degenerateInput_returnsEmptyString() {
        assertThat(generateCode("!!!")).isEmpty();
        assertThat(generateCode("@#$")).isEmpty();
    }

    @Test
    void generateCode_noLeadingOrTrailingUnderscores() {
        String code = generateCode("4 Tosa");
        assertThat(code).doesNotStartWith("_").doesNotEndWith("_");
    }
}
