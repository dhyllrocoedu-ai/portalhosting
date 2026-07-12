import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../server/console_streamer.dart';

part 'console_provider.g.dart';

@riverpod
ConsoleStreamer consoleStreamer(ConsoleStreamerRef ref) {
  final cs = ConsoleStreamer();
  ref.onDispose(() => cs.dispose());
  return cs;
}
