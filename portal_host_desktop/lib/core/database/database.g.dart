// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'database.dart';

// ignore_for_file: type=lint
class $ServersTable extends Servers with TableInfo<$ServersTable, Server> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $ServersTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
    'id',
    aliasedName,
    false,
    hasAutoIncrement: true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'PRIMARY KEY AUTOINCREMENT',
    ),
  );
  static const VerificationMeta _nameMeta = const VerificationMeta('name');
  @override
  late final GeneratedColumn<String> name = GeneratedColumn<String>(
    'name',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _jarPathMeta = const VerificationMeta(
    'jarPath',
  );
  @override
  late final GeneratedColumn<String> jarPath = GeneratedColumn<String>(
    'jar_path',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _portMeta = const VerificationMeta('port');
  @override
  late final GeneratedColumn<int> port = GeneratedColumn<int>(
    'port',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultValue: const Constant(25565),
  );
  static const VerificationMeta _maxPlayersMeta = const VerificationMeta(
    'maxPlayers',
  );
  @override
  late final GeneratedColumn<int> maxPlayers = GeneratedColumn<int>(
    'max_players',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultValue: const Constant(20),
  );
  static const VerificationMeta _serverTypeMeta = const VerificationMeta(
    'serverType',
  );
  @override
  late final GeneratedColumn<String> serverType = GeneratedColumn<String>(
    'server_type',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _mcVersionMeta = const VerificationMeta(
    'mcVersion',
  );
  @override
  late final GeneratedColumn<String> mcVersion = GeneratedColumn<String>(
    'mc_version',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _javaArgsMeta = const VerificationMeta(
    'javaArgs',
  );
  @override
  late final GeneratedColumn<String> javaArgs = GeneratedColumn<String>(
    'java_args',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
    defaultValue: const Constant(''),
  );
  static const VerificationMeta _autoBackupMeta = const VerificationMeta(
    'autoBackup',
  );
  @override
  late final GeneratedColumn<int> autoBackup = GeneratedColumn<int>(
    'auto_backup',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultValue: const Constant(1),
  );
  static const VerificationMeta _autoRestartMeta = const VerificationMeta(
    'autoRestart',
  );
  @override
  late final GeneratedColumn<int> autoRestart = GeneratedColumn<int>(
    'auto_restart',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultValue: const Constant(0),
  );
  static const VerificationMeta _resourcePackUrlMeta = const VerificationMeta(
    'resourcePackUrl',
  );
  @override
  late final GeneratedColumn<String> resourcePackUrl = GeneratedColumn<String>(
    'resource_pack_url',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _resourcePackSha1Meta = const VerificationMeta(
    'resourcePackSha1',
  );
  @override
  late final GeneratedColumn<String> resourcePackSha1 = GeneratedColumn<String>(
    'resource_pack_sha1',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _statusMeta = const VerificationMeta('status');
  @override
  late final GeneratedColumn<String> status = GeneratedColumn<String>(
    'status',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
    defaultValue: const Constant('stopped'),
  );
  static const VerificationMeta _javaPathMeta = const VerificationMeta(
    'javaPath',
  );
  @override
  late final GeneratedColumn<String> javaPath = GeneratedColumn<String>(
    'java_path',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _createdAtMeta = const VerificationMeta(
    'createdAt',
  );
  @override
  late final GeneratedColumn<DateTime> createdAt = GeneratedColumn<DateTime>(
    'created_at',
    aliasedName,
    true,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: false,
  );
  @override
  List<GeneratedColumn> get $columns => [
    id,
    name,
    jarPath,
    port,
    maxPlayers,
    serverType,
    mcVersion,
    javaArgs,
    autoBackup,
    autoRestart,
    resourcePackUrl,
    resourcePackSha1,
    status,
    javaPath,
    createdAt,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'servers';
  @override
  VerificationContext validateIntegrity(
    Insertable<Server> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('name')) {
      context.handle(
        _nameMeta,
        name.isAcceptableOrUnknown(data['name']!, _nameMeta),
      );
    } else if (isInserting) {
      context.missing(_nameMeta);
    }
    if (data.containsKey('jar_path')) {
      context.handle(
        _jarPathMeta,
        jarPath.isAcceptableOrUnknown(data['jar_path']!, _jarPathMeta),
      );
    } else if (isInserting) {
      context.missing(_jarPathMeta);
    }
    if (data.containsKey('port')) {
      context.handle(
        _portMeta,
        port.isAcceptableOrUnknown(data['port']!, _portMeta),
      );
    }
    if (data.containsKey('max_players')) {
      context.handle(
        _maxPlayersMeta,
        maxPlayers.isAcceptableOrUnknown(data['max_players']!, _maxPlayersMeta),
      );
    }
    if (data.containsKey('server_type')) {
      context.handle(
        _serverTypeMeta,
        serverType.isAcceptableOrUnknown(data['server_type']!, _serverTypeMeta),
      );
    } else if (isInserting) {
      context.missing(_serverTypeMeta);
    }
    if (data.containsKey('mc_version')) {
      context.handle(
        _mcVersionMeta,
        mcVersion.isAcceptableOrUnknown(data['mc_version']!, _mcVersionMeta),
      );
    }
    if (data.containsKey('java_args')) {
      context.handle(
        _javaArgsMeta,
        javaArgs.isAcceptableOrUnknown(data['java_args']!, _javaArgsMeta),
      );
    }
    if (data.containsKey('auto_backup')) {
      context.handle(
        _autoBackupMeta,
        autoBackup.isAcceptableOrUnknown(data['auto_backup']!, _autoBackupMeta),
      );
    }
    if (data.containsKey('auto_restart')) {
      context.handle(
        _autoRestartMeta,
        autoRestart.isAcceptableOrUnknown(
          data['auto_restart']!,
          _autoRestartMeta,
        ),
      );
    }
    if (data.containsKey('resource_pack_url')) {
      context.handle(
        _resourcePackUrlMeta,
        resourcePackUrl.isAcceptableOrUnknown(
          data['resource_pack_url']!,
          _resourcePackUrlMeta,
        ),
      );
    }
    if (data.containsKey('resource_pack_sha1')) {
      context.handle(
        _resourcePackSha1Meta,
        resourcePackSha1.isAcceptableOrUnknown(
          data['resource_pack_sha1']!,
          _resourcePackSha1Meta,
        ),
      );
    }
    if (data.containsKey('status')) {
      context.handle(
        _statusMeta,
        status.isAcceptableOrUnknown(data['status']!, _statusMeta),
      );
    }
    if (data.containsKey('java_path')) {
      context.handle(
        _javaPathMeta,
        javaPath.isAcceptableOrUnknown(data['java_path']!, _javaPathMeta),
      );
    }
    if (data.containsKey('created_at')) {
      context.handle(
        _createdAtMeta,
        createdAt.isAcceptableOrUnknown(data['created_at']!, _createdAtMeta),
      );
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  Server map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return Server(
      id: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}id'],
      )!,
      name: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}name'],
      )!,
      jarPath: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}jar_path'],
      )!,
      port: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}port'],
      )!,
      maxPlayers: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}max_players'],
      )!,
      serverType: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}server_type'],
      )!,
      mcVersion: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}mc_version'],
      ),
      javaArgs: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}java_args'],
      )!,
      autoBackup: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}auto_backup'],
      )!,
      autoRestart: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}auto_restart'],
      )!,
      resourcePackUrl: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}resource_pack_url'],
      ),
      resourcePackSha1: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}resource_pack_sha1'],
      ),
      status: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}status'],
      )!,
      javaPath: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}java_path'],
      ),
      createdAt: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}created_at'],
      ),
    );
  }

  @override
  $ServersTable createAlias(String alias) {
    return $ServersTable(attachedDatabase, alias);
  }
}

