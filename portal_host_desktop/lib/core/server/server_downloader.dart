import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'server_type.dart';

class BuildInfo {
  final String version;
  final String? build;
  final String url;
  final String? sha256;

  const BuildInfo({
    required this.version,
    this.build,
    required this.url,
    this.sha256,
  });
}

abstract class ServerProvider {
  String get name;
  Future<List<String>> getVersions();
  Future<List<BuildInfo>> getBuilds(String version);
}

class PaperProvider extends ServerProvider {
  @override
  String get name => 'Paper';

  @override
  Future<List<String>> getVersions() async {
    final res = await http.get(Uri.parse(
        'https://api.papermc.io/v2/projects/paper'));
    final data = jsonDecode(res.body) as Map;
    return List<String>.from(data['versions']);
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    final res = await http.get(Uri.parse(
        'https://api.papermc.io/v2/projects/paper/versions/$version'));
    final data = jsonDecode(res.body) as Map;
    final builds = List<int>.from(data['builds']);
    final result = <BuildInfo>[];
    for (final build in builds.reversed.take(10)) {
      final bRes = await http.get(Uri.parse(
          'https://api.papermc.io/v2/projects/paper/versions/$version/builds/$build'));
      final bData = jsonDecode(bRes.body) as Map;
      final info = bData['downloads']['application'] as Map;
      result.add(BuildInfo(
        version: version,
        build: '$build',
        url:
            'https://api.papermc.io/v2/projects/paper/versions/$version/builds/$build/downloads/${info['name']}',
        sha256: info['sha256'] as String?,
      ));
    }
    return result;
  }
}

class VanillaProvider extends ServerProvider {
  @override
  String get name => 'Vanilla';

  @override
  Future<List<String>> getVersions() async {
    final res = await http.get(Uri.parse(
        'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json'));
    final data = jsonDecode(res.body) as Map;
    final versions = data['versions'] as List;
    return versions
        .map((v) => v['id'] as String)
        .where((id) => !id.contains('snapshot') && !id.contains('pre') && !id.contains('rc'))
        .take(20)
        .toList();
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    final res = await http.get(Uri.parse(
        'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json'));
    final data = jsonDecode(res.body) as Map;
    final versions = data['versions'] as List;
    final match = versions.firstWhere(
        (v) => v['id'] == version,
        orElse: () => <String, dynamic>{});
    if (match is! Map || !match.containsKey('url')) return [];
    final vRes = await http.get(Uri.parse(match['url']));
    final vData = jsonDecode(vRes.body) as Map;
    final server = vData['downloads']['server'] as Map;
    return [
      BuildInfo(
        version: version,
        url: server['url'],
        sha256: server['sha1'] as String?,
      ),
    ];
  }
}

class ForgeProvider extends ServerProvider {
  @override
  String get name => 'Forge';

  @override
  Future<List<String>> getVersions() async {
    final res = await http.get(Uri.parse(
        'https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json'));
    final data = jsonDecode(res.body) as Map<String, dynamic>;
    return data.keys.take(20).toList();
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    final res = await http.get(Uri.parse(
        'https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json'));
    final data = jsonDecode(res.body) as Map<String, dynamic>;
    final builds = data[version] as List? ?? [];
    final result = <BuildInfo>[];
    for (final build in builds.take(10)) {
      final buildStr = build.toString();
      result.add(BuildInfo(
        version: version,
        build: buildStr,
        url:
            'https://maven.minecraftforge.net/net/minecraftforge/forge/$version-$buildStr/forge-$version-$buildStr-installer.jar',
      ));
    }
    return result;
  }
}

class NeoForgeProvider extends ServerProvider {
  @override
  String get name => 'NeoForge';

  @override
  Future<List<String>> getVersions() async {
    final res = await http.get(Uri.parse(
        'https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml'));
    final body = res.body;
    final versions = RegExp(r'<version>(.*?)</version>')
        .allMatches(body)
        .map((m) => m.group(1)!)
        .toList();
    return versions.reversed.take(20).toList();
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    return [
      BuildInfo(
        version: version,
        url:
            'https://maven.neoforged.net/releases/net/neoforged/neoforge/$version/neoforge-$version-installer.jar',
      ),
    ];
  }
}

