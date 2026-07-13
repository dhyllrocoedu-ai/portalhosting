import 'package:flutter_test/flutter_test.dart';
import 'package:portal_host_desktop/app.dart';

void main() {
  testWidgets('App loads without error', (WidgetTester tester) async {
    await tester.pumpWidget(const App());
    expect(find.text('Portal Host'), findsWidgets);
  });
}
