package fr.anisekai.web.packets.results;

import org.json.JSONObject;

public class DiscordIdentity {

    private final long   id;
    private final String username;
    private final String discriminator;
    private final String globalName;
    private final String avatar;

    public DiscordIdentity(JSONObject json) {

        this.id            = Long.parseLong(json.getString("id"));
        this.username      = json.getString("username");
        this.discriminator = json.optString("discriminator", null);
        this.globalName    = json.getString("global_name");
        this.avatar        = json.getString("avatar");
    }

    public long getId() {

        return this.id;
    }

    public String getUsername() {

        return this.username;
    }

    public String getDiscriminator() {

        return this.discriminator;
    }

    public String getGlobalName() {

        return this.globalName;
    }

    public String getAvatar() {

        return this.avatar;
    }

    public String getEffectiveAvatarUrl() {

        if (this.avatar != null && !this.avatar.isEmpty()) {
            String extension = this.avatar.startsWith("a_") ? "gif" : "png";
            return String.format("https://cdn.discordapp.com/avatars/%d/%s.%s", this.id, this.avatar, extension);
        }

        long index;
        if (this.discriminator == null) {
            index = (this.id >> 22) % 6;
        } else {
            index = Long.parseLong(this.discriminator) % 5;
        }

        return String.format("https://cdn.discordapp.com/embed/avatars/%d.png", index);
    }

    @Override
    public String toString() {

        return "DiscordIdentity{id=%d, username='%s', discriminator='%s', globalName='%s', avatar='%s'}".formatted(
                this.id,
                this.username,
                this.discriminator,
                this.globalName,
                this.avatar
        );
    }

}
