package QRAuth.discordqr;

import com.google.gson.JsonParser;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

class PacketWebsocket extends WebSocketClient {
    Consumer<Packet> onPacket;
    Consumer<Integer> onClosed;
    Consumer<Exception> onError;

    public PacketWebsocket(URI uri, Map<String, String> headers, Consumer<Packet> onPacket, Consumer<Integer> onClosed, Consumer<Exception> onError) {
        super(uri, headers);
        this.onPacket = onPacket;
        this.onClosed = onClosed;
        this.onError = onError;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
    }

    @Override
    public void send(String text) {
        super.send(text);
    }

    @Override
    public void onMessage(String message) {
        Packet packet = new Packet(JsonParser.parseString(message).getAsJsonObject());
        onPacket.accept(packet);
    }

    @Override
    public void onError(Exception ex) {
        onError.accept(ex);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        onClosed.accept(code);
    }
}
