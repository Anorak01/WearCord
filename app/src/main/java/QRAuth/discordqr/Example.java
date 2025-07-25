package QRAuth.discordqr;

import java.io.IOException;

public class Example {
    /*
    Creates a QR code and waits for the user to scan it
     */
    public static void main(String[] args) throws Exception {
        DiscordQrAuthClient discordAuth = new DiscordQrAuthClient(Throwable::printStackTrace);  // create the client, logging any errors
        discordAuth.getCodeFuture()
            .thenAccept(s -> System.out.println("Got QR code link: https://discordapp.com/ra/" + s));  // print the full url to stdout when we get it
        discordAuth.getCodeScannedFuture()
            .thenAccept(discordUser -> System.out.printf("User %s scanned qr code, waiting for confirmation%n", discordUser));  // print the user who scanned the qr code
        discordAuth.start();  // start the client
        String s1 = discordAuth.getTokenFuture().get();  // wait for the token to arrive
        System.out.println("OK: " + s1); // print the token
    }

    /*
    Logs into an existing QR code
     */
    public static void loginWithQr() throws IOException, InterruptedException {
        DiscordQrAuthUser user = new DiscordQrAuthUser("token-logging-in-here", "qr-code-fingerprint-here", false);
        user.login();
    }
}
