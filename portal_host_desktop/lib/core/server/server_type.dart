enum ServerType {
  paper('Paper', 'paper', true, true),
  vanilla('Vanilla', 'vanilla', true, false),
  forge('Forge', 'forge', true, true),
  neoforge('NeoForge', 'neoforge', true, true),
  purpur('Purpur', 'purpur', true, true),
  folia('Folia', 'folia', true, true),
  fabric('Fabric', 'fabric', true, true);

  final String displayName;
  final String key;
  final bool supportsVersion;
  final bool supportsBuild;

  const ServerType(this.displayName, this.key, this.supportsVersion, this.supportsBuild);

  static ServerType fromKey(String key) =>
      ServerType.values.firstWhere((t) => t.key == key, orElse: () => paper);

  static ServerType fromDisplayName(String name) =>
      ServerType.values.firstWhere((t) => t.displayName == name, orElse: () => paper);
}
