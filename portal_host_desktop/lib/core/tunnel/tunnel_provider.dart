enum TunnelProvider {
  playit,
  cloudflare,
  none,
}

extension TunnelProviderX on TunnelProvider {
  String get displayName {
    switch (this) {
      case TunnelProvider.playit:
        return 'Playit.gg';
      case TunnelProvider.cloudflare:
        return 'Cloudflare Tunnel';
      case TunnelProvider.none:
        return 'None (Local Only)';
    }
  }

  String get description {
    switch (this) {
      case TunnelProvider.playit:
        return 'Easy setup, free, works through NAT. Recommended for most users.';
      case TunnelProvider.cloudflare:
        return 'Requires Cloudflare account. More control, custom domains.';
      case TunnelProvider.none:
        return 'Server only accessible on local network.';
    }
  }

  String get icon {
    switch (this) {
      case TunnelProvider.playit:
        return '🌐';
      case TunnelProvider.cloudflare:
        return '☁️';
      case TunnelProvider.none:
        return '🏠';
    }
  }
}