class Server extends DataClass implements Insertable<Server> {
  final int id;
  final String name;
  final String jarPath;
  final int port;
  final int maxPlayers;
  final String serverType;
  final String? mcVersion;
  final String javaArgs;
  final int autoBackup;
  final int autoRestart;
  final String? resourcePackUrl;
  final String? resourcePackSha1;
  final String status;
  final String? javaPath;
  final DateTime? createdAt;
  const Server({
    required this.id,
    required this.name,
    required this.jarPath,
    required this.port,
    required this.maxPlayers,
    required this.serverType,
    this.mcVersion,
    required this.javaArgs,
    required this.autoBackup,
    required this.autoRestart,
    this.resourcePackUrl,
    this.resourcePackSha1,
    required this.status,
    this.javaPath,
    this.createdAt,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['name'] = Variable<String>(name);
    map['jar_path'] = Variable<String>(jarPath);
    map['port'] = Variable<int>(port);
    map['max_players'] = Variable<int>(maxPlayers);
    map['server_type'] = Variable<String>(serverType);
    if (!nullToAbsent || mcVersion != null) {
      map['mc_version'] = Variable<String>(mcVersion);
    }
    map['java_args'] = Variable<String>(javaArgs);
    map['auto_backup'] = Variable<int>(autoBackup);
    map['auto_restart'] = Variable<int>(autoRestart);
    if (!nullToAbsent || resourcePackUrl != null) {
      map['resource_pack_url'] = Variable<String>(resourcePackUrl);
    }
    if (!nullToAbsent || resourcePackSha1 != null) {
      map['resource_pack_sha1'] = Variable<String>(resourcePackSha1);
    }
    map['status'] = Variable<String>(status);
    if (!nullToAbsent || javaPath != null) {
      map['java_path'] = Variable<String>(javaPath);
    }
    if (!nullToAbsent || createdAt != null) {
      map['created_at'] = Variable<DateTime>(createdAt);
    }
    return map;
  }

  ServersCompanion toCompanion(bool nullToAbsent) {
    return ServersCompanion(
      id: Value(id),
      name: Value(name),
      jarPath: Value(jarPath),
      port: Value(port),
      maxPlayers: Value(maxPlayers),
      serverType: Value(serverType),
      mcVersion: mcVersion == null && nullToAbsent
          ? const Value.absent()
          : Value(mcVersion),
      javaArgs: Value(javaArgs),
      autoBackup: Value(autoBackup),
      autoRestart: Value(autoRestart),
      resourcePackUrl: resourcePackUrl == null && nullToAbsent
          ? const Value.absent()
          : Value(resourcePackUrl),
      resourcePackSha1: resourcePackSha1 == null && nullToAbsent
          ? const Value.absent()
          : Value(resourcePackSha1),
      status: Value(status),
      javaPath: javaPath == null && nullToAbsent
          ? const Value.absent()
          : Value(javaPath),
      createdAt: createdAt == null && nullToAbsent
          ? const Value.absent()
          : Value(createdAt),
    );
  }

  factory Server.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return Server(
      id: serializer.fromJson<int>(json['id']),
      name: serializer.fromJson<String>(json['name']),
      jarPath: serializer.fromJson<String>(json['jarPath']),
      port: serializer.fromJson<int>(json['port']),
      maxPlayers: serializer.fromJson<int>(json['maxPlayers']),
      serverType: serializer.fromJson<String>(json['serverType']),
      mcVersion: serializer.fromJson<String?>(json['mcVersion']),
      javaArgs: serializer.fromJson<String>(json['javaArgs']),
      autoBackup: serializer.fromJson<int>(json['autoBackup']),
      autoRestart: serializer.fromJson<int>(json['autoRestart']),
      resourcePackUrl: serializer.fromJson<String?>(json['resourcePackUrl']),
      resourcePackSha1: serializer.fromJson<String?>(json['resourcePackSha1']),
      status: serializer.fromJson<String>(json['status']),
      javaPath: serializer.fromJson<String?>(json['javaPath']),
      createdAt: serializer.fromJson<DateTime?>(json['createdAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'name': serializer.toJson<String>(name),
      'jarPath': serializer.toJson<String>(jarPath),
      'port': serializer.toJson<int>(port),
      'maxPlayers': serializer.toJson<int>(maxPlayers),
      'serverType': serializer.toJson<String>(serverType),
      'mcVersion': serializer.toJson<String?>(mcVersion),
      'javaArgs': serializer.toJson<String>(javaArgs),
      'autoBackup': serializer.toJson<int>(autoBackup),
      'autoRestart': serializer.toJson<int>(autoRestart),
      'resourcePackUrl': serializer.toJson<String?>(resourcePackUrl),
      'resourcePackSha1': serializer.toJson<String?>(resourcePackSha1),
      'status': serializer.toJson<String>(status),
      'javaPath': serializer.toJson<String?>(javaPath),
      'createdAt': serializer.toJson<DateTime?>(createdAt),
    };
  }

  Server copyWith({
    int? id,
    String? name,
    String? jarPath,
    int? port,
    int? maxPlayers,
    String? serverType,
    Value<String?> mcVersion = const Value.absent(),
    String? javaArgs,
    int? autoBackup,
    int? autoRestart,
    Value<String?> resourcePackUrl = const Value.absent(),
    Value<String?> resourcePackSha1 = const Value.absent(),
    String? status,
    Value<String?> javaPath = const Value.absent(),
    Value<DateTime?> createdAt = const Value.absent(),
  }) => Server(
    id: id ?? this.id,
    name: name ?? this.name,
    jarPath: jarPath ?? this.jarPath,
    port: port ?? this.port,
    maxPlayers: maxPlayers ?? this.maxPlayers,
    serverType: serverType ?? this.serverType,
    mcVersion: mcVersion.present ? mcVersion.value : this.mcVersion,
    javaArgs: javaArgs ?? this.javaArgs,
    autoBackup: autoBackup ?? this.autoBackup,
    autoRestart: autoRestart ?? this.autoRestart,
    resourcePackUrl: resourcePackUrl.present
        ? resourcePackUrl.value
        : this.resourcePackUrl,
    resourcePackSha1: resourcePackSha1.present
        ? resourcePackSha1.value
        : this.resourcePackSha1,
    status: status ?? this.status,
    javaPath: javaPath.present ? javaPath.value : this.javaPath,
    createdAt: createdAt.present ? createdAt.value : this.createdAt,
  );
  Server copyWithCompanion(ServersCompanion data) {
    return Server(
      id: data.id.present ? data.id.value : this.id,
      name: data.name.present ? data.name.value : this.name,
      jarPath: data.jarPath.present ? data.jarPath.value : this.jarPath,
      port: data.port.present ? data.port.value : this.port,
      maxPlayers: data.maxPlayers.present
          ? data.maxPlayers.value
          : this.maxPlayers,
      serverType: data.serverType.present
          ? data.serverType.value
          : this.serverType,
      mcVersion: data.mcVersion.present ? data.mcVersion.value : this.mcVersion,
      javaArgs: data.javaArgs.present ? data.javaArgs.value : this.javaArgs,
      autoBackup: data.autoBackup.present
          ? data.autoBackup.value
          : this.autoBackup,
      autoRestart: data.autoRestart.present
          ? data.autoRestart.value
          : this.autoRestart,
      resourcePackUrl: data.resourcePackUrl.present
          ? data.resourcePackUrl.value
          : this.resourcePackUrl,
      resourcePackSha1: data.resourcePackSha1.present
          ? data.resourcePackSha1.value
          : this.resourcePackSha1,
      status: data.status.present ? data.status.value : this.status,
      javaPath: data.javaPath.present ? data.javaPath.value : this.javaPath,
      createdAt: data.createdAt.present ? data.createdAt.value : this.createdAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('Server(')
          ..write('id: $id, ')
          ..write('name: $name, ')
          ..write('jarPath: $jarPath, ')
          ..write('port: $port, ')
          ..write('maxPlayers: $maxPlayers, ')
          ..write('serverType: $serverType, ')
          ..write('mcVersion: $mcVersion, ')
          ..write('javaArgs: $javaArgs, ')
          ..write('autoBackup: $autoBackup, ')
          ..write('autoRestart: $autoRestart, ')
          ..write('resourcePackUrl: $resourcePackUrl, ')
          ..write('resourcePackSha1: $resourcePackSha1, ')
          ..write('status: $status, ')
          ..write('javaPath: $javaPath, ')
          ..write('createdAt: $createdAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
    id,
    name,
    jarPath,
    port,
    maxPlayers,
    serverType,
    mcVersion,
    javaArgs,
    autoBackup,
    autoRestart,
    resourcePackUrl,
    resourcePackSha1,
    status,
    javaPath,
    createdAt,
  );
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is Server &&
          other.id == this.id &&
          other.name == this.name &&
          other.jarPath == this.jarPath &&
          other.port == this.port &&
          other.maxPlayers == this.maxPlayers &&
          other.serverType == this.serverType &&
          other.mcVersion == this.mcVersion &&
          other.javaArgs == this.javaArgs &&
          other.autoBackup == this.autoBackup &&
          other.autoRestart == this.autoRestart &&
          other.resourcePackUrl == this.resourcePackUrl &&
          other.resourcePackSha1 == this.resourcePackSha1 &&
          other.status == this.status &&
          other.javaPath == this.javaPath &&
          other.createdAt == this.createdAt);
}

class ServersCompanion extends UpdateCompanion<Server> {
  final Value<int> id;
  final Value<String> name;
  final Value<String> jarPath;
  final Value<int> port;
  final Value<int> maxPlayers;
  final Value<String> serverType;
  final Value<String?> mcVersion;
  final Value<String> javaArgs;
  final Value<int> autoBackup;
  final Value<int> autoRestart;
  final Value<String?> resourcePackUrl;
  final Value<String?> resourcePackSha1;
  final Value<String> status;
  final Value<String?> javaPath;
  final Value<DateTime?> createdAt;
  const ServersCompanion({
    this.id = const Value.absent(),
    this.name = const Value.absent(),
    this.jarPath = const Value.absent(),
    this.port = const Value.absent(),
    this.maxPlayers = const Value.absent(),
    this.serverType = const Value.absent(),
    this.mcVersion = const Value.absent(),
    this.javaArgs = const Value.absent(),
    this.autoBackup = const Value.absent(),
    this.autoRestart = const Value.absent(),
    this.resourcePackUrl = const Value.absent(),
    this.resourcePackSha1 = const Value.absent(),
    this.status = const Value.absent(),
    this.javaPath = const Value.absent(),
    this.createdAt = const Value.absent(),
  });
  ServersCompanion.insert({
    this.id = const Value.absent(),
    required String name,
    required String jarPath,
    this.port = const Value.absent(),
    this.maxPlayers = const Value.absent(),
    required String serverType,
    this.mcVersion = const Value.absent(),
    this.javaArgs = const Value.absent(),
    this.autoBackup = const Value.absent(),
    this.autoRestart = const Value.absent(),
    this.resourcePackUrl = const Value.absent(),
    this.resourcePackSha1 = const Value.absent(),
    this.status = const Value.absent(),
    this.javaPath = const Value.absent(),
    this.createdAt = const Value.absent(),
  }) : name = Value(name),
       jarPath = Value(jarPath),
       serverType = Value(serverType);
  static Insertable<Server> custom({
    Expression<int>? id,
    Expression<String>? name,
    Expression<String>? jarPath,
    Expression<int>? port,
    Expression<int>? maxPlayers,
    Expression<String>? serverType,
    Expression<String>? mcVersion,
    Expression<String>? javaArgs,
    Expression<int>? autoBackup,
    Expression<int>? autoRestart,
    Expression<String>? resourcePackUrl,
    Expression<String>? resourcePackSha1,
    Expression<String>? status,
    Expression<String>? javaPath,
    Expression<DateTime>? createdAt,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (name != null) 'name': name,
      if (jarPath != null) 'jar_path': jarPath,
      if (port != null) 'port': port,
      if (maxPlayers != null) 'max_players': maxPlayers,
      if (serverType != null) 'server_type': serverType,
      if (mcVersion != null) 'mc_version': mcVersion,
      if (javaArgs != null) 'java_args': javaArgs,
      if (autoBackup != null) 'auto_backup': autoBackup,
      if (autoRestart != null) 'auto_restart': autoRestart,
      if (resourcePackUrl != null) 'resource_pack_url': resourcePackUrl,
      if (resourcePackSha1 != null) 'resource_pack_sha1': resourcePackSha1,
      if (status != null) 'status': status,
      if (javaPath != null) 'java_path': javaPath,
      if (createdAt != null) 'created_at': createdAt,
    });
  }

  ServersCompanion copyWith({
    Value<int>? id,
    Value<String>? name,
    Value<String>? jarPath,
    Value<int>? port,
    Value<int>? maxPlayers,
    Value<String>? serverType,
    Value<String?>? mcVersion,
    Value<String>? javaArgs,
    Value<int>? autoBackup,
    Value<int>? autoRestart,
    Value<String?>? resourcePackUrl,
    Value<String?>? resourcePackSha1,
    Value<String>? status,
    Value<String?>? javaPath,
    Value<DateTime?>? createdAt,
  }) {
    return ServersCompanion(
      id: id ?? this.id,
      name: name ?? this.name,
      jarPath: jarPath ?? this.jarPath,
      port: port ?? this.port,
      maxPlayers: maxPlayers ?? this.maxPlayers,
      serverType: serverType ?? this.serverType,
      mcVersion: mcVersion ?? this.mcVersion,
      javaArgs: javaArgs ?? this.javaArgs,
      autoBackup: autoBackup ?? this.autoBackup,
      autoRestart: autoRestart ?? this.autoRestart,
      resourcePackUrl: resourcePackUrl ?? this.resourcePackUrl,
      resourcePackSha1: resourcePackSha1 ?? this.resourcePackSha1,
      status: status ?? this.status,
      javaPath: javaPath ?? this.javaPath,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (name.present) {
      map['name'] = Variable<String>(name.value);
    }
    if (jarPath.present) {
      map['jar_path'] = Variable<String>(jarPath.value);
    }
    if (port.present) {
      map['port'] = Variable<int>(port.value);
    }
    if (maxPlayers.present) {
      map['max_players'] = Variable<int>(maxPlayers.value);
    }
    if (serverType.present) {
      map['server_type'] = Variable<String>(serverType.value);
    }
    if (mcVersion.present) {
      map['mc_version'] = Variable<String>(mcVersion.value);
    }
    if (javaArgs.present) {
      map['java_args'] = Variable<String>(javaArgs.value);
    }
    if (autoBackup.present) {
      map['auto_backup'] = Variable<int>(autoBackup.value);
    }
    if (autoRestart.present) {
      map['auto_restart'] = Variable<int>(autoRestart.value);
    }
    if (resourcePackUrl.present) {
      map['resource_pack_url'] = Variable<String>(resourcePackUrl.value);
    }
    if (resourcePackSha1.present) {
      map['resource_pack_sha1'] = Variable<String>(resourcePackSha1.value);
    }
    if (status.present) {
      map['status'] = Variable<String>(status.value);
    }
    if (javaPath.present) {
      map['java_path'] = Variable<String>(javaPath.value);
    }
    if (createdAt.present) {
      map['created_at'] = Variable<DateTime>(createdAt.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('ServersCompanion(')
          ..write('id: $id, ')
          ..write('name: $name, ')
          ..write('jarPath: $jarPath, ')
          ..write('port: $port, ')
          ..write('maxPlayers: $maxPlayers, ')
          ..write('serverType: $serverType, ')
          ..write('mcVersion: $mcVersion, ')
          ..write('javaArgs: $javaArgs, ')
          ..write('autoBackup: $autoBackup, ')
          ..write('autoRestart: $autoRestart, ')
          ..write('resourcePackUrl: $resourcePackUrl, ')
          ..write('resourcePackSha1: $resourcePackSha1, ')
          ..write('status: $status, ')
          ..write('javaPath: $javaPath, ')
          ..write('createdAt: $createdAt')
          ..write(')'))
        .toString();
  }
}

class $ServerPropertiesTable extends ServerProperties
    with TableInfo<$ServerPropertiesTable, ServerProperty> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $ServerPropertiesTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
    'id',
    aliasedName,
    false,
    hasAutoIncrement: true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'PRIMARY KEY AUTOINCREMENT',
    ),
  );
  static const VerificationMeta _serverIdMeta = const VerificationMeta(
    'serverId',
  );
  @override
  late final GeneratedColumn<int> serverId = GeneratedColumn<int>(
    'server_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'REFERENCES servers (id)',
    ),
  );
  static const VerificationMeta _keyMeta = const VerificationMeta('key');
  @override
  late final GeneratedColumn<String> key = GeneratedColumn<String>(
    'key',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _valueMeta = const VerificationMeta('value');
  @override
  late final GeneratedColumn<String> value = GeneratedColumn<String>(
    'value',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  @override
  List<GeneratedColumn> get $columns => [id, serverId, key, value];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'server_properties';
  @override
  VerificationContext validateIntegrity(
    Insertable<ServerProperty> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('server_id')) {
      context.handle(
        _serverIdMeta,
        serverId.isAcceptableOrUnknown(data['server_id']!, _serverIdMeta),
      );
    } else if (isInserting) {
      context.missing(_serverIdMeta);
    }
    if (data.containsKey('key')) {
      context.handle(
        _keyMeta,
        key.isAcceptableOrUnknown(data['key']!, _keyMeta),
      );
    } else if (isInserting) {
      context.missing(_keyMeta);
    }
    if (data.containsKey('value')) {
      context.handle(
        _valueMeta,
        value.isAcceptableOrUnknown(data['value']!, _valueMeta),
      );
    } else if (isInserting) {
      context.missing(_valueMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  ServerProperty map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return ServerProperty(
      id: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}id'],
      )!,
      serverId: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}server_id'],
      )!,
      key: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}key'],
      )!,
      value: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}value'],
      )!,
    );
  }

  @override
  $ServerPropertiesTable createAlias(String alias) {
    return $ServerPropertiesTable(attachedDatabase, alias);
  }
}

