import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_picker/file_picker.dart';
import 'package:path/path.dart' as p;
import 'package:go_router/go_router.dart';
import '../../core/providers/server_provider.dart';
import '../../core/providers/settings_provider.dart';
import '../../core/server/server_downloader.dart';
import '../../core/server/server_type.dart';
import '../../shared/models/server_config.dart';

enum ChooseSource {
  downloadPaper,
  downloadVanilla,
  downloadFabric,
  downloadForge,
  downloadNeoForge,
  downloadPurpur,
  downloadFolia,
  pickFile,
}

class CreateServerScreen extends ConsumerStatefulWidget {
  const CreateServerScreen({super.key});

  @override
  ConsumerState<CreateServerScreen> createState() =>
      _CreateServerScreenState();
}

class _CreateServerScreenState extends ConsumerState<CreateServerScreen> {
  int _currentStep = 0;
  static const _totalSteps = 6;

  ChooseSource? _source;
  String _jarName = '';
  String? _jarTargetPath;
  bool _downloading = false;
  double _downloadProgress = 0;
  String? _downloadError;
  String _mcVersion = '';
  List<String> _availableVersions = [];
  bool _versionsLoading = false;
  bool _versionsExpanded = false;
  String? _versionsError;
  String _selectedBuildId = '';
  List<BuildInfo> _availableBuilds = [];

  final _nameCtrl = TextEditingController();
  double _minRam = 1.0;
  double _maxRam = 4.0;
  double _maxRamLimit = 16.0;
  final _portCtrl = TextEditingController(text: '25565');
  String _gamemode = 'survival';
  String _difficulty = 'easy';
  final _motdCtrl = TextEditingController(text: 'A Minecraft Server');
  static const _gamemodes = ['survival', 'creative', 'adventure', 'spectator'];
  static const _difficulties = ['peaceful', 'easy', 'normal', 'hard'];
  bool _eulaAccepted = false;
  bool _creating = false;
  String? _serverIconPath;

  @override
  void initState() {
    super.initState();
    _maxRamLimit = _getSystemRam();
  }

  double _getSystemRam() => 16.0;

  ServerProvider? get _provider {
    if (_source == null || _source == ChooseSource.pickFile) return null;
    final type = _sourceToType(_source!);
    if (type == null) return null;
    return ServerDownloader.providerFor(type);
  }

  bool get _supportsBuilds => [
        ServerType.paper,
        ServerType.fabric,
        ServerType.forge,
        ServerType.neoforge,
        ServerType.purpur,
        ServerType.folia,
      ].contains(_sourceToType(_source!));

  bool get _step0Complete =>
      _source != null &&
      _downloadError == null &&
      (_source == ChooseSource.pickFile ||
          (_mcVersion.isNotEmpty && !_downloading));

  Future<void> _selectDownloadSource(ChooseSource src) async {
    setState(() {
      _source = src;
      _mcVersion = '';
      _selectedBuildId = '';
      _jarName = '';
      _jarTargetPath = null;
      _downloadError = null;
      _availableBuilds = [];
      _versionsLoading = true;
      _versionsExpanded = false;
      _versionsError = null;
      _availableVersions = [];
    });

    if (src == ChooseSource.pickFile) {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['jar'],
      );
      if (result != null && result.files.isNotEmpty) {
        setState(() {
          _jarName = result.files.first.name;
          _jarTargetPath = result.files.first.path;
        });
      }
      return;
    }