class PurpurProvider extends ServerProvider {
  @override
  String get name => 'Purpur';

  @override
  Future<List<String>> getVersions() async {
    final res = await http.get(Uri.parse(
        'https://api.purpurmc.org/v2/purpur'));
    final data = jsonDecode(res.body) as Map<String, dynamic>;
    final versions = data['versions'] as Map<String, dynamic>;
    return versions.keys.toList().reversed.take(20).toList();
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    final res = await http.get(Uri.parse(
        'https://api.purpurmc.org/v2/purpur/$version'));
    final data = jsonDecode(res.body) as Map<String, dynamic>;
    final builds = data['builds'] as Map<String, dynamic>;
    final result = <BuildInfo>[];
    for (final build in builds.keys.toList().reversed.take(10)) {
      result.add(BuildInfo(
        version: version,
        build: build,
        url: 'https://api.purpurmc.org/v2/purpur/$version/$build/download',
      ));
    }
    return result;
  }
}

class FoliaProvider extends ServerProvider {
  @override
  String get name => 'Folia';

  @override
  Future<List<String>> getVersions() async {
    final res = await http.get(Uri.parse(
        'https://api.papermc.io/v2/projects/folia'));
    final data = jsonDecode(res.body) as Map;
    return List<String>.from(data['versions']);
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    final res = await http.get(Uri.parse(
        'https://api.papermc.io/v2/projects/folia/versions/$version'));
    final data = jsonDecode(res.body) as Map;
    final builds = List<int>.from(data['builds']);
    final result = <BuildInfo>[];
    for (final build in builds.reversed.take(10)) {
      final bRes = await http.get(Uri.parse(
          'https://api.papermc.io/v2/projects/folia/versions/$version/builds/$build'));
      final bData = jsonDecode(bRes.body) as Map;
      final info = bData['downloads']['application'] as Map;
      result.add(BuildInfo(
        version: version,
        build: '$build',
        url:
            'https://api.papermc.io/v2/projects/folia/versions/$version/builds/$build/downloads/${info['name']}',
        sha256: info['sha256'] as String?,
      ));
    }
    return result;
  }
}

class FabricProvider extends ServerProvider {
  @override
  String get name => 'Fabric';

  @override
  Future<List<String>> getVersions() async {
    final res = await http.get(Uri.parse(
        'https://meta.fabricmc.net/v2/versions/game'));
    final data = jsonDecode(res.body) as List;
    return data
        .where((v) => !(v['stable'] == false))
        .map((v) => v['version'] as String)
        .take(20)
        .toList();
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    final res = await http.get(Uri.parse(
        'https://meta.fabricmc.net/v2/versions/loader/$version'));
    final data = jsonDecode(res.body) as List;
    if (data.isEmpty) return [];
    final loader = data.first['loader'] as Map;
    final installer = data.first['intermediary'] as Map;
    final loaderVersion = loader['version'];
    final intermediaryVersion = installer['version'];
    return [
      BuildInfo(
        version: version,
        build: loaderVersion,
        url:
            'https://meta.fabricmc.net/v2/versions/loader/$version/$loaderVersion/$intermediaryVersion/server/jar',
      ),
    ];
  }
}

class ServerDownloader {
  static ServerProvider providerFor(ServerType type) {
    switch (type) {
      case ServerType.paper:
        return PaperProvider();
      case ServerType.vanilla:
        return VanillaProvider();
      case ServerType.forge:
        return ForgeProvider();
      case ServerType.neoforge:
        return NeoForgeProvider();
      case ServerType.purpur:
        return PurpurProvider();
      case ServerType.folia:
        return FoliaProvider();
      case ServerType.fabric:
        return FabricProvider();
    }
  }

  static Future<void> download(
      BuildInfo build, String destPath,
      {void Function(int received, int total)? onProgress}) async {
    final request = http.Request('GET', Uri.parse(build.url));
    final response = await http.Client().send(request);

    final file = File(destPath);
    await file.create(recursive: true);
    final sink = file.openWrite();
    final total = response.contentLength ?? -1;
    var received = 0;

    await for (final chunk in response.stream) {
      sink.add(chunk);
      received += chunk.length;
      onProgress?.call(received, total);
    }

    await sink.close();
  }
}
