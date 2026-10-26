package coachless.data;

public class Names {
    // This class is just a container for constants. It should not be instantiated.
    private Names() {}

    public static String PrettyPrint(String monsterID) {
        int dot = monsterID.indexOf('.');
        String s = dot >= 0 ? monsterID.substring(dot + 1) : monsterID;
        StringBuilder sb = new StringBuilder();
        for (String w : s.split("[_\\-]+")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
