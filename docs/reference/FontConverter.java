/**
 * Wandelt normalen Text in Unicode-Smallcaps um.
 * <p>
 * Der Konverter läuft auf dem Roh-String, <b>bevor</b> MiniMessage ihn parst.
 * Alles innerhalb eines Tags — von {@code <} bis zum zugehörigen {@code >},
 * quotierte Argumente eingeschlossen — bleibt unverändert. Dadurch überstehen
 * sowohl Formatierungs-Tags als auch Platzhalter die Umwandlung, und
 * Platzhalterwerte wie Spielernamen werden später von MiniMessage unkonvertiert
 * eingesetzt.
 * <p>
 * Groß- und Kleinbuchstaben werden auf dieselben Smallcaps abgebildet, die
 * Groß-/Kleinschreibung des Eingabetexts geht also verloren.
 * <p>
 * Bekannte Einschränkung: Ein {@code <}, dem irgendwo im Resttext ein {@code >}
 * folgt, wird als Tag-Beginn gewertet, auch wenn es als Kleiner-als-Zeichen
 * gemeint war. In Nachrichtentexten sollte {@code <} daher als {@code \<}
 * geschrieben werden.
 */
public final class FontConverter {

    /**
     * Zuordnung a–z auf Smallcaps, indiziert über {@code zeichen - 'a'}.
     * <p>
     * Für s, x und q gibt es in der Minecraft-Schriftart keine brauchbaren
     * Smallcaps, daher stehen hier bewusst Ersatzzeichen.
     */
    private static final char[] SMALL_CAPS = { 'ᴀ', 'ʙ', 'ᴄ', 'ᴅ', 'ᴇ', 'ꜰ', 'ɢ', 'ʜ', 'ɪ', 'ᴊ', 'ᴋ', 'ʟ', 'ᴍ', 'ɴ', 'ᴏ', 'ᴘ', 'ǫ', 'ʀ', 's', 'ᴛ', 'ᴜ', 'ᴠ', 'ᴡ', 'x', 'ʏ', 'ᴢ' };

    private FontConverter() {
    }

    /**
     * Konvertiert den Text eines Nachrichten-Templates in Smallcaps.
     *
     * @param input das Template, roh wie aus der Konfiguration gelesen;
     *              darf {@code null} oder leer sein
     * @return der konvertierte Text, oder die unveränderte Eingabe bei {@code null} oder leerem String
     */
    public static String convert(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder builder = new StringBuilder(input.length());
        boolean inTag = false;
        char quote = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (!inTag) {
                // Escaptes \< ist ein literales Kleiner-als, kein Tag-Beginn
                if (c == '\\' && i + 1 < input.length() && input.charAt(i + 1) == '<') {
                    builder.append('\\').append('<');
                    i++;
                } else if (c == '<' && input.indexOf('>', i + 1) != -1) {
                    inTag = true;
                    builder.append(c);
                } else {
                    builder.append(map(c));
                }
                continue;
            }

            builder.append(c);

            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '>') {
                inTag = false;
            }
        }

        return builder.toString();
    }

    /**
     * Bildet ein einzelnes Zeichen auf sein Smallcaps-Gegenstück ab.
     *
     * @param c das Zeichen
     * @return das Smallcaps-Zeichen für a–z und A–Z, sonst das unveränderte Zeichen — also auch für Umlaute, ß, Ziffern und Satzzeichen
     */
    private static char map(char c) {
        if (c >= 'a' && c <= 'z') {
            return SMALL_CAPS[c - 'a'];
        }
        if (c >= 'A' && c <= 'Z') {
            return SMALL_CAPS[c - 'A'];
        }
        return c;
    }

}
