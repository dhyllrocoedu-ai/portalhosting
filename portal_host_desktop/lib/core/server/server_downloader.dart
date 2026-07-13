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

  String get label => build ?? 'latest';
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
    final headers = {'User-Agent': 'PortalHost/1.0', 'Accept': 'application/json'};
    // Use PaperMC Fill API v3 (v2 is sunset)
    final endpoints = [
      'https://fill.papermc.io/v3/projects/paper/versions',
      'https://api.papermc.io/v3/projects/paper/versions',
    ];

    for (final endpoint in endpoints) {
      final res = await http.get(Uri.parse(endpoint), headers: headers);
      print('[PaperProvider] GET $endpoint -> ${res.statusCode}');
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        List<String> versions = [];

        if (data['versions'] is List) {
          // Fill API v3 returns: {"versions": [{"version":{"id":"1.21.4"...}}, ...]}
          final versionsList = data['versions'] as List;
          for (final item in versionsList) {
            if (item is Map && item['version'] is Map && item['version']['id'] is String) {
              versions.add(item['version']['id'] as String);
            }
          }
        } else if (data['version_groups'] is List) {
          for (final group in data['version_groups']) {
            if (group['versions'] is List) {
              for (final v in group['versions']) {
                if (v is Map && v['version'] is Map && v['version']['id'] is String) {
                  versions.add(v['version']['id'] as String);
                }
              }
            }
          }
        } else if (data['project_id'] != null && data['versions'] is Map) {
          versions = (data['versions'] as Map?)?.keys.cast<String>().toList() ?? [];
        }

        if (versions.isNotEmpty) {
          print('[PaperProvider] Got ${versions.length} versions from $endpoint');
          return versions.reversed.toList();
        }
      }
      if (res.statusCode == 410) {
        print('[PaperProvider] $endpoint -> 410 Gone, trying next...');
        continue;
      }
    }
    throw Exception('Failed to fetch Paper versions from all endpoints. PaperMC API may be unavailable.');
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    final headers = {'User-Agent': 'PortalHost/1.0', 'Accept': 'application/json'};
    final buildEndpoints = [
      'https://fill.papermc.io/v3/projects/paper/versions/$version/builds',
      'https://api.papermc.io/v3/projects/paper/versions/$version/builds',
      'https://fill.papermc.io/v3/projects/paper/versions/$version',
    ];

    for (final endpoint in buildEndpoints) {
      final res = await http.get(Uri.parse(endpoint), headers: headers);
      print('[PaperProvider] GET $endpoint -> ${res.statusCode}');
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        final builds = (data['builds'] as List?)?.cast<int>() ?? [];
        final result = <BuildInfo>[];
        for (final build in builds.reversed.take(10)) {
          final bRes = await http.get(Uri.parse(
              'https://fill.papermc.io/v3/projects/paper/versions/$version/builds/$build'), headers: headers);
          if (bRes.statusCode != 200) continue;
          final bData = jsonDecode(bRes.body) as Map<String, dynamic>;
          final downloads = bData['downloads'] as Map<String, dynamic>?;
          if (downloads == null) continue;
          final application = downloads['server:default'] as Map<String, dynamic>?;
          if (application == null || !application.containsKey('name')) continue;
          result.add(BuildInfo(
            version: version,
            build: '$build',
            url: application['url'] as String,
            sha256: application['checksums']?['sha256'] as String?,
          ));
        }
        if (result.isNotEmpty) return result;
      }
      if (res.statusCode == 410) continue;
    }
    return [];
  }
}

class VanillaProvider extends ServerProvider {
  @override
  String get name => 'Vanilla';

