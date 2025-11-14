package fr.anisekai.discord.exceptions.tasks;

import fr.anisekai.core.annotations.FatalTask;
import fr.anisekai.discord.exceptions.SilentDiscordException;
import org.jetbrains.annotations.NotNull;

@FatalTask
public class UndefinedAnnouncementChannelException extends SilentDiscordException {

    public UndefinedAnnouncementChannelException() {

        super("The announcement channel has not been setup correctly.");
    }

    @Override
    public @NotNull String getFriendlyMessage() {

        return "Problème de configuration: Le salon d'annonce pour les animés n'a pas été paramétré correctement.";
    }

}
