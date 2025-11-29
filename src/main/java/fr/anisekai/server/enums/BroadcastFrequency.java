package fr.anisekai.server.enums;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

public enum BroadcastFrequency {

    ONCE(null, "Une fois"),
    DAILY(date -> date.plus(1, ChronoUnit.DAYS), "Quotidien"),
    WEEKLY(date -> date.plus(7, ChronoUnit.DAYS), "Hebdomadaire");


    private final Function<Instant, Instant> dateModifier;
    private final String                     displayName;

    BroadcastFrequency(Function<Instant, Instant> dateModifier, String displayName) {

        this.dateModifier = dateModifier;
        this.displayName  = displayName;
    }

    public boolean hasDateModifier() {

        return this.dateModifier != null;
    }

    public Function<Instant, Instant> getDateModifier() {

        return this.dateModifier;
    }

    public String getDisplayName() {

        return this.displayName;
    }

    public static BroadcastFrequency from(String name) {

        if (name == null) return ONCE; // Default value;
        return BroadcastFrequency.valueOf(name.toUpperCase());
    }
}
