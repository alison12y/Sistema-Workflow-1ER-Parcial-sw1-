package com.workflow.politicas.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpanishDateExtractionTest {

    @Test
    void extractRange_desdeEl10DeJunioHastaEl12DeJunio() {
        var range = SpanishDateExtraction.extractRange(
                "desde el 10 de junio hasta el 12 de junio"
        );
        assertTrue(range.isPresent());
        assertEquals("2026-06-10", range.get().startIso());
        assertEquals("2026-06-12", range.get().endIso());
    }

    @Test
    void extractRange_del10DeJunioAl12DeJunio() {
        var range = SpanishDateExtraction.extractRange(
                "del 10 de junio al 12 de junio"
        );
        assertTrue(range.isPresent());
        assertEquals("2026-06-10", range.get().startIso());
        assertEquals("2026-06-12", range.get().endIso());
    }

    @Test
    void extractRange_desdeSlashWithYear() {
        var range = SpanishDateExtraction.extractRange(
                "desde 10/06/2026 hasta 12/06/2026"
        );
        assertTrue(range.isPresent());
        assertEquals("2026-06-10", range.get().startIso());
        assertEquals("2026-06-12", range.get().endIso());
    }

    @Test
    void extractRange_delSlashWithoutYear() {
        var range = SpanishDateExtraction.extractRange(
                "del 10/06 al 12/06"
        );
        assertTrue(range.isPresent());
        assertEquals(LocalDateYear() + "-06-10", range.get().startIso());
        assertEquals(LocalDateYear() + "-06-12", range.get().endIso());
    }

    @Test
    void extractRange_iniciaYTermina() {
        var range = SpanishDateExtraction.extractRange(
                "inicia el 10 de junio y termina el 12 de junio"
        );
        assertTrue(range.isPresent());
        assertEquals("2026-06-10", range.get().startIso());
        assertEquals("2026-06-12", range.get().endIso());
    }

    private static String LocalDateYear() {
        return String.valueOf(java.time.LocalDate.now().getYear());
    }
}
