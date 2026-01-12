package org.telegram.telegrambots.tutorial.Lesson1.src;

public class Format {
    public static String primaLetteraMaiuscola(String input) {
        if (input == null || input.isEmpty()) return "";
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static String getEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) return "🌐";
        int firstChar = Character.codePointAt(countryCode.toUpperCase(), 0) - 0x41 + 0x1F1E6;
        int secondChar = Character.codePointAt(countryCode.toUpperCase(), 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }
}
