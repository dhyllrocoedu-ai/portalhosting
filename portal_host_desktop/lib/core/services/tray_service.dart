import 'package:flutter/foundation.dart';
import 'package:tray_manager/tray_manager.dart';
import 'package:window_manager/window_manager.dart';

class TrayService extends ChangeNotifier {
  bool _isInitialized = false;
  bool _minimizeToTray = true;
  bool _closeToTray = true;

  bool get isInitialized => _isInitialized;
  bool get minimizeToTray => _minimizeToTray;
  bool get closeToTray => _closeToTray;

  set minimizeToTray(bool value) {
    _minimizeToTray = value;
    notifyListeners();
  }

  set closeToTray(bool value) {
    _closeToTray = value;
    notifyListeners();
  }

  Future<void> initialize() async {
    if (_isInitialized) return;

    try {
      // Register window listener first
      windowManager.addListener(_WindowListener(this));

      // Initialize tray manager - set icon
      await TrayManager.instance.setIcon('assets/icons/tray_icon.png');

      // Set up tray menu
      final menu = Menu(
        items: [
          MenuItem(
            key: 'show',
            label: 'Show Portal Host',
          ),
          MenuItem.separator(),
          MenuItem(
            key: 'settings',
            label: 'Settings',
          ),
          MenuItem.separator(),
          MenuItem(
            key: 'quit',
            label: 'Quit',
          ),
        ],
      );

      await TrayManager.instance.setContextMenu(menu);
      TrayManager.instance.addListener(_TrayListener(this));

      _isInitialized = true;
      notifyListeners();
    } catch (e) {
      if (kDebugMode) {
        print('Failed to initialize tray service: $e');
      }
    }
  }

  Future<void> showWindow() async {
    await windowManager.show();
    await windowManager.focus();
  }

  Future<void> hideWindow() async {
    await windowManager.hide();
  }

  Future<void> toggleWindow() async {
    final isVisible = await windowManager.isVisible();
    if (isVisible) {
      await hideWindow();
    } else {
      await showWindow();
    }
  }

  Future<void> quitApp() async {
    await TrayManager.instance.destroy();
    await windowManager.destroy();
  }
}

class _TrayListener with TrayListener {
  final TrayService _service;

  _TrayListener(this._service);

  @override
  void onTrayIconMouseDown() {}

  @override
  void onTrayIconRightMouseDown() {}

  @override
  void onTrayIconMouseUp() {}

  @override
  void onTrayIconRightMouseUp() {}

  @override
  void onTrayMenuItemClick(MenuItem menuItem) async {
    switch (menuItem.key) {
      case 'show':
        await _service.showWindow();
        break;
      case 'settings':
        await _service.showWindow();
        break;
      case 'quit':
        await _service.quitApp();
        break;
    }
  }
}

class _WindowListener with WindowListener {
  final TrayService _service;

  _WindowListener(this._service);

  @override
  void onWindowClose() async {
    if (_service.closeToTray) {
      await _service.hideWindow();
    } else {
      await _service.quitApp();
    }
  }

  @override
  void onWindowMinimize() async {
    if (_service.minimizeToTray) {
      await _service.hideWindow();
    }
  }

  @override
  void onWindowRestore() {}

  @override
  void onWindowFocus() {}

  @override
  void onWindowBlur() {}

  @override
  void onWindowResize() {}
}