class ServerProperty extends DataClass implements Insertable<ServerProperty> {
  final int id;
  final int serverId;
  final String key;
  final String value;
  const ServerProperty({
    required this.id,
    required this.serverId,
    required this.key,
    required this.value,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['server_id'] = Variable<int>(serverId);
    map['key'] = Variable<String>(key);
    map['value'] = Variable<String>(value);
    return map;
  }

  ServerPropertiesCompanion toCompanion(bool nullToAbsent) {
    return ServerPropertiesCompanion(
      id: Value(id),
      serverId: Value(serverId),
      key: Value(key),
      value: Value(value),
    );
  }

  factory ServerProperty.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return ServerProperty(
      id: serializer.fromJson<int>(json['id']),
      serverId: serializer.fromJson<int>(json['serverId']),
      key: serializer.fromJson<String>(json['key']),
      value: serializer.fromJson<String>(json['value']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'serverId': serializer.toJson<int>(serverId),
      'key': serializer.toJson<String>(key),
      'value': serializer.toJson<String>(value),
    };
  }

  ServerProperty copyWith({
    int? id,
    int? serverId,
    String? key,
    String? value,
  }) => ServerProperty(
    id: id ?? this.id,
    serverId: serverId ?? this.serverId,
    key: key ?? this.key,
    value: value ?? this.value,
  );
  ServerProperty copyWithCompanion(ServerPropertiesCompanion data) {
    return ServerProperty(
      id: data.id.present ? data.id.value : this.id,
      serverId: data.serverId.present ? data.serverId.value : this.serverId,
      key: data.key.present ? data.key.value : this.key,
      value: data.value.present ? data.value.value : this.value,
    );
  }

  @override
  String toString() {
    return (StringBuffer('ServerProperty(')
          ..write('id: $id, ')
          ..write('serverId: $serverId, ')
          ..write('key: $key, ')
          ..write('value: $value')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, serverId, key, value);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is ServerProperty &&
          other.id == this.id &&
          other.serverId == this.serverId &&
          other.key == this.key &&
          other.value == this.value);
}

class ServerPropertiesCompanion extends UpdateCompanion<ServerProperty> {
  final Value<int> id;
  final Value<int> serverId;
  final Value<String> key;
  final Value<String> value;
  const ServerPropertiesCompanion({
    this.id = const Value.absent(),
    this.serverId = const Value.absent(),
    this.key = const Value.absent(),
    this.value = const Value.absent(),
  });
  ServerPropertiesCompanion.insert({
    this.id = const Value.absent(),
    required int serverId,
    required String key,
    required String value,
  }) : serverId = Value(serverId),
       key = Value(key),
       value = Value(value);
  static Insertable<ServerProperty> custom({
    Expression<int>? id,
    Expression<int>? serverId,
    Expression<String>? key,
    Expression<String>? value,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (serverId != null) 'server_id': serverId,
      if (key != null) 'key': key,
      if (value != null) 'value': value,
    });
  }

  ServerPropertiesCompanion copyWith({
    Value<int>? id,
    Value<int>? serverId,
    Value<String>? key,
    Value<String>? value,
  }) {
    return ServerPropertiesCompanion(
      id: id ?? this.id,
      serverId: serverId ?? this.serverId,
      key: key ?? this.key,
      value: value ?? this.value,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (serverId.present) {
      map['server_id'] = Variable<int>(serverId.value);
    }
    if (key.present) {
      map['key'] = Variable<String>(key.value);
    }
    if (value.present) {
      map['value'] = Variable<String>(value.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('ServerPropertiesCompanion(')
          ..write('id: $id, ')
          ..write('serverId: $serverId, ')
          ..write('key: $key, ')
          ..write('value: $value')
          ..write(')'))
        .toString();
  }
}

class $BackupsTable extends Backups with TableInfo<$BackupsTable, Backup> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $BackupsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
    'id',
    aliasedName,
    false,
    hasAutoIncrement: true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'PRIMARY KEY AUTOINCREMENT',
    ),
  );
  static const VerificationMeta _serverIdMeta = const VerificationMeta(
    'serverId',
  );
  @override
  late final GeneratedColumn<int> serverId = GeneratedColumn<int>(
    'server_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'REFERENCES servers (id)',
    ),
  );
  static const VerificationMeta _nameMeta = const VerificationMeta('name');
  @override
  late final GeneratedColumn<String> name = GeneratedColumn<String>(
    'name',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _pathMeta = const VerificationMeta('path');
  @override
  late final GeneratedColumn<String> path = GeneratedColumn<String>(
    'path',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _sizeMeta = const VerificationMeta('size');
  @override
  late final GeneratedColumn<int> size = GeneratedColumn<int>(
    'size',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _createdAtMeta = const VerificationMeta(
    'createdAt',
  );
  @override
  late final GeneratedColumn<DateTime> createdAt = GeneratedColumn<DateTime>(
    'created_at',
    aliasedName,
    true,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: false,
  );
  @override
  List<GeneratedColumn> get $columns => [
    id,
    serverId,
    name,
    path,
    size,
    createdAt,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'backups';
  @override
  VerificationContext validateIntegrity(
    Insertable<Backup> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('server_id')) {
      context.handle(
        _serverIdMeta,
        serverId.isAcceptableOrUnknown(data['server_id']!, _serverIdMeta),
      );
    } else if (isInserting) {
      context.missing(_serverIdMeta);
    }
    if (data.containsKey('name')) {
      context.handle(
        _nameMeta,
        name.isAcceptableOrUnknown(data['name']!, _nameMeta),
      );
    } else if (isInserting) {
      context.missing(_nameMeta);
    }
    if (data.containsKey('path')) {
      context.handle(
        _pathMeta,
        path.isAcceptableOrUnknown(data['path']!, _pathMeta),
      );
    } else if (isInserting) {
      context.missing(_pathMeta);
    }
    if (data.containsKey('size')) {
      context.handle(
        _sizeMeta,
        size.isAcceptableOrUnknown(data['size']!, _sizeMeta),
      );
    } else if (isInserting) {
      context.missing(_sizeMeta);
    }
    if (data.containsKey('created_at')) {
      context.handle(
        _createdAtMeta,
        createdAt.isAcceptableOrUnknown(data['created_at']!, _createdAtMeta),
      );
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  Backup map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return Backup(
      id: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}id'],
      )!,
      serverId: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}server_id'],
      )!,
      name: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}name'],
      )!,
      path: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}path'],
      )!,
      size: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}size'],
      )!,
      createdAt: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}created_at'],
      ),
    );
  }

  @override
  $BackupsTable createAlias(String alias) {
    return $BackupsTable(attachedDatabase, alias);
  }
}

