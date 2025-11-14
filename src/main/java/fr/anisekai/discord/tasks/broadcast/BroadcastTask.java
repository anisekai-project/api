package fr.anisekai.discord.tasks.broadcast;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.validation.JsonObjectRule;
import fr.anisekai.core.internal.plannifier.interfaces.entities.Planifiable;
import fr.anisekai.discord.JDAStore;
import fr.anisekai.library.Library;
import fr.anisekai.server.domain.entities.Anime;
import fr.anisekai.server.services.BroadcastService;
import fr.anisekai.server.tasking.TaskExecutor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

public abstract class BroadcastTask implements TaskExecutor {


    public static final String           OPT_BROADCAST = "broadcast";
    private final       Library          library;
    private final       JDAStore         store;
    private final       BroadcastService service;

    public BroadcastTask(Library library, JDAStore store, BroadcastService service) {

        this.library = library;
        this.store   = store;
        this.service = service;
    }

    public BroadcastService getService() {

        return this.service;
    }

    public JDAStore getStore() {

        return this.store;
    }

    public Guild getGuild() throws Exception {

        return this.getStore()
                   .getGuild()
                   .orElseThrow(() -> new Exception("The Guild is not available."));
    }

    public EventData getEventData(Planifiable<Anime> planifiable) throws Exception {

        return new EventData(this.library, planifiable);
    }

    @Override
    public void validateParams(AnisekaiJson params) {

        params.validate(
                new JsonObjectRule(OPT_BROADCAST, true, int.class, long.class, Integer.class, Long.class)
        );
    }

    public static class EventData {

        private final Library        library;
        public final  String         title;
        public final  String         description;
        public final  Icon           icon;
        public final  OffsetDateTime startTime;
        public final  OffsetDateTime endTime;

        public EventData(Library library, Planifiable<Anime> planifiable) throws Exception {

            this.library     = library;
            this.title       = "Soirée Anime";
            this.description = String.format(
                    "**%s**\nÉpisode(s): %s",
                    planifiable.getWatchTarget().getTitle(),
                    getEpisodeText(planifiable)
            );
            this.icon        = this.getIcon(planifiable);
            this.startTime   = planifiable.getStartingAt().toOffsetDateTime();
            this.endTime     = planifiable.getEndingAt().toOffsetDateTime();
        }

        private Icon getIcon(Planifiable<Anime> planifiable) throws Exception {

            Path path = this.library.getResolver(Library.EVENT_IMAGES).file(planifiable.getWatchTarget());

            if (Files.exists(path)) {
                return Icon.from(path.toFile(), Icon.IconType.PNG);
            }
            return null;
        }

        private static @NotNull String getEpisodeText(Planifiable<?> broadcast) {

            String episodeText;
            if (broadcast.getEpisodeCount() == 1) {
                episodeText = String.format("%02d", broadcast.getFirstEpisode());
            } else if (broadcast.getEpisodeCount() == 2) {
                episodeText = String.format(
                        "%02d et %02d",
                        broadcast.getFirstEpisode(),
                        broadcast.getFirstEpisode() + 1
                );
            } else {
                episodeText = String.format(
                        "%02d à %02d",
                        broadcast.getFirstEpisode(),
                        broadcast.getFirstEpisode() + broadcast.getEpisodeCount() - 1
                );
            }
            return episodeText;
        }

    }

}
