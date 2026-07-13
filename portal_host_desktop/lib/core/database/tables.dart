import 'package:drift/drift.dart';

class Servers extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text()();
  TextColumn get jarPath => text()();
  IntColumn get port => integer().withDefault(const Constant(25565))();
  IntColumn get maxPlayers => integer().withDefault(const Constant(20))();
  TextColumn get serverType => text()();
  TextColumn get mcVersion => text().nullable()();
  TextColumn get javaArgs => text().withDefault(const Constant(''))();
  IntColumn get autoBackup => integer().withDefault(const Constant(1))();
  IntColumn get autoRestart => integer().withDefault(const Constant(0))();
  TextColumn get resourcePackUrl => text().nullable()();
  TextColumn get resourcePackSha1 => text().nullable()();
  TextColumn get status => text().withDefault(const Constant('stopped'))();
  TextColumn get javaPath => text().nullable()();
  TextColumn get serverDir => text().withDefault(const Constant(''))();
  TextColumn get iconPath => text().nullable()();
  DateTimeColumn get createdAt => dateTime().nullable()();
}

class ServerProperties extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get serverId => integer().references(Servers, #id)();
  TextColumn get key => text()();
  TextColumn get value => text()();
}

class Backups extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get serverId => integer().references(Servers, #id)();
  TextColumn get name => text()();
  TextColumn get path => text()();
  IntColumn get size => integer()();
  DateTimeColumn get createdAt => dateTime().nullable()();
}

class ConsoleLogs extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get serverId => integer().references(Servers, #id)();
  TextColumn get line => text()();
  IntColumn get lineType => integer().withDefault(const Constant(0))();
  DateTimeColumn get timestamp => dateTime().nullable()();
}
