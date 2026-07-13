import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_picker/file_picker.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:path/path.dart' as p;
import 'package:shared_preferences/shared_preferences.dart';
import '../../core/providers/settings_provider.dart';
import '../../core/java/jdk_manager.dart';
import '../../core/tunnel/tunnel_provider.dart';
import 'jdk_management_screen.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  final _jdkManager = JdkManager();
  String? _javaHome;
  String? _javaVersion;
  bool _checkingJdk = true;
  bool _installingJdk = false;
  double _installProgress = 0.0;
  String _installStatus = '';
  int _backupInterval = 30;
  int _maxBackups = 10;
  TunnelProvider _tunnelProvider = TunnelProvider.playit;

  @override
  void initState() {
    super.initState();
    _checkJdkStatus();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _backupInterval = prefs.getInt('backup_interval') ?? 30;
      _maxBackups = prefs.getInt('max_backups') ?? 10;
      _tunnelProvider = TunnelProvider.values[prefs.getInt('tunnel_provider') ?? 0];
    });
  }

  Future<void> _checkJdkStatus() async {
    setState(() => _checkingJdk = true);
    try {
      final jdkPath = await _jdkManager.getJavaHome();
      if (jdkPath != null && jdkPath.isNotEmpty) {
        final version = await _getJavaVersion(jdkPath);
        setState(() {
          _javaHome = jdkPath;
          _javaVersion = version;
        });
      } else {
        setState(() {
          _javaHome = null;
          _javaVersion = null;
        });
      }
    } catch (_) {
      setState(() {
        _javaHome = null;
        _javaVersion = null;
      });
    }
    setState(() => _checkingJdk = false);
  }

  Future<String?> _getJavaVersion(String javaHome) async {
    try {
      final javaBin = p.join(javaHome, 'bin', Platform.isWindows ? 'java.exe' : 'java');
      if (!File(javaBin).existsSync()) return null;
      final result = await Process.run(javaBin, ['-version']);
      final output = result.stderr.toString();
      final match = RegExp(r'version "([^"]+)"').firstMatch(output);
      return match?.group(1);
    } catch (e) {
      return null;
    }
  }

  Future<void> _pickServersDir() async {
    final path = await FilePicker.platform.getDirectoryPath(
      dialogTitle: 'Select Servers Directory',
    );
    if (path != null) {
      ref.read(settingsProvider.notifier).setServersDir(path);
    }
  }

  Future<void> _pickJavaHome() async {
    String? path;
    if (Platform.isWindows) {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['exe'],
        dialogTitle: 'Select java.exe',
      );
      if (result != null && result.files.isNotEmpty) {
        path = result.files.first.path;
      }
    } else {
      final result = await FilePicker.platform.getDirectoryPath(
        dialogTitle: 'Select Java Home Directory',
      );
      if (result != null) {
        final javaBin = p.join(result, 'bin', Platform.isWindows ? 'java.exe' : 'java');
        if (await File(javaBin).exists()) {
          path = result;
        } else {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Selected directory does not contain a valid JDK')),
            );
          }
          return;
        }
      }
    }

    if (path != null) {
      final version = await _getJavaVersion(path);
      setState(() {
        _javaHome = path;
        _javaVersion = version;
      });
      ref.read(settingsProvider.notifier).setJavaPath(path);
    }
  }

  Future<void> _openJdkManagement() async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const JdkManagementScreen()),
    );
    _checkJdkStatus();
  }

  Future<void> _installBundledJdk() async {
    setState(() {
      _installingJdk = true;
      _installProgress = 0.0;
      _installStatus = 'Starting download...';
    });

    final result = await _jdkManager.downloadAndInstallJdk(
      onProgress: (progress, status) {
        if (mounted) {
          setState(() {
            _installProgress = progress;
            _installStatus = status;
          });
        }
      },
    );

    if (mounted) {
      setState(() => _installingJdk = false);
      if (result.success) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result.message)),
        );
        _checkJdkStatus();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(result.message),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  Future<void> _setThemeMode(ThemeMode mode) async {
    await ref.read(settingsProvider.notifier).setThemeMode(mode);
  }

  @override
  Widget build(BuildContext context) {
    final settings = ref.watch(settingsProvider);
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildSectionHeader(theme, 'Java / JDK'),
          if (_checkingJdk)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Center(child: CircularProgressIndicator()),
            )
          else ...[
            _buildInfoTile(
              theme,
              icon: Icons.code,
              title: 'Java Version',
              subtitle: _javaVersion ?? 'Not detected',
              trailing: _javaHome != null
                  ? IconButton(
                      icon: const Icon(Icons.info_outline),
                      onPressed: _openJdkManagement,
                    )
                  : null,
            ),
            if (_javaHome != null)
              _buildInfoTile(
                theme,
                icon: Icons.folder,
                title: 'Java Path',
                subtitle: _javaHome!,
                trailing: IconButton(
                  icon: const Icon(Icons.edit),
                  onPressed: _pickJavaHome,
                ),
              ),
            const SizedBox(height: 8),
            if (_installingJdk)
              _buildProgressIndicator()
            else if (_javaHome == null) ...[
              _buildActionButton(
                theme,
                icon: Icons.download,
                label: 'Download & Install JDK 21',
                onPressed: _installBundledJdk,
              ),
              const SizedBox(height: 4),
            ],
            _buildActionButton(
              theme,
              icon: Icons.folder_open,
              label: 'Browse for Java',
              onPressed: _pickJavaHome,
            ),
          ],
          const Divider(height: 32),

          _buildSectionHeader(theme, 'Servers'),
          _buildInfoTile(
            theme,
            icon: Icons.folder,
            title: 'Servers Directory',
            subtitle: settings.serversDir.isNotEmpty
                ? settings.serversDir
                : 'Not set (uses app default)',
            trailing: IconButton(
              icon: const Icon(Icons.folder_open),
              onPressed: _pickServersDir,
            ),
          ),
          const Divider(height: 32),

          _buildSectionHeader(theme, 'Backup Settings'),
          _buildSliderTile(
            theme,
            title: 'Max Backups',
            subtitle: 'Keep up to $_maxBackups backups',
            value: _maxBackups.toDouble(),
            min: 1,
            max: 50,
            divisions: 49,
            onChanged: (v) {
              setState(() => _maxBackups = v.round());
              ref.read(settingsProvider.notifier).setMaxBackups(v.round());
            },
          ),
          _buildSliderTile(
            theme,
            title: 'Backup Interval',
            subtitle: 'Every $_backupInterval minutes',
            value: _backupInterval.toDouble(),
            min: 5,
            max: 120,
            divisions: 23,
            onChanged: (v) {
              setState(() => _backupInterval = v.round());
              ref.read(settingsProvider.notifier).setBackupInterval(v.round());
            },
          ),
          const Divider(height: 32),

          _buildSectionHeader(theme, 'Tunnel Provider'),
          ...TunnelProvider.values.map((provider) {
            return RadioListTile<TunnelProvider>(
              title: Text(provider.displayName),
              subtitle: Text(provider.description),
              value: provider,
              groupValue: _tunnelProvider,
              onChanged: (v) async {
                if (v == null) return;
                setState(() => _tunnelProvider = v);
                final prefs = await SharedPreferences.getInstance();
                await prefs.setInt('tunnel_provider', v.index);
              },
            );
          }),
          const Divider(height: 32),

          _buildSectionHeader(theme, 'Appearance'),
          ListTile(
            leading: const Icon(Icons.palette),
            title: const Text('Theme'),
            trailing: SegmentedButton<ThemeMode>(
              segments: const [
                ButtonSegment(value: ThemeMode.system, label: Text('System')),
                ButtonSegment(value: ThemeMode.light, label: Text('Light')),
                ButtonSegment(value: ThemeMode.dark, label: Text('Dark')),
              ],
              selected: {settings.themeMode},
              onSelectionChanged: (v) => _setThemeMode(v.first),
            ),
          ),
          const Divider(height: 32),

          _buildSectionHeader(theme, 'About'),
          ListTile(
            leading: const Icon(Icons.info),
            title: const Text('Portal Host'),
            subtitle: const Text('v4.0.0 Desktop Edition'),
          ),
          ListTile(
            leading: const Icon(Icons.code),
            title: const Text('Source Code'),
            subtitle: const Text('github.com/anomalyco/PortalHost'),
            trailing: const Icon(Icons.open_in_new),
            onTap: () => launchUrl(Uri.parse('https://github.com/anomalyco/PortalHost')),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(ThemeData theme, String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Text(
        title,
        style: theme.textTheme.titleMedium?.copyWith(
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildInfoTile(
    ThemeData theme, {
    required IconData icon,
    required String title,
    required String subtitle,
    Widget? trailing,
  }) {
    return ListTile(
      leading: Icon(icon),
      title: Text(title),
      subtitle: Text(
        subtitle,
        style: theme.textTheme.bodySmall,
        overflow: TextOverflow.ellipsis,
      ),
      trailing: trailing,
    );
  }

  Widget _buildActionButton(
    ThemeData theme, {
    required IconData icon,
    required String label,
    required VoidCallback onPressed,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: OutlinedButton.icon(
        icon: Icon(icon),
        label: Text(label),
        onPressed: onPressed,
      ),
    );
  }

  Widget _buildSliderTile(
    ThemeData theme, {
    required String title,
    required String subtitle,
    required double value,
    required double min,
    required double max,
    required int divisions,
    required ValueChanged<double> onChanged,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ListTile(
          title: Text(title),
          subtitle: Text(subtitle),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Slider(
            value: value,
            min: min,
            max: max,
            divisions: divisions,
            label: value.round().toString(),
            onChanged: onChanged,
          ),
        ),
      ],
    );
  }

  Widget _buildProgressIndicator() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            LinearProgressIndicator(value: _installProgress),
            const SizedBox(height: 8),
            Text(
              _installStatus,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ),
    );
  }
}
