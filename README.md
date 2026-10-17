# CyoriaMOTDGuard

Experimental Folia-compatible status routing guard for CyoriaSMP.

For protocol 774 (Minecraft 1.21.11), the guard leaves BannerMOTD's status handler in place. For other protocols, it removes that handler when present so MiniMOTD can provide the fallback. PacketEvents supplies the client protocol version.

## Requirements

- Folia 1.21.11
- PacketEvents 2.13.0 or compatible
- MiniMOTD for the fallback status text
- BannerMOTDplus is optional; its handler is used only when that plugin is loaded

The guard does not call BannerMOTDplus APIs. BannerMOTDplus is a soft dependency so its absence or rejection by Folia does not prevent this plugin from loading.

## Build

```sh
mvn --batch-mode clean verify
```

The build creates `target/CyoriaMOTDGuard-1.0.1-folia.1.jar`. Paper and PacketEvents are compile-time dependencies and are not bundled.

## Validation status

This is a prerelease. Startup and status-ping behavior must be checked on the target Folia build before treating it as production certified. The plugin uses reflective Netty access to inspect the connection and remove the optional handler; server internals may change between versions.
