package me.flashyreese.mods.greenlight.testmod;

import com.sun.net.httpserver.HttpServer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * End-to-end check of the trust boundary that can't be unit-tested: a real client connects to a
 * dedicated server that pushes a required resource pack carrying the sample policy, and Greenlight
 * must grant the feature (clamped to the server's {@code max}) only because the pack arrived as a
 * genuine {@code PackSource.SERVER} download.
 *
 * <p>Run headless under a virtual display, e.g. {@code xvfb-run ./gradlew :testmod:runGametestClient}.
 */
public class GreenlightClientGametest implements FabricClientGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("greenlight-gametest");
    private static final int EXPECTED_MAX = 16;
    private static final Identifier LOCAL_ONLY_POLICY_RESOURCE =
            Identifier.fromNamespaceAndPath("greenlight-test", "client_features/v1/local_only.json");
    // Connecting, downloading the required pack over HTTP, and the client resource reload it
    // triggers all happen before the world loads. The wait is tick-based, but the client can
    // idle-tick quickly through the budget while a slow reload runs in wall time, so keep this
    // generous because underpowered CI runners reload much slower. It costs nothing on success (the
    // wait returns as soon as the world loads); it only bounds how long a genuine hang runs.
    private static final int WORLD_LOAD_TIMEOUT_TICKS = 6000;

    @Override
    public void runTest(ClientGameTestContext context) {
        acceptEula();

        byte[] packZip = buildSamplePackZip();
        HttpServer packServer = startPackServer(packZip);
        try {
            int packPort = packServer.getAddress().getPort();

            Properties props = new Properties();
            props.setProperty("resource-pack", "http://127.0.0.1:" + packPort + "/pack.zip");
            props.setProperty("resource-pack-sha1", sha1Hex(packZip));
            props.setProperty("require-resource-pack", "true");

            try (TestDedicatedServerContext server = context.worldBuilder().createServer(props)) {
                int serverPort = server.computeOnServer(MinecraftServer::getPort);
                connectAcceptingServerPack(context, serverPort);

                // The applied server pack triggers a resource reload; let it settle before probing.
                context.waitTicks(40);

                boolean granted = context.computeOnClient(client -> GreenlightTestmod.SAMPLE.isAllowed());
                int max = context.computeOnClient(client ->
                        GreenlightTestmod.SAMPLE.policy().map(GreenlightTestmod.SamplePolicy::max).orElse(-1));

                if (!granted) {
                    throw new AssertionError("sample feature was not granted by the required server pack");
                }
                if (max != EXPECTED_MAX) {
                    throw new AssertionError("expected max " + EXPECTED_MAX + " from server policy, got " + max);
                }

                LOGGER.info("PASS: required server pack granted sample feature with max = {}", max);

                assertClientSidePackCannotAuthorize(context, max);

                disconnect(context);
            }
        } finally {
            packServer.stop(0);
        }
    }

    /**
     * The negative half of the trust boundary: this mod ships its own client-side policy resources
     * (a {@code greenlight-test:sample} override with max 9999, and a {@code greenlight-test:local_only}
     * grant), standing in for any client resource pack a player might install. They live in a
     * non-{@code PackSource.SERVER} pack, so Greenlight must ignore them even while a legitimate
     * server grant is active. Runs while still connected to the server from the positive check.
     */
    private static void assertClientSidePackCannotAuthorize(ClientGameTestContext context, int grantedMax) {
        // Guard against a false pass: prove the client-side policy is actually present on the client,
        // so the denial below is the source check at work, not the resource simply being absent.
        boolean clientPolicyPresent = context.computeOnClient(client ->
                !client.getResourceManager().getResourceStack(LOCAL_ONLY_POLICY_RESOURCE).isEmpty());
        if (!clientPolicyPresent) {
            throw new AssertionError("test setup error: client-side policy resource was not loaded on the client");
        }

        if (grantedMax != EXPECTED_MAX) {
            throw new AssertionError("client-side pack overrode the server clamp: max was " + grantedMax);
        }

        boolean localOnlyGranted = context.computeOnClient(client -> GreenlightTestmod.LOCAL_ONLY.isAllowed());
        if (localOnlyGranted) {
            throw new AssertionError("client-side pack granted greenlight-test:local_only, which no server authorized");
        }

        LOGGER.info("PASS: client-side pack ignored (server clamp held at {}, local_only denied)", grantedMax);
    }

    /** Reimplements the harness connect() but pre-accepts the server pack so no prompt blocks us. */
    private static void connectAcceptingServerPack(ClientGameTestContext context, int serverPort) {
        String address = "127.0.0.1:" + serverPort;
        context.runOnClient(client -> {
            ServerData data = new ServerData("greenlight-gametest", address, ServerData.Type.OTHER);
            data.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
            ConnectScreen.startConnecting(new TitleScreen(), client, ServerAddress.parseString(address), data, false, null);
        });
        context.waitFor(client -> client.level != null, WORLD_LOAD_TIMEOUT_TICKS);
    }

    private static void disconnect(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (client.level != null) {
                client.level.disconnect(Component.literal("gametest done"));
            }
            client.disconnectWithSavingScreen();
        });
        context.waitFor(client -> client.level == null);
        // The harness requires a client gametest to finish on the title screen; disconnecting
        // leaves us on the saving/disconnect screen, so return to the title explicitly.
        context.waitTicks(2);
        context.setScreen(TitleScreen::new);
    }

    private static void acceptEula() {
        // The harness boots a real dedicated server, which refuses to start without an agreed EULA.
        // The client JVM's working dir is the run directory, which is also the server's, so writing
        // eula.txt here keeps the test self-contained (no manual step, works locally and in CI).
        try {
            Files.writeString(Path.of("eula.txt"), "eula=true\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] buildSamplePackZip() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            putEntry(zip, "pack.mcmeta", """
                    {
                      "pack": {
                        "description": "Greenlight gametest policy",
                        "min_format": [88, 0],
                        "max_format": [88, 0]
                      }
                    }
                    """);
            putEntry(zip, "assets/greenlight-test/client_features/v1/sample.json", """
                    {
                      "protocol_version": 1,
                      "feature": "greenlight-test:sample",
                      "enabled": true,
                      "settings_version": 1,
                      "settings": { "max": 16 }
                    }
                    """);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static void putEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static HttpServer startPackServer(byte[] packZip) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/pack.zip", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, packZip.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(packZip);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha1Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
