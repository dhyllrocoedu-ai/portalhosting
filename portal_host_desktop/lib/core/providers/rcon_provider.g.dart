// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'rcon_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$rconManagerHash() => r'8b76c1e7d09ad5dae305f1f29e2c3d586f1dd2ca';

/// See also [RconManager].
@ProviderFor(RconManager)
final rconManagerProvider =
    AutoDisposeNotifierProvider<RconManager, Map<int, RconConnection>>.internal(
      RconManager.new,
      name: r'rconManagerProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$rconManagerHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$RconManager = AutoDisposeNotifier<Map<int, RconConnection>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
