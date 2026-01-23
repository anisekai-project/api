package fr.anisekai.discord.interactions.anime;

import fr.alexpado.interactions.annotations.Completion;
import fr.alexpado.interactions.annotations.Option;
import fr.alexpado.interactions.annotations.Param;
import fr.alexpado.interactions.annotations.Slash;
import fr.anisekai.discord.annotations.DiscordBean;
import fr.anisekai.discord.annotations.RequireAdmin;
import fr.anisekai.discord.completions.AnimeCompletion;
import fr.anisekai.discord.interfaces.InteractionResponse;
import fr.anisekai.discord.responses.DiscordResponse;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.domain.entities.DiscordUser;
import fr.anisekai.server.services.AnimeService;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.regex.Pattern;

@DiscordBean
@RequireAdmin
public class AnimeRegexSlashInteraction {

    //region Error Messages
    private static final String REGEX_MISSING_CAPTURE_GROUP = "La regex doit contenir le groupe de capture `(?<ep>\\\\d+)` qui permet d'extraire le numéro de l'épisode si la détection réussie.";
    private static final String REGEX_SYNTAX_ERROR          = "La regex est invalide. N'hésites pas à tester la regex en utilisant le site [regex101.com](https://regex101.com/).";
    //endregion

    private final AnimeService service;

    public AnimeRegexSlashInteraction(AnimeService service) {

        this.service = service;
    }

    @Slash(
            name = "anime/regex/add",
            description = "\uD83D\uDD12 — Ajoute une regex de titre nyaa.si à un anime.",
            options = {
                    @Option(
                            name = "anime",
                            description = "L'anime à modifier",
                            type = OptionType.INTEGER,
                            required = true,
                            completion = @Completion(named = AnimeCompletion.NAME)
                    ),
                    @Option(
                            name = "regex",
                            description = "Regex permettant de détecter le titre de l'anime sur nyaa.si",
                            type = OptionType.STRING,
                            required = true
                    )
            }
    )
    public InteractionResponse registerRegex(DiscordUser user, @Param("anime") long animeId, @Param("regex") String regex) {

        if (!regex.contains("(?<ep>\\d+)")) {
            return DiscordResponse.error(REGEX_MISSING_CAPTURE_GROUP);
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (Exception e) {
            return DiscordResponse.error(REGEX_SYNTAX_ERROR);
        }

        // TODO: Make title-regex an array.
        Anime anime = this.service.mod(animeId, entity -> entity.setTitleRegex(pattern));
        return DiscordResponse.info("L'anime **%s** a été mis à jour.\nRegex: `%s`", anime.getTitle(), regex);
    }

}

