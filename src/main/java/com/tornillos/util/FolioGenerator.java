package com.tornillos.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class FolioGenerator {
    private static final AtomicInteger counter = new AtomicInteger((int)(System.currentTimeMillis() % 10000));
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String generarEntrada() {
        return "ENT-" + LocalDateTime.now().format(FMT) + "-" + String.format("%04d", counter.incrementAndGet());
    }

    public static String generarSalida() {
        return "SAL-" + LocalDateTime.now().format(FMT) + "-" + String.format("%04d", counter.incrementAndGet());
    }
}
