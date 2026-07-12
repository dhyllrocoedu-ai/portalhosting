import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../server/process_manager.dart';

part 'process_provider.g.dart';

@riverpod
ProcessManager processManager(ProcessManagerRef ref) {
  final pm = ProcessManager();
  ref.onDispose(() => pm.dispose());
  return pm;
}
