package fr.anisekai.discord.exceptions.tasks;

import fr.anisekai.core.annotations.FatalTask;
import fr.anisekai.discord.exceptions.SilentDiscordException;
import org.jetbrains.annotations.NotNull;

@FatalTask
public class UndefinedAnnouncementRoleException extends SilentDiscordException {

    public UndefinedAnnouncementRoleException() {

        super("The announcement role has not been setup correctly.");
    }

    @Override
    public @NotNull String getFriendlyMessage() {

        return "Problème de configuration: Le role d'annonce pour les animés n'a pas été paramétré correctement.";
    }

}