class Backup extends DataClass implements Insertable<Backup> {
  final int id;
  final int serverId;
  final String name;
  final String path;
  final int size;
  final DateTime? createdAt;
  const Backup({
    required this.id,
    required this.serverId,
    required this.name,
    required this.path,
    required this.size,
    this.createdAt,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['server_id'] = Variable<int>(serverId);
    map['name'] = Variable<String>(name);
    map['path'] = Variable<String>(path);
    map['size'] = Variable<int>(size);
    if (!nullToAbsent || createdAt != null) {
      map['created_at'] = Variable<DateTime>(createdAt);
    }
    return map;
  }

  BackupsCompanion toCompanion(bool nullToAbsent) {
    return BackupsCompanion(
      id: Value(id),
      serverId: Value(serverId),
      name: Value(name),
      path: Value(path),
      size: Value(size),
      createdAt: createdAt == null && nullToAbsent
          ? const Value.absent()
          : Value(createdAt),
    );
  }

  factory Backup.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return Backup(
      id: serializer.fromJson<int>(json['id']),
      serverId: serializer.fromJson<int>(json['serverId']),
      name: serializer.fromJson<String>(json['name']),
      path: serializer.fromJson<String>(json['path']),
      size: serializer.fromJson<int>(json['size']),
      createdAt: serializer.fromJson<DateTime?>(json['createdAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'serverId': serializer.toJson<int>(serverId),
      'name': serializer.toJson<String>(name),
      'path': serializer.toJson<String>(path),
      'size': serializer.toJson<int>(size),
      'createdAt': serializer.toJson<DateTime?>(createdAt),
    };
  }

  Backup copyWith({
    int? id,
    int? serverId,
    String? name,
    String? path,
    int? size,
    Value<DateTime?> createdAt = const Value.absent(),
  }) => Backup(
    id: id ?? this.id,
    serverId: serverId ?? this.serverId,
    name: name ?? this.name,
    path: path ?? this.path,
    size: size ?? this.size,
    createdAt: createdAt.present ? createdAt.value : this.createdAt,
  );
  Backup copyWithCompanion(BackupsCompanion data) {
    return Backup(
      id: data.id.present ? data.id.value : this.id,
      serverId: data.serverId.present ? data.serverId.value : this.serverId,
      name: data.name.present ? data.name.value : this.name,
      path: data.path.present ? data.path.value : this.path,
      size: data.size.present ? data.size.value : this.size,
      createdAt: data.createdAt.present ? data.createdAt.value : this.createdAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('Backup(')
          ..write('id: $id, ')
          ..write('serverId: $serverId, ')
          ..write('name: $name, ')
          ..write('path: $path, ')
          ..write('size: $size, ')
          ..write('createdAt: $createdAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, serverId, name, path, size, createdAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is Backup &&
          other.id == this.id &&
          other.serverId == this.serverId &&
          other.name == this.name &&
          other.path == this.path &&
          other.size == this.size &&
          other.createdAt == this.createdAt);
}

class BackupsCompanion extends UpdateCompanion<Backup> {
  final Value<int> id;
  final Value<int> serverId;
  final Value<String> name;
  final Value<String> path;
  final Value<int> size;
  final Value<DateTime?> createdAt;
  const BackupsCompanion({
    this.id = const Value.absent(),
    this.serverId = const Value.absent(),
    this.name = const Value.absent(),
    this.path = const Value.absent(),
    this.size = const Value.absent(),
    this.createdAt = const Value.absent(),
  });
  BackupsCompanion.insert({
    this.id = const Value.absent(),
    required int serverId,
    required String name,
    required String path,
    required int size,
    this.createdAt = const Value.absent(),
  }) : serverId = Value(serverId),
       name = Value(name),
       path = Value(path),
       size = Value(size);
  static Insertable<Backup> custom({
    Expression<int>? id,
    Expression<int>? serverId,
    Expression<String>? name,
    Expression<String>? path,
    Expression<int>? size,
    Expression<DateTime>? createdAt,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (serverId != null) 'server_id': serverId,
      if (name != null) 'name': name,
      if (path != null) 'path': path,
      if (size != null) 'size': size,
      if (createdAt != null) 'created_at': createdAt,
    });
  }

  BackupsCompanion copyWith({
    Value<int>? id,
    Value<int>? serverId,
    Value<String>? name,
    Value<String>? path,
    Value<int>? size,
    Value<DateTime?>? createdAt,
  }) {
    return BackupsCompanion(
      id: id ?? this.id,
      serverId: serverId ?? this.serverId,
      name: name ?? this.name,
      path: path ?? this.path,
      size: size ?? this.size,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (serverId.present) {
      map['server_id'] = Variable<int>(serverId.value);
    }
    if (name.present) {
      map['name'] = Variable<String>(name.value);
    }
    if (path.present) {
      map['path'] = Variable<String>(path.value);
    }
    if (size.present) {
      map['size'] = Variable<int>(size.value);
    }
    if (createdAt.present) {
      map['created_at'] = Variable<DateTime>(createdAt.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('BackupsCompanion(')
          ..write('id: $id, ')
          ..write('serverId: $serverId, ')
          ..write('name: $name, ')
          ..write('path: $path, ')
          ..write('size: $size, ')
          ..write('createdAt: $createdAt')
          ..write(')'))
        .toString();
  }
}

class $ConsoleLogsTable extends ConsoleLogs
    with TableInfo<$ConsoleLogsTable, ConsoleLog> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $ConsoleLogsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
    'id',
    aliasedName,
    false,
    hasAutoIncrement: true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'PRIMARY KEY AUTOINCREMENT',
    ),
  );
  static const VerificationMeta _serverIdMeta = const VerificationMeta(
    'serverId',
  );
  @override
  late final GeneratedColumn<int> serverId = GeneratedColumn<int>(
    'server_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'REFERENCES servers (id)',
    ),
  );
  static const VerificationMeta _lineMeta = const VerificationMeta('line');
  @override
  late final GeneratedColumn<String> line = GeneratedColumn<String>(
    'line',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _lineTypeMeta = const VerificationMeta(
    'lineType',
  );
  @override
  late final GeneratedColumn<int> lineType = GeneratedColumn<int>(
    'line_type',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultValue: const Constant(0),
  );
  static const VerificationMeta _timestampMeta = const VerificationMeta(
    'timestamp',
  );
  @override
  late final GeneratedColumn<DateTime> timestamp = GeneratedColumn<DateTime>(
    'timestamp',
    aliasedName,
    true,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: false,
  );
  @override
  List<GeneratedColumn> get $columns => [
    id,
    serverId,
    line,
    lineType,
    timestamp,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'console_logs';
  @override
  VerificationContext validateIntegrity(
    Insertable<ConsoleLog> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('server_id')) {
      context.handle(
        _serverIdMeta,
        serverId.isAcceptableOrUnknown(data['server_id']!, _serverIdMeta),
      );
    } else if (isInserting) {
      context.missing(_serverIdMeta);
    }
    if (data.containsKey('line')) {
      context.handle(
        _lineMeta,
        line.isAcceptableOrUnknown(data['line']!, _lineMeta),
      );
    } else if (isInserting) {
      context.missing(_lineMeta);
    }
    if (data.containsKey('line_type')) {
      context.handle(
        _lineTypeMeta,
        lineType.isAcceptableOrUnknown(data['line_type']!, _lineTypeMeta),
      );
    }
    if (data.containsKey('timestamp')) {
      context.handle(
        _timestampMeta,
        timestamp.isAcceptableOrUnknown(data['timestamp']!, _timestampMeta),
      );
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  ConsoleLog map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return ConsoleLog(
      id: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}id'],
      )!,
      serverId: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}server_id'],
      )!,
      line: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}line'],
      )!,
      lineType: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}line_type'],
      )!,
      timestamp: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}timestamp'],
      ),
    );
  }

  @override
  $ConsoleLogsTable createAlias(String alias) {
    return $ConsoleLogsTable(attachedDatabase, alias);
  }
}

