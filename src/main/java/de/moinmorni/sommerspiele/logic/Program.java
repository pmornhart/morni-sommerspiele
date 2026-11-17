package de.moinmorni.sommerspiele.logic;

import java.util.List;

/** Worüber im Dorf abgestimmt wird: der ganze Wettkampf (vier Disziplinen) oder eine Disziplin allein. */
public enum Program {
    WETTKAMPF("wettkampf", List.of(Discipline.ALMLAUF, Discipline.BOGEN, Discipline.REGATTA, Discipline.SEIFENKISTE)),
    ALMLAUF("almlauf", List.of(Discipline.ALMLAUF)),
    BOGEN("bogen", List.of(Discipline.BOGEN)),
    REGATTA("regatta", List.of(Discipline.REGATTA)),
    SEIFENKISTE("seifenkiste", List.of(Discipline.SEIFENKISTE));

    private final String id;
    private final List<Discipline> disciplines;

    Program(String id, List<Discipline> disciplines) {
        this.id = id;
        this.disciplines = disciplines;
    }

    public String id() {
        return id;
    }

    public List<Discipline> disciplines() {
        return disciplines;
    }

    /** Mehr als eine Disziplin: dann gibt es eine Gesamtwertung nach Punkten. */
    public boolean full() {
        return disciplines.size() > 1;
    }

    /** Programm einer Disziplin. */
    public static Program of(Discipline d) {
        return byId(d.id());
    }

    public static Program byId(String id) {
        if (id == null) {
            return null;
        }
        for (Program p : values()) {
            if (p.id.equalsIgnoreCase(id)) {
                return p;
            }
        }
        Discipline d = Discipline.byId(id);
        return d == null ? null : byId(d.id());
    }
}
