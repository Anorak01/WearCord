package QRAuth.discordqr;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * An implementation of the mobile device (the "user"), who is using the authorization mechanism.
 */
public class DiscordQrAuthUser {
    private static final URI BASE_URI;
    private static final URI USERS_BASE_ENDPOINT, REMOTE_AUTH_SCAN, REMOTE_AUTH_FINISH;

    static {
        try {
            // https://discord.com:443/api/v9
            BASE_URI = new URI("https", null, "discord.com", 443, "/api/v9/", null, null);
            USERS_BASE_ENDPOINT = BASE_URI.resolve("users/@me"); // https://discord.com:443/api/v9/users/@me
            REMOTE_AUTH_SCAN = BASE_URI.resolve("users/@me/remote-auth"); // https://discord.com:443/api/v9/users/@me/remote-auth
            REMOTE_AUTH_FINISH = BASE_URI.resolve("users/@me/remote-auth/finish"); // https://discord.com:443/api/v9/users/@me/remote-auth/finish
        } catch (URISyntaxException e) {
            throw new RuntimeException("This should never have happened..?", e);
        }
    }

    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final Gson GSON = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private boolean didScan = false, didLogin = false;
    String token, fingerprint, hsToken;

    /**
     * Creates a new DiscordQrAuthUser
     *
     * @param token            The user token to use for logging into the qr code
     * @param fingerprint      The fingerprint of the qr code's url
     * @param skipVerification Whether to skip token verification or not
     */
    public DiscordQrAuthUser(String token, String fingerprint, boolean skipVerification) {
        if (!skipVerification && !doVerify(token)) {
            throw new IllegalArgumentException("Token " + token + " is not valid");
        }
        this.token = token;
        this.fingerprint = fingerprint;
    }

    private Request.Builder setupReq(Request.Builder r) {
        return r.header("Authorization", this.token)
                .header("User-Agent", "cock dick balling/1.0");
    }

    /**
     * Sends the payload equivalent to first scanning the qr code. Will send basic user information to the qr code owner
     *
     * @throws IOException          When the web request fails
     * @throws InterruptedException When the web request fails
     */
    public void pretendScan() throws IOException, InterruptedException {
        if (didScan) throw new IllegalStateException("Double call to pretendScan()");
        JsonObject jo = new JsonObject();
        jo.addProperty("fingerprint", this.fingerprint);

        RequestBody body = RequestBody.create(GSON.toJson(jo), JSON);
        Request request = setupReq(new Request.Builder().url(REMOTE_AUTH_SCAN.toString()))
                .header("Content-Type", "application/json") // OkHttp typically infers this from RequestBody, but explicit is fine
                .post(body)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.code() == 404) {
                throw new IllegalArgumentException("Remote authentication session expired or is invalid");
            }
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            Structs.HandshakeResponse handshakeResponse = GSON.fromJson(responseBody, Structs.HandshakeResponse.class);
            this.hsToken = handshakeResponse.handshakeToken;
            didScan = true;
        }
    }

    /**
     * Actually logs into the qr code. It's recommended to wait about 2 seconds before calling login() after calling {@link #pretendScan()} if the target QR code is on discord.com/login itself, since the site isn't made to handle scanning the qr code that fast.
     * Will call {@link #pretendScan()}, if it hasn't already been called
     *
     * @throws IOException          When the web request fails
     * @throws InterruptedException When the web request fails
     */
    public void login() throws IOException, InterruptedException {
        if (didLogin) throw new IllegalStateException("Double call to login()");
        if (didLogin) throw new IllegalStateException("Double call to login()");
        if (!didScan) {
            pretendScan(); // This can also throw IOException
        }

        if (this.hsToken == null)
            throw new IllegalStateException("Handshake token is null. This should never happen.");

        JsonObject jo = new JsonObject();
        jo.addProperty("handshake_token", this.hsToken);
        jo.addProperty("temporary_token", false);

        RequestBody body = RequestBody.create(GSON.toJson(jo), JSON);
        Request request = setupReq(new Request.Builder().url(REMOTE_AUTH_FINISH.toString()))
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            // Discord API might not return a body for this, or might return 204 No Content
            // We just care if it was successful.
            if (!response.isSuccessful()) {
                // You might want to log response.body().string() here if available for debugging
                throw new IOException("Login request failed with code: " + response.code());
            }
        }
        didLogin = true;
    }

    /**
     * Creates a new DiscordQrAuthUser with skipVerification set to false
     *
     * @param token       The user token to use for logging into the qr code
     * @param fingerprint The fingerprint of the qr code's url
     */
    public DiscordQrAuthUser(String token, String fingerprint) {
        this(token, fingerprint, false);
    }

    private static boolean doVerify(String token) {
        Request request = new Request.Builder()
                .url(USERS_BASE_ENDPOINT.toString())
                .header("User-Agent", "cock dick balling/1.0") // Consider using a more professional User-Agent
                .header("Authorization", token)
                .build();
        try (Response response = CLIENT.newCall(request).execute()) {
            return response.isSuccessful(); // isSuccessful checks for 2xx status codes
        } catch (IOException e) {
            // Log the exception or handle it as appropriate
            e.printStackTrace(); // Example: print stack trace
            return false;
        }
    }
}