class ConsoleLog extends DataClass implements Insertable<ConsoleLog> {
  final int id;
  final int serverId;
  final String line;
  final int lineType;
  final DateTime? timestamp;
  const ConsoleLog({
    required this.id,
    required this.serverId,
    required this.line,
    required this.lineType,
    this.timestamp,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['server_id'] = Variable<int>(serverId);
    map['line'] = Variable<String>(line);
    map['line_type'] = Variable<int>(lineType);
    if (!nullToAbsent || timestamp != null) {
      map['timestamp'] = Variable<DateTime>(timestamp);
    }
    return map;
  }

  ConsoleLogsCompanion toCompanion(bool nullToAbsent) {
    return ConsoleLogsCompanion(
      id: Value(id),
      serverId: Value(serverId),
      line: Value(line),
      lineType: Value(lineType),
      timestamp: timestamp == null && nullToAbsent
          ? const Value.absent()
          : Value(timestamp),
    );
  }

  factory ConsoleLog.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return ConsoleLog(
      id: serializer.fromJson<int>(json['id']),
      serverId: serializer.fromJson<int>(json['serverId']),
      line: serializer.fromJson<String>(json['line']),
      lineType: serializer.fromJson<int>(json['lineType']),
      timestamp: serializer.fromJson<DateTime?>(json['timestamp']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'serverId': serializer.toJson<int>(serverId),
      'line': serializer.toJson<String>(line),
      'lineType': serializer.toJson<int>(lineType),
      'timestamp': serializer.toJson<DateTime?>(timestamp),
    };
  }

  ConsoleLog copyWith({
    int? id,
    int? serverId,
    String? line,
    int? lineType,
    Value<DateTime?> timestamp = const Value.absent(),
  }) => ConsoleLog(
    id: id ?? this.id,
    serverId: serverId ?? this.serverId,
    line: line ?? this.line,
    lineType: lineType ?? this.lineType,
    timestamp: timestamp.present ? timestamp.value : this.timestamp,
  );
  ConsoleLog copyWithCompanion(ConsoleLogsCompanion data) {
    return ConsoleLog(
      id: data.id.present ? data.id.value : this.id,
      serverId: data.serverId.present ? data.serverId.value : this.serverId,
      line: data.line.present ? data.line.value : this.line,
      lineType: data.lineType.present ? data.lineType.value : this.lineType,
      timestamp: data.timestamp.present ? data.timestamp.value : this.timestamp,
    );
  }

  @override
  String toString() {
    return (StringBuffer('ConsoleLog(')
          ..write('id: $id, ')
          ..write('serverId: $serverId, ')
          ..write('line: $line, ')
          ..write('lineType: $lineType, ')
          ..write('timestamp: $timestamp')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, serverId, line, lineType, timestamp);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is ConsoleLog &&
          other.id == this.id &&
          other.serverId == this.serverId &&
          other.line == this.line &&
          other.lineType == this.lineType &&
          other.timestamp == this.timestamp);
}

class ConsoleLogsCompanion extends UpdateCompanion<ConsoleLog> {
  final Value<int> id;
  final Value<int> serverId;
  final Value<String> line;
  final Value<int> lineType;
  final Value<DateTime?> timestamp;
  const ConsoleLogsCompanion({
    this.id = const Value.absent(),
    this.serverId = const Value.absent(),
    this.line = const Value.absent(),
    this.lineType = const Value.absent(),
    this.timestamp = const Value.absent(),
  });
  ConsoleLogsCompanion.insert({
    this.id = const Value.absent(),
    required int serverId,
    required String line,
    this.lineType = const Value.absent(),
    this.timestamp = const Value.absent(),
  }) : serverId = Value(serverId),
       line = Value(line);
  static Insertable<ConsoleLog> custom({
    Expression<int>? id,
    Expression<int>? serverId,
    Expression<String>? line,
    Expression<int>? lineType,
    Expression<DateTime>? timestamp,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (serverId != null) 'server_id': serverId,
      if (line != null) 'line': line,
      if (lineType != null) 'line_type': lineType,
      if (timestamp != null) 'timestamp': timestamp,
    });
  }

