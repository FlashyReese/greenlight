# Greenlight test mod

A dev-only Fabric mod that exercises Greenlight end to end. It registers a sample feature
(`greenlight-test:sample`, schema v1, one setting `max`) and logs what the current server
grants each time you join a world. This is the manual counterpart to the unit tests: the
trust boundary (only a real **server** pack grants anything) can't be checked
headless, so it's verified here.

Not published. It bundles the Greenlight API/runtime sources plus the sample consumer.

## Run it

1. `./gradlew :testmod:runClient`
2. Serve `sample-server-pack/` as a server resource pack on a dev server. Zip its
   contents (not the folder), then in `server.properties`:

   ```properties
   resource-pack=http://<host>/sample-server-pack.zip
   resource-pack-sha1=<sha1 of the zip>
   ```

   A quick local host: `cd testmod && python3 -m http.server 8000`, then point
   `resource-pack` at `http://localhost:8000/sample-server-pack.zip`. (`localhost` only
   works when client and server share a machine; use the LAN IP otherwise.)
3. Join the server and watch the client log.

## What to expect

- **On the pack server:** `sample feature GRANTED, max = 16`.
- **On a server without the pack, or in singleplayer:** `sample feature NOT granted
  (default-deny)`.
- **After disconnecting:** the grant is gone; rejoining a plain server logs NOT granted.

## Negative check (trust boundary)

Put the same zip in the client's own `resourcepacks/` folder and enable it while on a
server that does **not** ship it. It must still log NOT granted. A local pack cannot
authorize anything, even with identical JSON.

## Automated end-to-end test

`GreenlightClientGametest` (a Fabric client gametest) automates both halves with no manual
steps: it hosts the sample pack over HTTP in-process, stands up a dedicated server that
pushes it as an optional pack, connects a real client (pre-accepting the pack), and
asserts the sample feature is granted with `max = 16`. It exercises the genuine
`PackSource.SERVER` download path, which the headless unit tests can't.

It also checks the negative case in the same session: this mod ships its own client-side
policy resources (a `greenlight-test:sample` override claiming `max 9999`, and a
`greenlight-test:local_only` grant), standing in for a resource pack a player might install.
The test proves those resources are present on the client yet grant nothing. The server's
clamp holds at 16 and `local_only` stays denied because only a `PackSource.SERVER` pack is
trusted.

It needs a display and a GL driver, so run it headless:

```bash
mkdir -p testmod/run/gametest && echo "eula=true" > testmod/run/gametest/eula.txt
xvfb-run -a ./gradlew :testmod:runGametestClient
```

The `eula.txt` line is required because the test boots a real dedicated server. CI runs
this via `.github/workflows/gametest.yml`.
