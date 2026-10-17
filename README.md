# CyoriaMOTDGuard

Experimental Folia-compatible status routing guard for CyoriaSMP.

For protocol 774 (Minecraft 1.21.11), the guard leaves BannerMOTD's status handler in place. For other protocols, it removes that handler when present so MiniMOTD can provide the fallback. The client protocol comes from Paper's `StatusClient#getProtocolVersion`; finding the Netty channel still uses reflection into server internals.

## Requirements

- Folia 1.21.11
- MiniMOTD for the fallback status text
- BannerMOTDplus is optional; its handler is used only when that plugin is loaded

The guard does not call BannerMOTDplus APIs. BannerMOTDplus is a soft dependency so its absence or rejection by Folia does not prevent this plugin from loading.

## Build

```sh
mvn --batch-mode clean verify
```

The build creates `target/CyoriaMOTDGuard-${project.version}.jar`. Paper and PacketEvents are compile-time dependencies and are not bundled.

## Validation status

Version 1.0.2-folia.1 was smoke-tested on Folia 1.21.11-14 with PacketEvents 2.13.0 and MiniMOTD 2.2.5, without BannerMOTDplus installed. Version 1.0.3-folia.1 reads the status protocol from Paper's public API, so classification no longer depends on PacketEvents or successful channel lookup. Version 1.0.3-folia.2 calls the public Netty `ChannelPipeline` API reflectively so the plugin compiles against Paper's provided API without bundling Netty.

Real Bedrock login and BannerMOTDplus decoration remain unverified. The connection lookup still uses reflection into server internals and may need adjustment after server changes.