    try {
      final p = _provider!;
      _availableVersions = await p.getVersions();
    } catch (e) {
      _versionsError = 'Failed to fetch versions: $e';
    }
    setState(() => _versionsLoading = false);
  }

  Future<void> _pickServerIcon() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.image,
    );
    if (result != null && result.files.isNotEmpty) {
      setState(() {
        _serverIconPath = result.files.first.path;
      });
    }
  }

  Future<void> _loadBuilds() async {
    if (_mcVersion.isEmpty || _source == ChooseSource.pickFile) return;
    setState(() => _availableBuilds = []);
    try {
      _availableBuilds = await _provider!.getBuilds(_mcVersion);
      if (_availableBuilds.isNotEmpty) {
        _selectedBuildId = _availableBuilds.first.build ?? '';
      }
    } catch (_) {}
  }

  Future<void> _startDownload() async {
    if (_source == ChooseSource.pickFile || _mcVersion.isEmpty) return;
    setState(() {
      _downloading = true;
      _downloadError = null;
      _downloadProgress = 0;
    });

    try {
      final p = _provider!;
      final builds = await p.getBuilds(_mcVersion);
      if (builds.isEmpty) {
        throw Exception('No downloads available');
      }
      final build = _selectedBuildId.isNotEmpty
          ? builds.firstWhere(
              (b) => b.build == _selectedBuildId,
              orElse: () => builds.first)
          : builds.first;
      _jarName = build.label;
      final dir = Directory.systemTemp.path;
      final destPath = '$dir\\${_source!.name}.jar';
      await ServerDownloader.download(build, destPath,
          onProgress: (rec, total) {
        setState(() => _downloadProgress = total > 0 ? rec / total : 0);
      });
      setState(() => _jarTargetPath = destPath);
    } catch (e) {
      setState(() => _downloadError = '$e');
    }
    setState(() => _downloading = false);
  }

  Future<void> _create() async {
    setState(() => _creating = true);
    try {
      final serverTypeLabel = switch (_source) {
        null => 'custom',
        ChooseSource.downloadPaper => 'paper',
        ChooseSource.downloadVanilla => 'vanilla',
        ChooseSource.downloadFabric => 'fabric',
        ChooseSource.downloadForge => 'forge',
        ChooseSource.downloadNeoForge => 'neoforge',
        ChooseSource.downloadPurpur => 'purpur',
        ChooseSource.downloadFolia => 'folia',
        ChooseSource.pickFile => 'custom',
      };

      final serversDir = ref.read(settingsProvider).serversDir;
      final serverName = _nameCtrl.text.trim();
      final serverPath = Directory(p.join(serversDir, serverName));
      await serverPath.create(recursive: true);

      String? iconPath;
      if (_serverIconPath != null) {
        final iconFile = File(_serverIconPath!);
        final iconDest = File(p.join(serverPath.path, 'server-icon.png'));
        await iconFile.copy(iconDest.path);
        iconPath = iconDest.path;
      }

      if (_jarTargetPath != null) {
        final jarSource = File(_jarTargetPath!);
        final jarDest = File(p.join(serverPath.path, 'server.jar'));
        await jarSource.copy(jarDest.path);
      }

      final config = ServerConfig(
        id: 0,
        name: serverName,
        jarPath: p.join(serverPath.path, 'server.jar'),
        port: int.tryParse(_portCtrl.text) ?? 25565,
        maxPlayers: 20,
        serverType: serverTypeLabel,
        mcVersion: _mcVersion.isNotEmpty ? _mcVersion : null,
        javaArgs:
            '-Xms${(_minRam * 1024).toInt()}M -Xmx${(_maxRam * 1024).toInt()}M',
        autoBackup: true,
        autoRestart: false,
        serverDir: serverPath.path,
        iconPath: iconPath,
      );

      await ref.read(serverListProvider.notifier).addServer(config);
      
      await ref.read(serverListProvider.notifier).createServerProperties(
        serverName: serverName,
        serverPath: serverPath.path,
        motd: _motdCtrl.text,
        gamemode: _gamemode,
        difficulty: _difficulty,
        port: int.tryParse(_portCtrl.text) ?? 25565,
        maxPlayers: 20,
        eulaAccepted: _eulaAccepted,
      );

      if (mounted) {
        if (Navigator.of(context).canPop()) {
          Navigator.of(context).pop();
        } else {
          context.go('/servers');
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Error: $e')));
      }
    }
    setState(() => _creating = false);
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _portCtrl.dispose();
    _motdCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Create Server'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            if (_currentStep > 0) {
              setState(() => _currentStep--);
            } else {
              if (Navigator.of(context).canPop()) {
                Navigator.of(context).pop();
              } else {
                context.go('/servers');
              }
            }
          },
        ),
      ),
      body: Column(
        children: [
          _buildStepIndicator(theme),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: Text(
              'Step ${_currentStep + 1} of $_totalSteps',
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ),
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: _buildStep(theme),
            ),
          ),
          _buildNavigation(theme),
        ],
      ),
    );
  }

  Widget _buildStepIndicator(ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      child: Row(
        children: List.generate(_totalSteps, (i) {
          final filled = i <= _currentStep;
          return Expanded(
            child: Container(
              height: 4,
              margin: const EdgeInsets.symmetric(horizontal: 2),
              decoration: BoxDecoration(
                color: filled
                    ? theme.colorScheme.primary
                    : theme.colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          );
        }),
      ),
    );
  }

  Widget _buildStep(ThemeData theme) {
    switch (_currentStep) {
      case 0:
        return _buildStepChooseSource(theme);
      case 1:
        return _buildStepName(theme);
      case 2:
        return _buildStepRam(theme);
      case 3:
        return _buildStepProperties(theme);
      case 4:
        return _buildStepEula(theme);
      case 5:
        return _buildStepReview(theme);
      default:
        return const SizedBox();
    }
  }

  Widget _buildStepChooseSource(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Server Software',
            style: theme.textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.bold)),
        const SizedBox(height: 4),
        Text('Choose a server jar source',
            style: theme.textTheme.bodyMedium
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
        const SizedBox(height: 16),
        _sourceCard('Paper', 'High-performance server software',
            ChooseSource.downloadPaper, Icons.description, theme),
        _sourceCard('Vanilla', 'Official Mojang server jar',
            ChooseSource.downloadVanilla, Icons.description, theme),
        _sourceCard('Fabric', 'Lightweight mod loader',
            ChooseSource.downloadFabric, Icons.extension, theme),
        _sourceCard('Forge', 'Popular mod loader',
            ChooseSource.downloadForge, Icons.build, theme),
        _sourceCard('NeoForge', 'Modern fork of Forge',
            ChooseSource.downloadNeoForge, Icons.build, theme),
        _sourceCard('Purpur', 'High-performance Paper fork',
            ChooseSource.downloadPurpur, Icons.extension, theme),
        _sourceCard('Folia', 'Regionized multithreading Paper fork',
            ChooseSource.downloadFolia, Icons.extension, theme),
        _sourceCard('Pick JAR file', 'Browse local storage',
            ChooseSource.pickFile, Icons.folder_open, theme),
        const SizedBox(height: 16),
        if (_source != null && _source != ChooseSource.pickFile) ...[
          if (_versionsLoading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_versionsError != null)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 16),
              child: Text(
                _versionsError!,
                style: const TextStyle(color: Colors.red),
              ),
            )
          else if (_availableVersions.isNotEmpty) ...[
            const SizedBox(height: 16),
            InkWell(
              onTap: () {
                setState(() {
                  _versionsExpanded = !_versionsExpanded;
                });
              },
              borderRadius: BorderRadius.circular(12),
              child: Ink(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                decoration: BoxDecoration(
                  color: theme.colorScheme.surfaceContainer,
                  border: Border.all(color: theme.colorScheme.outlineVariant),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.layers,
                      color: theme.colorScheme.primary,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Minecraft Version',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                          Text(
                            _mcVersion.isEmpty ? 'Select a version...' : _mcVersion,
                            style: theme.textTheme.bodyLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: _mcVersion.isEmpty
                                  ? theme.colorScheme.onSurfaceVariant
                                  : theme.colorScheme.onSurface,
                            ),
                          ),
                        ],
                      ),
                    ),
                    AnimatedRotation(
                      turns: _versionsExpanded ? 0.5 : 0.0,
                      duration: const Duration(milliseconds: 200),
                      child: Icon(
                        Icons.keyboard_arrow_down,
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            AnimatedSize(
              duration: const Duration(milliseconds: 300),
              curve: Curves.fastOutSlowIn,
              alignment: Alignment.topCenter,
              child: _versionsExpanded
                  ? Container(
                      margin: const EdgeInsets.only(top: 12),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.surfaceContainerLow,
                        border: Border.all(color: theme.colorScheme.outlineVariant),
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: LayoutBuilder(
                        builder: (context, constraints) {
                          final spacing = 8.0;
                          final totalWidth = constraints.maxWidth;
                          final itemWidth =
                              (totalWidth - spacing * 5) / 6;
                          return Wrap(
                            spacing: spacing,
                            runSpacing: spacing,
                            children: [
                              for (final v in _availableVersions)
                                SizedBox(
                                  width: itemWidth,
                                  height: 36,
                                  child: HoverVersionContainer(
                                    version: v,
                                    isSelected: _mcVersion == v,
                                    onTap: () {
                                      setState(() {
                                        _mcVersion = v;
                                        _versionsExpanded = false;
                                      });
                                      _loadBuilds();
                                    },
                                  ),
                                ),
                            ],
                          );
                        },
                      ),
                    )
                  : const SizedBox(width: double.infinity, height: 0),
            ),
          ],
          if (_mcVersion.isNotEmpty && _supportsBuilds)
            _availableBuilds.isEmpty
                ? const Text('Loading builds...')
                : Text(
                    'Latest build: ${_availableBuilds.isNotEmpty ? _availableBuilds.first.label : 'N/A'}',
                    style: const TextStyle(fontWeight: FontWeight.w500)),
          if (_downloading)
            Column(
              children: [
                LinearProgressIndicator(value: _downloadProgress),
                const SizedBox(height: 4),
                Text('Downloading... ${(_downloadProgress * 100).toInt()}%'),
              ],
            ),
          if (_jarTargetPath != null)
            Row(
              children: [
                const Icon(Icons.check_circle, color: Colors.green),
                const SizedBox(width: 8),
                Text('Ready: $_jarName',
                    style: const TextStyle(color: Colors.green)),
              ],
            ),
        ],
        if (_source == ChooseSource.pickFile && _jarTargetPath != null)
          Row(
            children: [
              const Icon(Icons.check_circle, color: Colors.green),
              const SizedBox(width: 8),
              Text('Ready: $_jarName',
                  style: const TextStyle(color: Colors.green)),
            ],
          ),
      ],
    );
  }

  Widget _sourceCard(String title, String subtitle, ChooseSource source,
      IconData icon, ThemeData theme) {
    final selected = _source == source;
    return Card(
      color: selected
          ? theme.colorScheme.primaryContainer
          : null,
      child: ListTile(
        leading: Icon(icon,
            color: selected
                ? theme.colorScheme.onPrimaryContainer
                : theme.colorScheme.primary),
        title: Text(title),
        subtitle: Text(subtitle, style: theme.textTheme.bodySmall),
        trailing: selected
            ? Icon(Icons.check_circle,
                color: theme.colorScheme.onPrimaryContainer)
            : null,
        onTap: () => _selectDownloadSource(source),
      ),
    );
  }

  Widget _buildStepName(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Server Name',
            style: theme.textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.bold)),
        const SizedBox(height: 24),
        TextField(
          controller: _nameCtrl,
          decoration: const InputDecoration(
            labelText: 'Server Name',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 24),
        Row(
          children: [
            if (_serverIconPath != null)
              Image.file(
                File(_serverIconPath!),
                width: 48,
                height: 48,
                fit: BoxFit.cover,
              )
            else
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: theme.colorScheme.surfaceContainerHighest,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(Icons.add_a_photo),
              ),
            const SizedBox(width: 16),
            FilledButton.tonal(
              onPressed: _pickServerIcon,
              child: const Text('Select Icon'),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildStepRam(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Memory (RAM)',
            style: theme.textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.bold)),
        const SizedBox(height: 24),
        Text('Minimum RAM: ${_minRam.toStringAsFixed(1)} GB'),
        Slider(
          value: _minRam,
          min: 0.5,
          max: _maxRam,
          divisions: ((_maxRam - 0.5) / 0.1).round(),
          onChanged: (v) =>
              setState(() => _minRam = (v / 0.1).round() * 0.1),
        ),
        Text('Maximum RAM: ${_maxRam.toStringAsFixed(1)} GB'),
        Slider(
          value: _maxRam,
          min: 0.5,
          max: _maxRamLimit,
          divisions: ((_maxRamLimit - 0.5) / 0.1).round(),
          onChanged: (v) =>
              setState(() => _maxRam = (v / 0.1).round() * 0.1),
        ),
      ],
    );
  }

  Widget _buildStepProperties(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Server Properties',
            style: theme.textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.bold)),
        const SizedBox(height: 24),
        TextField(
          controller: _portCtrl,
          decoration: const InputDecoration(
            labelText: 'Port',
            border: OutlineInputBorder(),
          ),
          keyboardType: TextInputType.number,
        ),
        const SizedBox(height: 16),
        Text('Gamemode', style: theme.textTheme.titleSmall),
        Wrap(
          spacing: 4,
          children: _gamemodes.map((gm) => ChoiceChip(
            label: Text(gm[0].toUpperCase() + gm.substring(1)),
            selected: _gamemode == gm,
            onSelected: (_) => setState(() => _gamemode = gm),
          )).toList(),
        ),
        const SizedBox(height: 16),
        Text('Difficulty', style: theme.textTheme.titleSmall),
        Wrap(
          spacing: 4,
          children: _difficulties.map((d) => ChoiceChip(
            label: Text(d[0].toUpperCase() + d.substring(1)),
            selected: _difficulty == d,
            onSelected: (_) => setState(() => _difficulty = d),
          )).toList(),
        ),
        const SizedBox(height: 16),
        Text('MOTD', style: theme.textTheme.titleSmall),
        TextField(
          controller: _motdCtrl,
          decoration: const InputDecoration(
            labelText: 'Server MOTD (appears in server list)',
            border: OutlineInputBorder(),
          ),
          maxLines: 2,
        ),
        const SizedBox(height: 16),
        Text('Preview', style: theme.textTheme.titleSmall),
        const SizedBox(height: 8),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: theme.colorScheme.surfaceContainer,
            border: Border.all(color: theme.colorScheme.outlineVariant),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Row(
            children: [
              if (_serverIconPath != null)
                Image.file(
                  File(_serverIconPath!),
                  width: 32,
                  height: 32,
                  fit: BoxFit.cover,
                )
              else
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    color: theme.colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: const Icon(Icons.gamepad, size: 16),
                ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  _motdCtrl.text.isEmpty ? 'No MOTD set' : _motdCtrl.text,
                  style: theme.textTheme.bodyMedium,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildStepEula(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('EULA Agreement',
            style: theme.textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.bold)),
        const SizedBox(height: 24),
        Card(
          color: theme.colorScheme.surfaceContainerHighest,
          child: const Padding(
            padding: EdgeInsets.all(16),
            child: Text(
              'By checking the box below, you agree to the Minecraft EULA.\n'
              'The eula.txt file will be created with eula=true.',
            ),
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Checkbox(
              value: _eulaAccepted,
              onChanged: (v) => setState(() => _eulaAccepted = v ?? false),
            ),
            const SizedBox(width: 8),
            const Text('I agree to the Minecraft EULA'),
          ],
        ),
      ],
    );
  }

  Widget _buildStepReview(ThemeData theme) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Review & Create',
            style: theme.textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.bold)),
        const SizedBox(height: 24),
        _reviewRow('Name', _nameCtrl.text.trim(), theme),
        _reviewRow('Source', _source?.name.replaceAll('_', ' ') ?? '', theme),
        _reviewRow('Version', _mcVersion.isNotEmpty ? _mcVersion : 'Custom', theme),
        _reviewRow('Min RAM', '${_minRam.toStringAsFixed(1)} GB', theme),
        _reviewRow('Max RAM', '${_maxRam.toStringAsFixed(1)} GB', theme),
        _reviewRow('Port', _portCtrl.text, theme),
        _reviewRow('Gamemode', _gamemode, theme),
        _reviewRow('Difficulty', _difficulty, theme),
        _reviewRow('MOTD', _motdCtrl.text, theme),
      ],
    );
  }

  Widget _reviewRow(String label, String value, ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(label,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(fontWeight: FontWeight.bold)),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }

  Widget _buildNavigation(ThemeData theme) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        border: Border(
            top: BorderSide(color: theme.colorScheme.outlineVariant))),
      child: Row(
        children: [
          if (_currentStep > 0)
            OutlinedButton(
              onPressed: () => setState(() => _currentStep--),
              child: const Text('Back'),
            ),
          const Spacer(),
          if (_currentStep < _totalSteps - 1)
            FilledButton(
              onPressed: _canGoNext() ? _goNext : null,
              child: const Text('Next'),
            )
          else
            FilledButton(
              onPressed: _eulaAccepted && !_creating ? _create : null,
              child: _creating
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child:
                          CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Create Server'),
            ),
        ],
      ),
    );
  }

  bool _canGoNext() {
    switch (_currentStep) {
      case 0:
        return _step0Complete && !_downloading;
      case 1:
        return _nameCtrl.text.trim().isNotEmpty;
      default:
        return true;
    }
  }

  void _goNext() {
    if (_currentStep == 0 && _source != null && _source != ChooseSource.pickFile && _jarTargetPath == null && _mcVersion.isNotEmpty) {
      _startDownload();
    } else {
      setState(() => _currentStep++);
    }
  }
}