  @override
  Future<List<String>> getVersions() async {
    final res = await http.get(Uri.parse(
        'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json'));
    if (res.statusCode != 200) {
      throw Exception('Failed to fetch Vanilla versions: HTTP ${res.statusCode}');
    }
    final data = jsonDecode(res.body) as Map;
    final versions = data['versions'] as List;
    return versions
        .where((v) => v is Map && v['id'] is String)
        .map((v) => v['id'] as String)
        .where((id) => !id.contains('snapshot') && !id.contains('pre') && !id.contains('rc'))
        .take(20)
        .toList();
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    final res = await http.get(Uri.parse(
        'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json'));
    if (res.statusCode != 200) {
      throw Exception('Failed to fetch Vanilla manifest: HTTP ${res.statusCode}');
    }
    final data = jsonDecode(res.body) as Map;
    final versions = data['versions'] as List;
    Map<String, dynamic>? match;
    for (final v in versions) {
      if (v is Map && v['id'] == version) {
        match = v as Map<String, dynamic>;
        break;
      }
    }
    if (match == null || !match.containsKey('url')) return [];
    final vRes = await http.get(Uri.parse(match['url'] as String));
    if (vRes.statusCode != 200) return [];
    final vData = jsonDecode(vRes.body) as Map;
    final downloads = vData['downloads'] as Map?;
    if (downloads == null) return [];
    final server = downloads['server'] as Map?;
    if (server == null || !server.containsKey('url')) return [];
    return [
      BuildInfo(
        version: version,
        url: server['url'] as String,
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
    if (res.statusCode != 200) {
      throw Exception('Failed to fetch Forge versions: HTTP ${res.statusCode}');
    }
    final data = jsonDecode(res.body) as Map<String, dynamic>;
    return data.keys.toList().take(20).toList();
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    try {
      final res = await http.get(Uri.parse(
          'https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json'));
      if (res.statusCode != 200) return [];
      final data = jsonDecode(res.body) as Map<String, dynamic>;
      final versionData = data[version];
      if (versionData is! Map<String, dynamic>) return [];

      final builds = versionData['builds'];
      if (builds is! List) return [];

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
    } catch (_) {
      return [];
    }
  }
}

class NeoForgeProvider extends ServerProvider {
  @override
  String get name => 'NeoForge';

  @override
  Future<List<String>> getVersions() async {
    final res = await http.get(Uri.parse(
        'https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml'));
    if (res.statusCode != 200) {
      throw Exception('Failed to fetch NeoForge versions: HTTP ${res.statusCode}');
    }
    final body = res.body;
    final versions = RegExp(r'<version>(.*?)</version>')
        .allMatches(body)
        .map((m) => m.group(1)!)
        .toList();
    return versions.reversed.take(20).toList();
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    try {
      return [
        BuildInfo(
          version: version,
          url:
              'https://maven.neoforged.net/releases/net/neoforged/neoforge/$version/neoforge-$version-installer.jar',
        ),
      ];
    } catch (_) {
      return [];
    }
  }
}

class PurpurProvider extends ServerProvider {
  @override
  String get name => 'Purpur';

  @override
  Future<List<String>> getVersions() async {
    final res = await http.get(Uri.parse(
        'https://api.purpurmc.org/v2/purpur'));
    if (res.statusCode != 200) {
      throw Exception('Failed to fetch Purpur versions: HTTP ${res.statusCode}');
    }
    final data = jsonDecode(res.body) as Map<String, dynamic>;
    final versionsRaw = data['versions'];
    List<String> versions = [];
    if (versionsRaw is List) {
      versions = versionsRaw.whereType<String>().toList();
    } else if (versionsRaw is Map) {
      versions = versionsRaw.keys.whereType<String>().toList();
    }
    return versions.reversed.take(20).toList();
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    try {
      final res = await http.get(Uri.parse(
          'https://api.purpurmc.org/v2/purpur/$version'));
      if (res.statusCode != 200) return [];
      final data = jsonDecode(res.body) as Map<String, dynamic>;
      final buildsRaw = data['builds'];
      final result = <BuildInfo>[];
      if (buildsRaw is List) {
        for (final entry in buildsRaw.reversed.take(10)) {
          if (entry is Map) {
            final build = entry['build']?.toString();
            if (build == null) continue;
            result.add(BuildInfo(
              version: version,
              build: build,
              url: 'https://api.purpurmc.org/v2/purpur/$version/$build/download',
            ));
          }
        }
      } else if (buildsRaw is Map) {
        final builds = buildsRaw as Map<String, dynamic>;
        for (final build in builds.keys.toList().reversed.take(10)) {
          result.add(BuildInfo(
            version: version,
            build: build,
            url: 'https://api.purpurmc.org/v2/purpur/$version/$build/download',
          ));
        }
      }
      return result;
    } catch (_) {
      return [];
    }
  }
}

class FoliaProvider extends ServerProvider {
  @override
  String get name => 'Folia';

  @override
  Future<List<String>> getVersions() async {
    final headers = {'User-Agent': 'PortalHost/1.0', 'Accept': 'application/json'};
    // Use PaperMC Fill API v3 (v2 is sunset)
    final endpoints = [
      'https://fill.papermc.io/v3/projects/folia/versions',
      'https://api.papermc.io/v3/projects/folia/versions',
    ];

    for (final endpoint in endpoints) {
      final res = await http.get(Uri.parse(endpoint), headers: headers);
      print('[FoliaProvider] GET $endpoint -> ${res.statusCode}');
      print('[FoliaProvider] Body: ${res.body}');
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        List<String> versions = [];

        if (data['versions'] is List) {
          // Fill API v3 returns: {"versions":[{"version":{"id":"1.21.4"...}}, ...]}
          final versionsList = data['versions'] as List;
          for (final item in versionsList) {
            if (item is Map && item['version'] is Map && item['version']['id'] is String) {
              versions.add(item['version']['id'] as String);
            }
          }
        } else if (data['version_groups'] is List) {
          for (final group in data['version_groups']) {
            if (group['versions'] is List) {
              for (final v in group['versions']) {
                if (v is Map && v['version'] is Map && v['version']['id'] is String) {
                  versions.add(v['version']['id'] as String);
                }
              }
            }
          }
        } else if (data['project_id'] != null && data['versions'] is Map) {
          versions = (data['versions'] as Map?)?.keys.cast<String>().toList() ?? [];
        }

        if (versions.isNotEmpty) {
          print('[FoliaProvider] Got ${versions.length} versions from $endpoint');
          return versions.reversed.take(20).toList();
        }
      }
      if (res.statusCode == 410) {
        print('[FoliaProvider] $endpoint -> 410 Gone, trying next...');
        continue;
      }
    }
    throw Exception('Failed to fetch Folia versions from all endpoints. PaperMC API may be unavailable.');
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    final headers = {'User-Agent': 'PortalHost/1.0', 'Accept': 'application/json'};
    final buildEndpoints = [
      'https://fill.papermc.io/v3/projects/folia/versions/$version/builds',
      'https://api.papermc.io/v3/projects/folia/versions/$version/builds',
    ];

    for (final endpoint in buildEndpoints) {
      final res = await http.get(Uri.parse(endpoint), headers: headers);
      print('[FoliaProvider] GET $endpoint -> ${res.statusCode}');
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body) as Map<String, dynamic>;
        final builds = (data['builds'] as List?)?.whereType<int>().toList() ?? [];
        final result = <BuildInfo>[];
        for (final build in builds.reversed.take(10)) {
          final bRes = await http.get(Uri.parse(
              'https://fill.papermc.io/v3/projects/folia/versions/$version/builds/$build'), headers: headers);
          if (bRes.statusCode != 200) continue;
          final bData = jsonDecode(bRes.body) as Map<String, dynamic>;
          final downloads = bData['downloads'] as Map<String, dynamic>?;
          if (downloads == null) continue;
          final info = downloads['server:default'] as Map<String, dynamic>?;
          if (info == null || !info.containsKey('name')) continue;
          result.add(BuildInfo(
            version: version,
            build: '$build',
            url: info['url'] as String,
            sha256: info['checksums']?['sha256'] as String?,
          ));
        }
        if (result.isNotEmpty) return result;
      }
      if (res.statusCode == 410) continue;
    }
    return [];
  }
}

