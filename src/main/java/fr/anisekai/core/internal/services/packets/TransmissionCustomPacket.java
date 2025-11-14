package fr.anisekai.core.internal.services.packets;

import fr.alexpado.lib.rest.RestAction;
import fr.alexpado.lib.rest.enums.RequestMethod;
import fr.alexpado.lib.rest.interfaces.IRestResponse;
import fr.anisekai.core.internal.json.AnisekaiJson;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Custom implementation of a {@link RestAction} allowing to send a query request to a transmission daemon api.
 */
public class TransmissionCustomPacket extends RestAction<AnisekaiJson> {

    private final String           endpoint;
    private final Supplier<String> sessionSupplier;
    private final AnisekaiJson     json;

    /**
     * Create a new instance of this {@link TransmissionCustomPacket}
     *
     * @param rpc
     *         The URL pointing to the RPC api.
     * @param sessionSupplier
     *         The {@link Supplier} allowing to retrieve the session id.
     * @param json
     *         The {@link AnisekaiJson} to send to the API.
     */
    public TransmissionCustomPacket(String rpc, Supplier<String> sessionSupplier, AnisekaiJson json) {

        this.endpoint        = rpc;
        this.sessionSupplier = sessionSupplier;
        this.json            = json;
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
        headers.put("X-Transmission-Session-Id", this.sessionSupplier.get());
        return headers;
    }

    @Override
    public @NotNull String getRequestBody() {

        return this.json.toString();
    }

    @Override
    public AnisekaiJson convert(IRestResponse response) {

        return new AnisekaiJson(new String(response.getBody()));
    }

}
