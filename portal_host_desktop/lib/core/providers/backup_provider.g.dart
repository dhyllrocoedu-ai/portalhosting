// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'backup_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$backupManagerHash() => r'55e5bab2db7e6f2de7f70627f41441be7f229971';

/// See also [backupManager].
@ProviderFor(backupManager)
final backupManagerProvider = AutoDisposeProvider<BackupManager>.internal(
  backupManager,
  name: r'backupManagerProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$backupManagerHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef BackupManagerRef = AutoDisposeProviderRef<BackupManager>;
String _$backupListHash() => r'ed55664c4e1e66a398e245eff8ebe129e8a7bb93';

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

/// See also [backupList].
@ProviderFor(backupList)
const backupListProvider = BackupListFamily();

/// See also [backupList].
class BackupListFamily extends Family<AsyncValue<List<BackupEntry>>> {
  /// See also [backupList].
  const BackupListFamily();

  /// See also [backupList].
  BackupListProvider call(int serverId) {
    return BackupListProvider(serverId);
  }

  @override
  BackupListProvider getProviderOverride(
    covariant BackupListProvider provider,
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
  String? get name => r'backupListProvider';
}

/// See also [backupList].
class BackupListProvider extends AutoDisposeFutureProvider<List<BackupEntry>> {
  /// See also [backupList].
  BackupListProvider(int serverId)
    : this._internal(
        (ref) => backupList(ref as BackupListRef, serverId),
        from: backupListProvider,
        name: r'backupListProvider',
        debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
            ? null
            : _$backupListHash,
        dependencies: BackupListFamily._dependencies,
        allTransitiveDependencies: BackupListFamily._allTransitiveDependencies,
        serverId: serverId,
      );

  BackupListProvider._internal(
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
  Override overrideWith(
    FutureOr<List<BackupEntry>> Function(BackupListRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: BackupListProvider._internal(
        (ref) => create(ref as BackupListRef),
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
  AutoDisposeFutureProviderElement<List<BackupEntry>> createElement() {
    return _BackupListProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is BackupListProvider && other.serverId == serverId;
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
mixin BackupListRef on AutoDisposeFutureProviderRef<List<BackupEntry>> {
  /// The parameter `serverId` of this provider.
  int get serverId;
}

class _BackupListProviderElement
    extends AutoDisposeFutureProviderElement<List<BackupEntry>>
    with BackupListRef {
  _BackupListProviderElement(super.provider);

  @override
  int get serverId => (origin as BackupListProvider).serverId;
}

// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
