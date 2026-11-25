# Enchantment Reveal

Enchantment Reveal shows the full result of an enchanting-table offer on Minecraft
Java Edition 26.1.2 with Fabric and Java 25.

## Installation

Install Fabric Loader 0.19.3 or newer for Minecraft 26.1.2, Fabric API for 26.1.2,
and the Enchantment Reveal JAR. Use Fabric API 0.150.0+26.1.2 to match the build.

| Where you play | Where to install the JAR and Fabric API |
| --- | --- |
| Singleplayer | Your client, which also runs the integrated server |
| Open to LAN | The host and each guest who wants full previews |
| Dedicated Fabric server | The server and each client who wants full previews |

Clients without Enchantment Reveal can join a server running the mod and see the
normal tooltip. A client with the mod on a stock server or a server without the
matching preview channel also sees the vanilla clue. Vanilla multiplayer syncs
only the low 16 bits of the 32-bit enchantment seed, so the client relies on the
modded server to provide the complete offer.

## Build and verify

Run `./gradlew clean build` with Java 25. The first build downloads the pinned
Gradle distribution and dependencies. The build runs `verifyOfferPreview` and
writes the mod JAR to `build/libs`.

For an in-game regression check, use a level-30 enchanting table and hover all
three rows. Apply an offer and compare the preview with the resulting item,
including any secondary enchantments. Repeat with a book and a tool, swap inputs,
and reopen the table to verify the offers refresh. Repeat as a LAN guest or on a
dedicated server with the mod on both ends. Connect the same client to a stock
server and confirm the standard clue and level or lapis requirements are visible.
On a modded dedicated server, run `reload` from the server console while the
table is open. Check that the tooltip falls back to the clue and that reinserting
the input restores the full offer.
