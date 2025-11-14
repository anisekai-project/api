package fr.anisekai.server.domain.enums;

import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.Selection;

import java.time.Month;
import java.time.ZonedDateTime;

/**
 * Represents a season in which an {@link Anime} has been released, often associated with a year. Used mainly with
 * {@link Selection} to match every {@link Anime} of a particular season.
 */
public enum AnimeSeason {

    /**
     * Season from {@link Month#DECEMBER} to {@link Month#FEBRUARY}.
     */
    WINTER,

    /**
     * Season from {@link Month#MARCH} to {@link Month#MAY}
     */
    SPRING,

    /**
     * Season from {@link Month#JUNE} to {@link Month#AUGUST}
     */
    SUMMER,

    /**
     * Season from {@link Month#SEPTEMBER} to {@link Month#NOVEMBER}
     */
    AUTUMN;

    /**
     * Retrieve the {@link AnimeSeason} matching the provided month.
     *
     * @param date
     *         The {@link ZonedDateTime} from which the {@link AnimeSeason} will be extracted.
     *
     * @return A {@link AnimeSeason}
     */
    public static AnimeSeason fromDate(ZonedDateTime date) {

        Month month = date.getMonth();
        return switch (month) {
            case DECEMBER, JANUARY, FEBRUARY -> WINTER;
            case MARCH, APRIL, MAY -> SPRING;
            case JUNE, JULY, AUGUST -> SUMMER;
            case SEPTEMBER, OCTOBER, NOVEMBER -> AUTUMN;
        };
    }

}