ServerType? _sourceToType(ChooseSource src) {
  switch (src) {
    case ChooseSource.downloadPaper:
      return ServerType.paper;
    case ChooseSource.downloadVanilla:
      return ServerType.vanilla;
    case ChooseSource.downloadFabric:
      return ServerType.fabric;
    case ChooseSource.downloadForge:
      return ServerType.forge;
    case ChooseSource.downloadNeoForge:
      return ServerType.neoforge;
    case ChooseSource.downloadPurpur:
      return ServerType.purpur;
    case ChooseSource.downloadFolia:
      return ServerType.folia;
    case ChooseSource.pickFile:
      return null;
  }
}

class HoverVersionContainer extends StatefulWidget {
  final String version;
  final bool isSelected;
  final VoidCallback onTap;

  const HoverVersionContainer({
    super.key,
    required this.version,
    required this.isSelected,
    required this.onTap,
  });

  @override
  State<HoverVersionContainer> createState() => _HoverVersionContainerState();
}

class _HoverVersionContainerState extends State<HoverVersionContainer> {
  bool _isHovered = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    final baseColor = widget.isSelected
        ? theme.colorScheme.primaryContainer
        : (_isHovered
            ? theme.colorScheme.surfaceContainerHigh
            : theme.colorScheme.surfaceContainerLow);

    final borderColor = widget.isSelected
        ? theme.colorScheme.primary
        : (_isHovered
            ? theme.colorScheme.outline
            : theme.colorScheme.outlineVariant);

    final textColor = widget.isSelected
        ? theme.colorScheme.onPrimaryContainer
        : theme.colorScheme.onSurface;

    return MouseRegion(
      onEnter: (_) => setState(() => _isHovered = true),
      onExit: (_) => setState(() => _isHovered = false),
      child: GestureDetector(
        onTap: widget.onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: baseColor,
            border: Border.all(color: borderColor, width: widget.isSelected ? 2.0 : 1.0),
            borderRadius: BorderRadius.circular(12),
            boxShadow: _isHovered && !widget.isSelected
                ? [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.05),
                      blurRadius: 4,
                      offset: const Offset(0, 2),
                    )
                  ]
                : null,
          ),
          padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
          child: Text(
            widget.version,
            style: TextStyle(
              fontSize: 12,
              fontWeight: widget.isSelected ? FontWeight.bold : FontWeight.w500,
              color: textColor,
            ),
            textAlign: TextAlign.center,
          ),
        ),
      ),
    );
  }
}