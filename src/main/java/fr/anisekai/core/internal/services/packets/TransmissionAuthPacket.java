package fr.anisekai.core.internal.services.packets;

import fr.alexpado.lib.rest.RestAction;
import fr.alexpado.lib.rest.enums.RequestMethod;
import fr.alexpado.lib.rest.interfaces.IRestResponse;
import fr.anisekai.core.internal.json.AnisekaiJson;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom implementation of a {@link RestAction} allowing to send an authentication request to a transmission daemon
 * api.
 */
public class TransmissionAuthPacket extends RestAction<AnisekaiJson> {

    private final String endpoint;

    /**
     * Create a new instance of this {@link TransmissionAuthPacket}
     *
     * @param rpc
     *         The URL pointing to the RPC api.
     */
    public TransmissionAuthPacket(String rpc) {

        this.endpoint = rpc;
    }

    @Override
    public @NotNull RequestMethod getRequestMethod() {

        return RequestMethod.POST;
    }

    @Override
    public @NotNull String getRequestURL() {

        return this.endpoint;
    }

    @Override
    public @NotNull Map<String, String> getRequestHeaders() {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    @Override
    public @NotNull String getRequestBody() {

        AnisekaiJson packetData = new AnisekaiJson();
        packetData.put("method", "session-get");
        return packetData.toString();
    }

    @Override
    public AnisekaiJson convert(IRestResponse response) {

        return new AnisekaiJson(new String(response.getBody()));
    }

}
