package de.moinmorni.sommerspiele;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.sommerspiele.logic.Discipline;
import de.moinmorni.sommerspiele.logic.Medal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

class TextsTest {

    /** Formatierungen, die keine Platzhalter sind. */
    private static final Set<String> STYLES = Set.of("gray", "white", "em", "au", "bl", "si", "bz", "red", "green",
            "dark_gray", "b", "bold", "gold", "yellow");
    // Tags wie <name> oder </gray> – aber nicht \<spieler> (maskiert).
    // https://regex101.com/?flavor=java&regex=(%3F%3C!%5C%5C)%3C%2F%3F(%5Ba-z_%5D%2B)%3E&testString=%3Cgray%3EHi%20%3Cname%3E%3C%2Fgray%3E%20%5C%3Cspieler%3E
    private static final Pattern TAG = Pattern.compile("(?<!\\\\)</?([a-z_]+)>");
    // Text-Schlüssel im Quelltext, z. B. "race.finish.msg" – Klänge (sound(…, "minecraft:…")) werden ausgelassen.
    // https://regex101.com/?flavor=java&regex=%22(%5Ba-z%5D%5Ba-z0-9_-%5D*(%3F%3A%5C.%5Ba-z0-9_-%5D%2B)%2B)%22&testString=Texts.msg(lang%2C%20%22race.finish.msg%22)
    private static final Pattern KEY = Pattern.compile("\"([a-z][a-z0-9_-]*(?:\\.[a-z0-9_-]+)+)\"");
    /** Standard-Tags von MiniMessage, die nie Platzhalter heißen dürfen (sonst färbt z. B. „gold“ statt zu ersetzen). */
    private static final Set<String> RESERVED = Set.of("gold", "red", "green", "blue", "aqua", "yellow", "white", "gray",
            "black", "br", "newline", "key", "lang", "score", "font", "color", "reset", "i", "u", "st", "obf", "sprite", "head");

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = TAG.matcher(text);
        while (m.find()) {
            if (!STYLES.contains(m.group(1))) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    @Test
    void everyTextExistsInBothLanguagesWithTheSamePlaceholders() {
        for (String key : Texts.keys()) {
            String de = Texts.raw("de", key);
            String en = Texts.raw("en", key);
            assertFalse(de.isBlank(), key);
            assertFalse(en.isBlank(), key);
            assertEquals(placeholders(de), placeholders(en), "Platzhalter in " + key);
            for (String p : placeholders(de)) {
                assertFalse(RESERVED.contains(p), "Platzhalter heißt wie ein Standard-Tag: " + key + " <" + p + ">");
            }
        }
    }

    @Test
    void everyTextParsesStrictly() {
        MiniMessage strict = MiniMessage.builder().strict(true)
                .editTags(t -> t.resolvers(
                        TagResolver.resolver("em", Tag.styling(TextColor.color(Texts.EMERALD))),
                        TagResolver.resolver("au", Tag.styling(TextColor.color(Texts.GOLD))),
                        TagResolver.resolver("bl", Tag.styling(TextColor.color(Texts.BLUE))),
                        TagResolver.resolver("si", Tag.styling(TextColor.color(Texts.SILVER))),
                        TagResolver.resolver("bz", Tag.styling(TextColor.color(Texts.BRONZE)))))
                .build();
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                TagResolver.Builder resolvers = TagResolver.builder();
                placeholders(text).forEach(p -> resolvers.resolver(Placeholder.unparsed(p, "x")));
                TagResolver all = resolvers.build();
                for (String line : text.split("\n")) {
                    assertDoesNotThrow(() -> strict.deserialize(line, all), lang + " " + key + ": " + line);
                }
            }
        }
    }

    /** Minecraft zeigt nur Zeichen der „Basic Multilingual Plane“ – Emoji wie 🏆 wären ein leeres Kästchen. */
    @Test
    void onlyCharactersMinecraftCanShow() {
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                assertEquals(text.length(), text.codePointCount(0, text.length()), lang + " " + key);
            }
        }
    }

    /** Jeder Schlüssel, den der Code benutzt, gibt es auch – sonst stünde im Spiel nur „race.finish.msg“. */
    @Test
    void everyKeyInTheCodeExists() throws IOException {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String src = Files.readString(f);
                Matcher m = KEY.matcher(src);
                while (m.find()) {
                    String key = m.group(1);
                    String before = src.substring(Math.max(0, m.start() - 60), m.start());
                    String line = before.substring(before.lastIndexOf('\n') + 1);
                    if (line.contains("ound") || line.contains("Key(") || line.contains("createBlockData")
                            || line.contains("info(") || line.contains("warning(") || key.startsWith("minecraft.")
                            || line.contains("c.get") || line.contains("y.set(") || line.contains("y.get")
                            || src.contains("\"" + key + "\" +") || !looksLikeText(key)) {
                        continue;
                    }
                    if (!Texts.keys().contains(key)) {
                        missing.add(f.getFileName() + ": " + key);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Fehlende Texte: " + missing);
    }

    private static boolean looksLikeText(String key) {
        String first = key.substring(0, key.indexOf('.'));
        return Texts.keys().stream().anyMatch(k -> k.startsWith(first + "."));
    }

    /** Schlüssel, die der Code zusammensetzt („discipline." + id …). */
    @Test
    void composedKeysExist() {
        List<String> keys = new ArrayList<>();
        for (Discipline d : Discipline.values()) {
            keys.add("discipline." + d.id());
            keys.add("discipline." + d.id() + ".desc");
        }
        for (Medal m : Medal.values()) {
            keys.add("comp.medal." + m.id());
            keys.add("comp.medal.title." + m.id());
        }
        for (int i = 1; i <= 3; i++) {
            keys.add("comp.podium." + i);
        }
        keys.add("program.wettkampf");
        for (String k : new String[] {"regatta.bar.east", "regatta.bar.west", "bogen.wind.east", "bogen.wind.west"}) {
            keys.add(k);
        }
        for (String k : keys) {
            assertTrue(Texts.keys().contains(k), "fehlt: " + k);
        }
    }

    @Test
    void englishIsReallyEnglish() {
        assertNotEquals(Texts.raw("de", "race.finish.title"), Texts.raw("en", "race.finish.title"));
        assertEquals(Texts.raw("de", "discipline.regatta"), Texts.raw("fr", "discipline.regatta"), "unbekannte Sprache → Deutsch");
        assertEquals("missing.key", Texts.raw("de", "missing.key"));
        assertEquals("Soapbox Race", Texts.raw("en", "discipline.seifenkiste"));
        assertEquals("12 Ringe", Texts.value(Discipline.BOGEN, 12, "de"));
        assertEquals("0:21,50", Texts.value(Discipline.ALMLAUF, 21_500, "de"));
    }

    /** Eigene Farb-Tags gewinnen gegen gleichnamige Standard-Tags (<em> wäre sonst kursiv). */
    @Test
    void ownColourTagsWinOverStandardTags() {
        var c = Texts.MM.deserialize("<em>x</em>");
        var flat = c.children().isEmpty() ? c : c.children().get(0);
        assertEquals(TextColor.color(Texts.EMERALD), flat.color());
        assertNotEquals(TextDecoration.State.TRUE, flat.decoration(TextDecoration.ITALIC), "<em> darf nicht kursiv sein");
        var b = Texts.MM.deserialize("<bz>x</bz>");
        var fb = b.children().isEmpty() ? b : b.children().get(0);
        assertEquals(TextColor.color(Texts.BRONZE), fb.color());
    }

    @Test
    void medalPlaceholdersAreReplaced() {
        var c = Texts.text("de", "sidebar.medals", Placeholder.unparsed("ng", "3"), Placeholder.unparsed("ns", "2"),
                Placeholder.unparsed("nb", "1"));
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
        assertTrue(plain.contains("●3") && plain.contains("●2") && plain.contains("●1"), plain);
    }
}
