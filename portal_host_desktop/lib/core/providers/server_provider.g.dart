// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'server_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$selectedServerIdHash() => r'26ddf6ada954e53e6e49747021e324a6f051114f';

/// See also [SelectedServerId].
@ProviderFor(SelectedServerId)
final selectedServerIdProvider =
    AutoDisposeNotifierProvider<SelectedServerId, int?>.internal(
      SelectedServerId.new,
      name: r'selectedServerIdProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$selectedServerIdHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$SelectedServerId = AutoDisposeNotifier<int?>;
String _$serverListHash() => r'b6c1a205241c1ab6d2935a603eb8af06834b4ab4';

/// See also [ServerList].
@ProviderFor(ServerList)
final serverListProvider =
    AutoDisposeAsyncNotifierProvider<ServerList, List<ServerConfig>>.internal(
      ServerList.new,
      name: r'serverListProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$serverListHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$ServerList = AutoDisposeAsyncNotifier<List<ServerConfig>>;
String _$activeServerHash() => r'9e74fb86e958f344496bd3e7ee79c481fc5fad4e';

/// Copied from Dart SDK
class _SystemHash {
  _SystemHash._();

  static int combine(int hash, int value) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + value);
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x0007ffff & hash) << 10));
    return hash ^ (hash >> 6);
  }

  static int finish(int hash) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x03ffffff & hash) << 3));
    // ignore: parameter_assignments
    hash = hash ^ (hash >> 11);
    return 0x1fffffff & (hash + ((0x00003fff & hash) << 15));
  }
}

abstract class _$ActiveServer
    extends BuildlessAutoDisposeNotifier<ServerState> {
  late final int serverId;

  ServerState build(int serverId);
}

/// See also [ActiveServer].
@ProviderFor(ActiveServer)
const activeServerProvider = ActiveServerFamily();

/// See also [ActiveServer].
class ActiveServerFamily extends Family<ServerState> {
  /// See also [ActiveServer].
  const ActiveServerFamily();

  /// See also [ActiveServer].
  ActiveServerProvider call(int serverId) {
    return ActiveServerProvider(serverId);
  }

  @override
  ActiveServerProvider getProviderOverride(
    covariant ActiveServerProvider provider,
  ) {
    return call(provider.serverId);
  }

  static const Iterable<ProviderOrFamily>? _dependencies = null;

  @override
  Iterable<ProviderOrFamily>? get dependencies => _dependencies;

  static const Iterable<ProviderOrFamily>? _allTransitiveDependencies = null;

  @override
  Iterable<ProviderOrFamily>? get allTransitiveDependencies =>
      _allTransitiveDependencies;

  @override
  String? get name => r'activeServerProvider';
}

/// See also [ActiveServer].
class ActiveServerProvider
    extends AutoDisposeNotifierProviderImpl<ActiveServer, ServerState> {
  /// See also [ActiveServer].
  ActiveServerProvider(int serverId)
    : this._internal(
        () => ActiveServer()..serverId = serverId,
        from: activeServerProvider,
        name: r'activeServerProvider',
        debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
            ? null
            : _$activeServerHash,
        dependencies: ActiveServerFamily._dependencies,
        allTransitiveDependencies:
            ActiveServerFamily._allTransitiveDependencies,
        serverId: serverId,
      );

  ActiveServerProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.serverId,
  }) : super.internal();

  final int serverId;

  @override
  ServerState runNotifierBuild(covariant ActiveServer notifier) {
    return notifier.build(serverId);
  }

  @override
  Override overrideWith(ActiveServer Function() create) {
    return ProviderOverride(
      origin: this,
      override: ActiveServerProvider._internal(
        () => create()..serverId = serverId,
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        serverId: serverId,
      ),
    );
  }

  @override
  AutoDisposeNotifierProviderElement<ActiveServer, ServerState>
  createElement() {
    return _ActiveServerProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is ActiveServerProvider && other.serverId == serverId;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, serverId.hashCode);

    return _SystemHash.finish(hash);
  }
}

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
mixin ActiveServerRef on AutoDisposeNotifierProviderRef<ServerState> {
  /// The parameter `serverId` of this provider.
  int get serverId;
}

class _ActiveServerProviderElement
    extends AutoDisposeNotifierProviderElement<ActiveServer, ServerState>
    with ActiveServerRef {
  _ActiveServerProviderElement(super.provider);

  @override
  int get serverId => (origin as ActiveServerProvider).serverId;
}

// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