  ConsoleLogsCompanion copyWith({
    Value<int>? id,
    Value<int>? serverId,
    Value<String>? line,
    Value<int>? lineType,
    Value<DateTime?>? timestamp,
  }) {
    return ConsoleLogsCompanion(
      id: id ?? this.id,
      serverId: serverId ?? this.serverId,
      line: line ?? this.line,
      lineType: lineType ?? this.lineType,
      timestamp: timestamp ?? this.timestamp,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (serverId.present) {
      map['server_id'] = Variable<int>(serverId.value);
    }
    if (line.present) {
      map['line'] = Variable<String>(line.value);
    }
    if (lineType.present) {
      map['line_type'] = Variable<int>(lineType.value);
    }
    if (timestamp.present) {
      map['timestamp'] = Variable<DateTime>(timestamp.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('ConsoleLogsCompanion(')
          ..write('id: $id, ')
          ..write('serverId: $serverId, ')
          ..write('line: $line, ')
          ..write('lineType: $lineType, ')
          ..write('timestamp: $timestamp')
          ..write(')'))
        .toString();
  }
}

abstract class _$AppDatabase extends GeneratedDatabase {
  _$AppDatabase(QueryExecutor e) : super(e);
  $AppDatabaseManager get managers => $AppDatabaseManager(this);
  late final $ServersTable servers = $ServersTable(this);
  late final $ServerPropertiesTable serverProperties = $ServerPropertiesTable(
    this,
  );
  late final $BackupsTable backups = $BackupsTable(this);
  late final $ConsoleLogsTable consoleLogs = $ConsoleLogsTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [
    servers,
    serverProperties,
    backups,
    consoleLogs,
  ];
}

typedef $$ServersTableCreateCompanionBuilder =
    ServersCompanion Function({
      Value<int> id,
      required String name,
      required String jarPath,
      Value<int> port,
      Value<int> maxPlayers,
      required String serverType,
      Value<String?> mcVersion,
      Value<String> javaArgs,
      Value<int> autoBackup,
      Value<int> autoRestart,
      Value<String?> resourcePackUrl,
      Value<String?> resourcePackSha1,
      Value<String> status,
      Value<String?> javaPath,
      Value<DateTime?> createdAt,
    });
typedef $$ServersTableUpdateCompanionBuilder =
    ServersCompanion Function({
      Value<int> id,
      Value<String> name,
      Value<String> jarPath,
      Value<int> port,
      Value<int> maxPlayers,
      Value<String> serverType,
      Value<String?> mcVersion,
      Value<String> javaArgs,
      Value<int> autoBackup,
      Value<int> autoRestart,
      Value<String?> resourcePackUrl,
      Value<String?> resourcePackSha1,
      Value<String> status,
      Value<String?> javaPath,
      Value<DateTime?> createdAt,
    });

final class $$ServersTableReferences
    extends BaseReferences<_$AppDatabase, $ServersTable, Server> {
  $$ServersTableReferences(super.$_db, super.$_table, super.$_typedResult);

  static MultiTypedResultKey<$ServerPropertiesTable, List<ServerProperty>>
  _serverPropertiesRefsTable(_$AppDatabase db) => MultiTypedResultKey.fromTable(
    db.serverProperties,
    aliasName: $_aliasNameGenerator(
      db.servers.id,
      db.serverProperties.serverId,
    ),
  );

  $$ServerPropertiesTableProcessedTableManager get serverPropertiesRefs {
    final manager = $$ServerPropertiesTableTableManager(
      $_db,
      $_db.serverProperties,
    ).filter((f) => f.serverId.id.sqlEquals($_itemColumn<int>('id')!));

    final cache = $_typedResult.readTableOrNull(
      _serverPropertiesRefsTable($_db),
    );
    return ProcessedTableManager(
      manager.$state.copyWith(prefetchedData: cache),
    );
  }

  static MultiTypedResultKey<$BackupsTable, List<Backup>> _backupsRefsTable(
    _$AppDatabase db,
  ) => MultiTypedResultKey.fromTable(
    db.backups,
    aliasName: $_aliasNameGenerator(db.servers.id, db.backups.serverId),
  );

  $$BackupsTableProcessedTableManager get backupsRefs {
    final manager = $$BackupsTableTableManager(
      $_db,
      $_db.backups,
    ).filter((f) => f.serverId.id.sqlEquals($_itemColumn<int>('id')!));

    final cache = $_typedResult.readTableOrNull(_backupsRefsTable($_db));
    return ProcessedTableManager(
      manager.$state.copyWith(prefetchedData: cache),
    );
  }

  static MultiTypedResultKey<$ConsoleLogsTable, List<ConsoleLog>>
  _consoleLogsRefsTable(_$AppDatabase db) => MultiTypedResultKey.fromTable(
    db.consoleLogs,
    aliasName: $_aliasNameGenerator(db.servers.id, db.consoleLogs.serverId),
  );

  $$ConsoleLogsTableProcessedTableManager get consoleLogsRefs {
    final manager = $$ConsoleLogsTableTableManager(
      $_db,
      $_db.consoleLogs,
    ).filter((f) => f.serverId.id.sqlEquals($_itemColumn<int>('id')!));

    final cache = $_typedResult.readTableOrNull(_consoleLogsRefsTable($_db));
    return ProcessedTableManager(
      manager.$state.copyWith(prefetchedData: cache),
    );
  }
}

class $$ServersTableFilterComposer
    extends Composer<_$AppDatabase, $ServersTable> {
  $$ServersTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get name => $composableBuilder(
    column: $table.name,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get jarPath => $composableBuilder(
    column: $table.jarPath,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get port => $composableBuilder(
    column: $table.port,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get maxPlayers => $composableBuilder(
    column: $table.maxPlayers,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get serverType => $composableBuilder(
    column: $table.serverType,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get mcVersion => $composableBuilder(
    column: $table.mcVersion,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get javaArgs => $composableBuilder(
    column: $table.javaArgs,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get autoBackup => $composableBuilder(
    column: $table.autoBackup,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get autoRestart => $composableBuilder(
    column: $table.autoRestart,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get resourcePackUrl => $composableBuilder(
    column: $table.resourcePackUrl,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get resourcePackSha1 => $composableBuilder(
    column: $table.resourcePackSha1,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get status => $composableBuilder(
    column: $table.status,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get javaPath => $composableBuilder(
    column: $table.javaPath,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnFilters(column),
  );

  Expression<bool> serverPropertiesRefs(
    Expression<bool> Function($$ServerPropertiesTableFilterComposer f) f,
  ) {
    final $$ServerPropertiesTableFilterComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.id,
      referencedTable: $db.serverProperties,
      getReferencedColumn: (t) => t.serverId,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServerPropertiesTableFilterComposer(
            $db: $db,
            $table: $db.serverProperties,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return f(composer);
  }

  Expression<bool> backupsRefs(
    Expression<bool> Function($$BackupsTableFilterComposer f) f,
  ) {
    final $$BackupsTableFilterComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.id,
      referencedTable: $db.backups,
      getReferencedColumn: (t) => t.serverId,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$BackupsTableFilterComposer(
            $db: $db,
            $table: $db.backups,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return f(composer);
  }

  Expression<bool> consoleLogsRefs(
    Expression<bool> Function($$ConsoleLogsTableFilterComposer f) f,
  ) {
    final $$ConsoleLogsTableFilterComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.id,
      referencedTable: $db.consoleLogs,
      getReferencedColumn: (t) => t.serverId,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ConsoleLogsTableFilterComposer(
            $db: $db,
            $table: $db.consoleLogs,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return f(composer);
  }
}

class $$ServersTableOrderingComposer
    extends Composer<_$AppDatabase, $ServersTable> {
  $$ServersTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get name => $composableBuilder(
    column: $table.name,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get jarPath => $composableBuilder(
    column: $table.jarPath,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get port => $composableBuilder(
    column: $table.port,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get maxPlayers => $composableBuilder(
    column: $table.maxPlayers,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get serverType => $composableBuilder(
    column: $table.serverType,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get mcVersion => $composableBuilder(
    column: $table.mcVersion,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get javaArgs => $composableBuilder(
    column: $table.javaArgs,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get autoBackup => $composableBuilder(
    column: $table.autoBackup,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get autoRestart => $composableBuilder(
    column: $table.autoRestart,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get resourcePackUrl => $composableBuilder(
    column: $table.resourcePackUrl,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get resourcePackSha1 => $composableBuilder(
    column: $table.resourcePackSha1,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get status => $composableBuilder(
    column: $table.status,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get javaPath => $composableBuilder(
    column: $table.javaPath,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$ServersTableAnnotationComposer
    extends Composer<_$AppDatabase, $ServersTable> {
  $$ServersTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get name =>
      $composableBuilder(column: $table.name, builder: (column) => column);

  GeneratedColumn<String> get jarPath =>
      $composableBuilder(column: $table.jarPath, builder: (column) => column);

  GeneratedColumn<int> get port =>
      $composableBuilder(column: $table.port, builder: (column) => column);

  GeneratedColumn<int> get maxPlayers => $composableBuilder(
    column: $table.maxPlayers,
    builder: (column) => column,
  );

  GeneratedColumn<String> get serverType => $composableBuilder(
    column: $table.serverType,
    builder: (column) => column,
  );

  GeneratedColumn<String> get mcVersion =>
      $composableBuilder(column: $table.mcVersion, builder: (column) => column);

  GeneratedColumn<String> get javaArgs =>
      $composableBuilder(column: $table.javaArgs, builder: (column) => column);

  GeneratedColumn<int> get autoBackup => $composableBuilder(
    column: $table.autoBackup,
    builder: (column) => column,
  );

  GeneratedColumn<int> get autoRestart => $composableBuilder(
    column: $table.autoRestart,
    builder: (column) => column,
  );

  GeneratedColumn<String> get resourcePackUrl => $composableBuilder(
    column: $table.resourcePackUrl,
    builder: (column) => column,
  );

  GeneratedColumn<String> get resourcePackSha1 => $composableBuilder(
    column: $table.resourcePackSha1,
    builder: (column) => column,
  );

  GeneratedColumn<String> get status =>
      $composableBuilder(column: $table.status, builder: (column) => column);

  GeneratedColumn<String> get javaPath =>
      $composableBuilder(column: $table.javaPath, builder: (column) => column);

  GeneratedColumn<DateTime> get createdAt =>
      $composableBuilder(column: $table.createdAt, builder: (column) => column);

  Expression<T> serverPropertiesRefs<T extends Object>(
    Expression<T> Function($$ServerPropertiesTableAnnotationComposer a) f,
  ) {
    final $$ServerPropertiesTableAnnotationComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.id,
      referencedTable: $db.serverProperties,
      getReferencedColumn: (t) => t.serverId,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServerPropertiesTableAnnotationComposer(
            $db: $db,
            $table: $db.serverProperties,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return f(composer);
  }

  Expression<T> backupsRefs<T extends Object>(
    Expression<T> Function($$BackupsTableAnnotationComposer a) f,
  ) {
    final $$BackupsTableAnnotationComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.id,
      referencedTable: $db.backups,
      getReferencedColumn: (t) => t.serverId,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$BackupsTableAnnotationComposer(
            $db: $db,
            $table: $db.backups,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return f(composer);
  }

  Expression<T> consoleLogsRefs<T extends Object>(
    Expression<T> Function($$ConsoleLogsTableAnnotationComposer a) f,
  ) {
    final $$ConsoleLogsTableAnnotationComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.id,
      referencedTable: $db.consoleLogs,
      getReferencedColumn: (t) => t.serverId,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ConsoleLogsTableAnnotationComposer(
            $db: $db,
            $table: $db.consoleLogs,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return f(composer);
  }
}

class $$ServersTableTableManager
    extends
        RootTableManager<
          _$AppDatabase,
          $ServersTable,
          Server,
          $$ServersTableFilterComposer,
          $$ServersTableOrderingComposer,
          $$ServersTableAnnotationComposer,
          $$ServersTableCreateCompanionBuilder,
          $$ServersTableUpdateCompanionBuilder,
          (Server, $$ServersTableReferences),
          Server,
          PrefetchHooks Function({
            bool serverPropertiesRefs,
            bool backupsRefs,
            bool consoleLogsRefs,
          })
        > {
  $$ServersTableTableManager(_$AppDatabase db, $ServersTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$ServersTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$ServersTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$ServersTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                Value<String> name = const Value.absent(),
                Value<String> jarPath = const Value.absent(),
                Value<int> port = const Value.absent(),
                Value<int> maxPlayers = const Value.absent(),
                Value<String> serverType = const Value.absent(),
                Value<String?> mcVersion = const Value.absent(),
                Value<String> javaArgs = const Value.absent(),
                Value<int> autoBackup = const Value.absent(),
                Value<int> autoRestart = const Value.absent(),
                Value<String?> resourcePackUrl = const Value.absent(),
                Value<String?> resourcePackSha1 = const Value.absent(),
                Value<String> status = const Value.absent(),
                Value<String?> javaPath = const Value.absent(),
                Value<DateTime?> createdAt = const Value.absent(),
              }) => ServersCompanion(
                id: id,
                name: name,
                jarPath: jarPath,
                port: port,
                maxPlayers: maxPlayers,
                serverType: serverType,
                mcVersion: mcVersion,
                javaArgs: javaArgs,
                autoBackup: autoBackup,
                autoRestart: autoRestart,
                resourcePackUrl: resourcePackUrl,
                resourcePackSha1: resourcePackSha1,
                status: status,
                javaPath: javaPath,
                createdAt: createdAt,
              ),
          createCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                required String name,
                required String jarPath,
                Value<int> port = const Value.absent(),
                Value<int> maxPlayers = const Value.absent(),
                required String serverType,
                Value<String?> mcVersion = const Value.absent(),
                Value<String> javaArgs = const Value.absent(),
                Value<int> autoBackup = const Value.absent(),
                Value<int> autoRestart = const Value.absent(),
                Value<String?> resourcePackUrl = const Value.absent(),
                Value<String?> resourcePackSha1 = const Value.absent(),
                Value<String> status = const Value.absent(),
                Value<String?> javaPath = const Value.absent(),
                Value<DateTime?> createdAt = const Value.absent(),
              }) => ServersCompanion.insert(
                id: id,
                name: name,
                jarPath: jarPath,
                port: port,
                maxPlayers: maxPlayers,
                serverType: serverType,
                mcVersion: mcVersion,
                javaArgs: javaArgs,
                autoBackup: autoBackup,
                autoRestart: autoRestart,
                resourcePackUrl: resourcePackUrl,
                resourcePackSha1: resourcePackSha1,
                status: status,
                javaPath: javaPath,
                createdAt: createdAt,
              ),
          withReferenceMapper: (p0) => p0
              .map(
                (e) => (
                  e.readTable(table),
                  $$ServersTableReferences(db, table, e),
                ),
              )
              .toList(),
          prefetchHooksCallback:
              ({
                serverPropertiesRefs = false,
                backupsRefs = false,
                consoleLogsRefs = false,
              }) {
                return PrefetchHooks(
                  db: db,
                  explicitlyWatchedTables: [
                    if (serverPropertiesRefs) db.serverProperties,
                    if (backupsRefs) db.backups,
                    if (consoleLogsRefs) db.consoleLogs,
                  ],
                  addJoins: null,
                  getPrefetchedDataCallback: (items) async {
                    return [
                      if (serverPropertiesRefs)
                        await $_getPrefetchedData<
                          Server,
                          $ServersTable,
                          ServerProperty
                        >(
                          currentTable: table,
                          referencedTable: $$ServersTableReferences
                              ._serverPropertiesRefsTable(db),
                          managerFromTypedResult: (p0) =>
                              $$ServersTableReferences(
                                db,
                                table,
                                p0,
                              ).serverPropertiesRefs,
                          referencedItemsForCurrentItem:
                              (item, referencedItems) => referencedItems.where(
                                (e) => e.serverId == item.id,
                              ),
                          typedResults: items,
                        ),
                      if (backupsRefs)
                        await $_getPrefetchedData<
                          Server,
                          $ServersTable,
                          Backup
                        >(
                          currentTable: table,
                          referencedTable: $$ServersTableReferences
                              ._backupsRefsTable(db),
                          managerFromTypedResult: (p0) =>
                              $$ServersTableReferences(
                                db,
                                table,
                                p0,
                              ).backupsRefs,
                          referencedItemsForCurrentItem:
                              (item, referencedItems) => referencedItems.where(
                                (e) => e.serverId == item.id,
                              ),
                          typedResults: items,
                        ),
                      if (consoleLogsRefs)
                        await $_getPrefetchedData<
                          Server,
                          $ServersTable,
                          ConsoleLog
                        >(
                          currentTable: table,
                          referencedTable: $$ServersTableReferences
                              ._consoleLogsRefsTable(db),
                          managerFromTypedResult: (p0) =>
                              $$ServersTableReferences(
                                db,
                                table,
                                p0,
                              ).consoleLogsRefs,
                          referencedItemsForCurrentItem:
                              (item, referencedItems) => referencedItems.where(
                                (e) => e.serverId == item.id,
                              ),
                          typedResults: items,
                        ),
                    ];
                  },
                );
              },
        ),
      );
}

typedef $$ServersTableProcessedTableManager =
    ProcessedTableManager<
      _$AppDatabase,
      $ServersTable,
      Server,
      $$ServersTableFilterComposer,
      $$ServersTableOrderingComposer,
      $$ServersTableAnnotationComposer,
      $$ServersTableCreateCompanionBuilder,
      $$ServersTableUpdateCompanionBuilder,
      (Server, $$ServersTableReferences),
      Server,
      PrefetchHooks Function({
        bool serverPropertiesRefs,
        bool backupsRefs,
        bool consoleLogsRefs,
      })
    >;
typedef $$ServerPropertiesTableCreateCompanionBuilder =
    ServerPropertiesCompanion Function({
      Value<int> id,
      required int serverId,
      required String key,
      required String value,
    });
typedef $$ServerPropertiesTableUpdateCompanionBuilder =
    ServerPropertiesCompanion Function({
      Value<int> id,
      Value<int> serverId,
      Value<String> key,
      Value<String> value,
    });

final class $$ServerPropertiesTableReferences
    extends
        BaseReferences<_$AppDatabase, $ServerPropertiesTable, ServerProperty> {
  $$ServerPropertiesTableReferences(
    super.$_db,
    super.$_table,
    super.$_typedResult,
  );

  static $ServersTable _serverIdTable(_$AppDatabase db) =>
      db.servers.createAlias(
        $_aliasNameGenerator(db.serverProperties.serverId, db.servers.id),
      );

  $$ServersTableProcessedTableManager get serverId {
    final $_column = $_itemColumn<int>('server_id')!;

    final manager = $$ServersTableTableManager(
      $_db,
      $_db.servers,
    ).filter((f) => f.id.sqlEquals($_column));
    final item = $_typedResult.readTableOrNull(_serverIdTable($_db));
    if (item == null) return manager;
    return ProcessedTableManager(
      manager.$state.copyWith(prefetchedData: [item]),
    );
  }
}

class $$ServerPropertiesTableFilterComposer
    extends Composer<_$AppDatabase, $ServerPropertiesTable> {
  $$ServerPropertiesTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get key => $composableBuilder(
    column: $table.key,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get value => $composableBuilder(
    column: $table.value,
    builder: (column) => ColumnFilters(column),
  );

  $$ServersTableFilterComposer get serverId {
    final $$ServersTableFilterComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.serverId,
      referencedTable: $db.servers,
      getReferencedColumn: (t) => t.id,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServersTableFilterComposer(
            $db: $db,
            $table: $db.servers,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return composer;
  }
}

class $$ServerPropertiesTableOrderingComposer
    extends Composer<_$AppDatabase, $ServerPropertiesTable> {
  $$ServerPropertiesTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get key => $composableBuilder(
    column: $table.key,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get value => $composableBuilder(
    column: $table.value,
    builder: (column) => ColumnOrderings(column),
  );

  $$ServersTableOrderingComposer get serverId {
    final $$ServersTableOrderingComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.serverId,
      referencedTable: $db.servers,
      getReferencedColumn: (t) => t.id,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServersTableOrderingComposer(
            $db: $db,
            $table: $db.servers,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return composer;
  }
}

class $$ServerPropertiesTableAnnotationComposer
    extends Composer<_$AppDatabase, $ServerPropertiesTable> {
  $$ServerPropertiesTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get key =>
      $composableBuilder(column: $table.key, builder: (column) => column);

  GeneratedColumn<String> get value =>
      $composableBuilder(column: $table.value, builder: (column) => column);

  $$ServersTableAnnotationComposer get serverId {
    final $$ServersTableAnnotationComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.serverId,
      referencedTable: $db.servers,
      getReferencedColumn: (t) => t.id,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServersTableAnnotationComposer(
            $db: $db,
            $table: $db.servers,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return composer;
  }
}

class $$ServerPropertiesTableTableManager
    extends
        RootTableManager<
          _$AppDatabase,
          $ServerPropertiesTable,
          ServerProperty,
          $$ServerPropertiesTableFilterComposer,
          $$ServerPropertiesTableOrderingComposer,
          $$ServerPropertiesTableAnnotationComposer,
          $$ServerPropertiesTableCreateCompanionBuilder,
          $$ServerPropertiesTableUpdateCompanionBuilder,
          (ServerProperty, $$ServerPropertiesTableReferences),
          ServerProperty,
          PrefetchHooks Function({bool serverId})
        > {
  $$ServerPropertiesTableTableManager(
    _$AppDatabase db,
    $ServerPropertiesTable table,
  ) : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$ServerPropertiesTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$ServerPropertiesTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$ServerPropertiesTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                Value<int> serverId = const Value.absent(),
                Value<String> key = const Value.absent(),
                Value<String> value = const Value.absent(),
              }) => ServerPropertiesCompanion(
                id: id,
                serverId: serverId,
                key: key,
                value: value,
              ),
          createCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                required int serverId,
                required String key,
                required String value,
              }) => ServerPropertiesCompanion.insert(
                id: id,
                serverId: serverId,
                key: key,
                value: value,
              ),
          withReferenceMapper: (p0) => p0
              .map(
                (e) => (
                  e.readTable(table),
                  $$ServerPropertiesTableReferences(db, table, e),
                ),
              )
              .toList(),
          prefetchHooksCallback: ({serverId = false}) {
            return PrefetchHooks(
              db: db,
              explicitlyWatchedTables: [],
              addJoins:
                  <
                    T extends TableManagerState<
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic
                    >
                  >(state) {
                    if (serverId) {
                      state =
                          state.withJoin(
                                currentTable: table,
                                currentColumn: table.serverId,
                                referencedTable:
                                    $$ServerPropertiesTableReferences
                                        ._serverIdTable(db),
                                referencedColumn:
                                    $$ServerPropertiesTableReferences
                                        ._serverIdTable(db)
                                        .id,
                              )
                              as T;
                    }

                    return state;
                  },
              getPrefetchedDataCallback: (items) async {
                return [];
              },
            );
          },
        ),
      );
}

typedef $$ServerPropertiesTableProcessedTableManager =
    ProcessedTableManager<
      _$AppDatabase,
      $ServerPropertiesTable,
      ServerProperty,
      $$ServerPropertiesTableFilterComposer,
      $$ServerPropertiesTableOrderingComposer,
      $$ServerPropertiesTableAnnotationComposer,
      $$ServerPropertiesTableCreateCompanionBuilder,
      $$ServerPropertiesTableUpdateCompanionBuilder,
      (ServerProperty, $$ServerPropertiesTableReferences),
      ServerProperty,
      PrefetchHooks Function({bool serverId})
    >;
typedef $$BackupsTableCreateCompanionBuilder =
    BackupsCompanion Function({
      Value<int> id,
      required int serverId,
      required String name,
      required String path,
      required int size,
      Value<DateTime?> createdAt,
    });
typedef $$BackupsTableUpdateCompanionBuilder =
    BackupsCompanion Function({
      Value<int> id,
      Value<int> serverId,
      Value<String> name,
      Value<String> path,
      Value<int> size,
      Value<DateTime?> createdAt,
    });

final class $$BackupsTableReferences
    extends BaseReferences<_$AppDatabase, $BackupsTable, Backup> {
  $$BackupsTableReferences(super.$_db, super.$_table, super.$_typedResult);

  static $ServersTable _serverIdTable(_$AppDatabase db) => db.servers
      .createAlias($_aliasNameGenerator(db.backups.serverId, db.servers.id));

  $$ServersTableProcessedTableManager get serverId {
    final $_column = $_itemColumn<int>('server_id')!;

    final manager = $$ServersTableTableManager(
      $_db,
      $_db.servers,
    ).filter((f) => f.id.sqlEquals($_column));
    final item = $_typedResult.readTableOrNull(_serverIdTable($_db));
    if (item == null) return manager;
    return ProcessedTableManager(
      manager.$state.copyWith(prefetchedData: [item]),
    );
  }
}

class $$BackupsTableFilterComposer
    extends Composer<_$AppDatabase, $BackupsTable> {
  $$BackupsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get name => $composableBuilder(
    column: $table.name,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get path => $composableBuilder(
    column: $table.path,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get size => $composableBuilder(
    column: $table.size,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnFilters(column),
  );

  $$ServersTableFilterComposer get serverId {
    final $$ServersTableFilterComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.serverId,
      referencedTable: $db.servers,
      getReferencedColumn: (t) => t.id,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServersTableFilterComposer(
            $db: $db,
            $table: $db.servers,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return composer;
  }
}

class $$BackupsTableOrderingComposer
    extends Composer<_$AppDatabase, $BackupsTable> {
  $$BackupsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get name => $composableBuilder(
    column: $table.name,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get path => $composableBuilder(
    column: $table.path,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get size => $composableBuilder(
    column: $table.size,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnOrderings(column),
  );

  $$ServersTableOrderingComposer get serverId {
    final $$ServersTableOrderingComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.serverId,
      referencedTable: $db.servers,
      getReferencedColumn: (t) => t.id,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServersTableOrderingComposer(
            $db: $db,
            $table: $db.servers,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return composer;
  }
}

class $$BackupsTableAnnotationComposer
    extends Composer<_$AppDatabase, $BackupsTable> {
  $$BackupsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get name =>
      $composableBuilder(column: $table.name, builder: (column) => column);

  GeneratedColumn<String> get path =>
      $composableBuilder(column: $table.path, builder: (column) => column);

  GeneratedColumn<int> get size =>
      $composableBuilder(column: $table.size, builder: (column) => column);

  GeneratedColumn<DateTime> get createdAt =>
      $composableBuilder(column: $table.createdAt, builder: (column) => column);

  $$ServersTableAnnotationComposer get serverId {
    final $$ServersTableAnnotationComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.serverId,
      referencedTable: $db.servers,
      getReferencedColumn: (t) => t.id,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServersTableAnnotationComposer(
            $db: $db,
            $table: $db.servers,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return composer;
  }
}

class $$BackupsTableTableManager
    extends
        RootTableManager<
          _$AppDatabase,
          $BackupsTable,
          Backup,
          $$BackupsTableFilterComposer,
          $$BackupsTableOrderingComposer,
          $$BackupsTableAnnotationComposer,
          $$BackupsTableCreateCompanionBuilder,
          $$BackupsTableUpdateCompanionBuilder,
          (Backup, $$BackupsTableReferences),
          Backup,
          PrefetchHooks Function({bool serverId})
        > {
  $$BackupsTableTableManager(_$AppDatabase db, $BackupsTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$BackupsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$BackupsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$BackupsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                Value<int> serverId = const Value.absent(),
                Value<String> name = const Value.absent(),
                Value<String> path = const Value.absent(),
                Value<int> size = const Value.absent(),
                Value<DateTime?> createdAt = const Value.absent(),
              }) => BackupsCompanion(
                id: id,
                serverId: serverId,
                name: name,
                path: path,
                size: size,
                createdAt: createdAt,
              ),
          createCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                required int serverId,
                required String name,
                required String path,
                required int size,
                Value<DateTime?> createdAt = const Value.absent(),
              }) => BackupsCompanion.insert(
                id: id,
                serverId: serverId,
                name: name,
                path: path,
                size: size,
                createdAt: createdAt,
              ),
          withReferenceMapper: (p0) => p0
              .map(
                (e) => (
                  e.readTable(table),
                  $$BackupsTableReferences(db, table, e),
                ),
              )
              .toList(),
          prefetchHooksCallback: ({serverId = false}) {
            return PrefetchHooks(
              db: db,
              explicitlyWatchedTables: [],
              addJoins:
                  <
                    T extends TableManagerState<
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic
                    >
                  >(state) {
                    if (serverId) {
                      state =
                          state.withJoin(
                                currentTable: table,
                                currentColumn: table.serverId,
                                referencedTable: $$BackupsTableReferences
                                    ._serverIdTable(db),
                                referencedColumn: $$BackupsTableReferences
                                    ._serverIdTable(db)
                                    .id,
                              )
                              as T;
                    }

                    return state;
                  },
              getPrefetchedDataCallback: (items) async {
                return [];
              },
            );
          },
        ),
      );
}

typedef $$BackupsTableProcessedTableManager =
    ProcessedTableManager<
      _$AppDatabase,
      $BackupsTable,
      Backup,
      $$BackupsTableFilterComposer,
      $$BackupsTableOrderingComposer,
      $$BackupsTableAnnotationComposer,
      $$BackupsTableCreateCompanionBuilder,
      $$BackupsTableUpdateCompanionBuilder,
      (Backup, $$BackupsTableReferences),
      Backup,
      PrefetchHooks Function({bool serverId})
    >;
typedef $$ConsoleLogsTableCreateCompanionBuilder =
    ConsoleLogsCompanion Function({
      Value<int> id,
      required int serverId,
      required String line,
      Value<int> lineType,
      Value<DateTime?> timestamp,
    });
typedef $$ConsoleLogsTableUpdateCompanionBuilder =
    ConsoleLogsCompanion Function({
      Value<int> id,
      Value<int> serverId,
      Value<String> line,
      Value<int> lineType,
      Value<DateTime?> timestamp,
    });

final class $$ConsoleLogsTableReferences
    extends BaseReferences<_$AppDatabase, $ConsoleLogsTable, ConsoleLog> {
  $$ConsoleLogsTableReferences(super.$_db, super.$_table, super.$_typedResult);

  static $ServersTable _serverIdTable(_$AppDatabase db) =>
      db.servers.createAlias(
        $_aliasNameGenerator(db.consoleLogs.serverId, db.servers.id),
      );

  $$ServersTableProcessedTableManager get serverId {
    final $_column = $_itemColumn<int>('server_id')!;

    final manager = $$ServersTableTableManager(
      $_db,
      $_db.servers,
    ).filter((f) => f.id.sqlEquals($_column));
    final item = $_typedResult.readTableOrNull(_serverIdTable($_db));
    if (item == null) return manager;
    return ProcessedTableManager(
      manager.$state.copyWith(prefetchedData: [item]),
    );
  }
}

class $$ConsoleLogsTableFilterComposer
    extends Composer<_$AppDatabase, $ConsoleLogsTable> {
  $$ConsoleLogsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get line => $composableBuilder(
    column: $table.line,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get lineType => $composableBuilder(
    column: $table.lineType,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get timestamp => $composableBuilder(
    column: $table.timestamp,
    builder: (column) => ColumnFilters(column),
  );

  $$ServersTableFilterComposer get serverId {
    final $$ServersTableFilterComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.serverId,
      referencedTable: $db.servers,
      getReferencedColumn: (t) => t.id,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServersTableFilterComposer(
            $db: $db,
            $table: $db.servers,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return composer;
  }
}

class $$ConsoleLogsTableOrderingComposer
    extends Composer<_$AppDatabase, $ConsoleLogsTable> {
  $$ConsoleLogsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get line => $composableBuilder(
    column: $table.line,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get lineType => $composableBuilder(
    column: $table.lineType,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get timestamp => $composableBuilder(
    column: $table.timestamp,
    builder: (column) => ColumnOrderings(column),
  );

  $$ServersTableOrderingComposer get serverId {
    final $$ServersTableOrderingComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.serverId,
      referencedTable: $db.servers,
      getReferencedColumn: (t) => t.id,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServersTableOrderingComposer(
            $db: $db,
            $table: $db.servers,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return composer;
  }
}

class $$ConsoleLogsTableAnnotationComposer
    extends Composer<_$AppDatabase, $ConsoleLogsTable> {
  $$ConsoleLogsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get line =>
      $composableBuilder(column: $table.line, builder: (column) => column);

  GeneratedColumn<int> get lineType =>
      $composableBuilder(column: $table.lineType, builder: (column) => column);

  GeneratedColumn<DateTime> get timestamp =>
      $composableBuilder(column: $table.timestamp, builder: (column) => column);

  $$ServersTableAnnotationComposer get serverId {
    final $$ServersTableAnnotationComposer composer = $composerBuilder(
      composer: this,
      getCurrentColumn: (t) => t.serverId,
      referencedTable: $db.servers,
      getReferencedColumn: (t) => t.id,
      builder:
          (
            joinBuilder, {
            $addJoinBuilderToRootComposer,
            $removeJoinBuilderFromRootComposer,
          }) => $$ServersTableAnnotationComposer(
            $db: $db,
            $table: $db.servers,
            $addJoinBuilderToRootComposer: $addJoinBuilderToRootComposer,
            joinBuilder: joinBuilder,
            $removeJoinBuilderFromRootComposer:
                $removeJoinBuilderFromRootComposer,
          ),
    );
    return composer;
  }
}

class $$ConsoleLogsTableTableManager
    extends
        RootTableManager<
          _$AppDatabase,
          $ConsoleLogsTable,
          ConsoleLog,
          $$ConsoleLogsTableFilterComposer,
          $$ConsoleLogsTableOrderingComposer,
          $$ConsoleLogsTableAnnotationComposer,
          $$ConsoleLogsTableCreateCompanionBuilder,
          $$ConsoleLogsTableUpdateCompanionBuilder,
          (ConsoleLog, $$ConsoleLogsTableReferences),
          ConsoleLog,
          PrefetchHooks Function({bool serverId})
        > {
  $$ConsoleLogsTableTableManager(_$AppDatabase db, $ConsoleLogsTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$ConsoleLogsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$ConsoleLogsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$ConsoleLogsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                Value<int> serverId = const Value.absent(),
                Value<String> line = const Value.absent(),
                Value<int> lineType = const Value.absent(),
                Value<DateTime?> timestamp = const Value.absent(),
              }) => ConsoleLogsCompanion(
                id: id,
                serverId: serverId,
                line: line,
                lineType: lineType,
                timestamp: timestamp,
              ),
          createCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                required int serverId,
                required String line,
                Value<int> lineType = const Value.absent(),
                Value<DateTime?> timestamp = const Value.absent(),
              }) => ConsoleLogsCompanion.insert(
                id: id,
                serverId: serverId,
                line: line,
                lineType: lineType,
                timestamp: timestamp,
              ),
          withReferenceMapper: (p0) => p0
              .map(
                (e) => (
                  e.readTable(table),
                  $$ConsoleLogsTableReferences(db, table, e),
                ),
              )
              .toList(),
          prefetchHooksCallback: ({serverId = false}) {
            return PrefetchHooks(
              db: db,
              explicitlyWatchedTables: [],
              addJoins:
                  <
                    T extends TableManagerState<
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic,
                      dynamic
                    >
                  >(state) {
                    if (serverId) {
                      state =
                          state.withJoin(
                                currentTable: table,
                                currentColumn: table.serverId,
                                referencedTable: $$ConsoleLogsTableReferences
                                    ._serverIdTable(db),
                                referencedColumn: $$ConsoleLogsTableReferences
                                    ._serverIdTable(db)
                                    .id,
                              )
                              as T;
                    }

                    return state;
                  },
              getPrefetchedDataCallback: (items) async {
                return [];
              },
            );
          },
        ),
      );
}

typedef $$ConsoleLogsTableProcessedTableManager =
    ProcessedTableManager<
      _$AppDatabase,
      $ConsoleLogsTable,
      ConsoleLog,
      $$ConsoleLogsTableFilterComposer,
      $$ConsoleLogsTableOrderingComposer,
      $$ConsoleLogsTableAnnotationComposer,
      $$ConsoleLogsTableCreateCompanionBuilder,
      $$ConsoleLogsTableUpdateCompanionBuilder,
      (ConsoleLog, $$ConsoleLogsTableReferences),
      ConsoleLog,
      PrefetchHooks Function({bool serverId})
    >;

class $AppDatabaseManager {
  final _$AppDatabase _db;
  $AppDatabaseManager(this._db);
  $$ServersTableTableManager get servers =>
      $$ServersTableTableManager(_db, _db.servers);
  $$ServerPropertiesTableTableManager get serverProperties =>
      $$ServerPropertiesTableTableManager(_db, _db.serverProperties);
  $$BackupsTableTableManager get backups =>
      $$BackupsTableTableManager(_db, _db.backups);
  $$ConsoleLogsTableTableManager get consoleLogs =>
      $$ConsoleLogsTableTableManager(_db, _db.consoleLogs);
}
