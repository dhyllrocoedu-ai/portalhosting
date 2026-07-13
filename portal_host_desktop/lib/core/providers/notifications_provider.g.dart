// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'notifications_provider.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$notificationCenterHash() =>
    r'028cc8e64b2d85bf50c39b00562e29c8ba3955f9';

/// See also [NotificationCenter].
@ProviderFor(NotificationCenter)
final notificationCenterProvider =
    AutoDisposeStreamNotifierProvider<
      NotificationCenter,
      List<Notification>
    >.internal(
      NotificationCenter.new,
      name: r'notificationCenterProvider',
      debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
          ? null
          : _$notificationCenterHash,
      dependencies: null,
      allTransitiveDependencies: null,
    );

typedef _$NotificationCenter = AutoDisposeStreamNotifier<List<Notification>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
