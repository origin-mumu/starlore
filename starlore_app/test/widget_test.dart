import 'package:flutter_test/flutter_test.dart';
import 'package:starlore_app/main.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const StarloreApp());
    expect(find.text('星语者'), findsOneWidget);
  });
}