class FabricProvider extends ServerProvider {
  @override
  String get name => 'Fabric';

  @override
  Future<List<String>> getVersions() async {
    try {
      final res = await http.get(Uri.parse(
          'https://meta.fabricmc.net/v2/versions/game'));
      if (res.statusCode != 200) {
        throw Exception('Failed to fetch Fabric versions: HTTP ${res.statusCode}');
      }
      final data = jsonDecode(res.body) as List;
      return data
          .where((v) => v is Map && v['stable'] != false && v['version'] is String)
          .map((v) => v['version'] as String)
          .take(20)
          .toList();
    } catch (e) {
      return [];
    }
  }

  @override
  Future<List<BuildInfo>> getBuilds(String version) async {
    try {
      final res = await http.get(Uri.parse(
          'https://meta.fabricmc.net/v2/versions/loader/$version'));
      if (res.statusCode != 200) return [];
      final data = jsonDecode(res.body) as List;
      if (data.isEmpty) return [];
      final loader = data.first['loader'] as Map?;
      final installer = data.first['intermediary'] as Map?;
      if (loader == null || installer == null) return [];
      final loaderVersion = loader['version'];
      final intermediaryVersion = installer['version'];
      return [
        BuildInfo(
          version: version,
          build: loaderVersion?.toString(),
          url:
              'https://meta.fabricmc.net/v2/versions/loader/$version/$loaderVersion/$intermediaryVersion/server/jar',
        ),
      ];
    } catch (_) {
      return [];
    }
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
