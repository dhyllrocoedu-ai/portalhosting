import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'tunnel_provider.dart';

class TunnelManager {
  final SharedPreferences _prefs;
  static const String _providerKey = 'tunnel_provider';

  TunnelManager(this._prefs);

  TunnelProvider get currentProvider {
    final value = _prefs.getInt(_providerKey);
    return TunnelProvider.values[value ?? 0];
  }

  Future<void> setProvider(TunnelProvider provider) async {
    await _prefs.setInt(_providerKey, provider.index);
  }

  String getTunnelAddress(int serverPort) {
    final provider = currentProvider;
    switch (provider) {
      case TunnelProvider.playit:
        return 'Connect via playit.gg claim code (check console)';
      case TunnelProvider.cloudflare:
        return 'Configure at dash.cloudflare.com';
      case TunnelProvider.none:
        return 'Local only: localhost:$serverPort';
    }
  }
}

// Provider for SharedPreferences
final sharedPreferencesProvider = FutureProvider<SharedPreferences>((ref) async {
  return await SharedPreferences.getInstance();
});

// Provider for TunnelManager that depends on SharedPreferences
final tunnelManagerProvider = Provider<TunnelManager>((ref) {
  final prefs = ref.watch(sharedPreferencesProvider).value;
  if (prefs == null) {
    throw Exception('SharedPreferences not initialized');
  }
  return TunnelManager(prefs);
});