package com.eventpro.domain;

import java.security.SecureRandom;

public final class CodeGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    // Alphanumeric without 0, O, 1, I.
    public static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private CodeGenerator() {}

    // prefix + length random characters from ALPHANUMERIC, using SecureRandom.
    public static String randomCode(String prefix, int length) {
        StringBuilder code = new StringBuilder(prefix);

        for (int remaining = length; remaining > 0; remaining--) {
            code.append(CodeGenerator.ALPHANUMERIC.charAt(CodeGenerator.RANDOM.nextInt(CodeGenerator.ALPHANUMERIC.length())));
        }

        return code.toString();
    }
}
