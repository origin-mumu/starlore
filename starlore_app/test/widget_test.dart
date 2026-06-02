import 'package:flutter_test/flutter_test.dart';

import 'package:starlore_app/app.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const StareloreApp());

    // Verify that the app renders without errors.
    expect(find.text('早安，星语者 ✨'), findsOneWidget);
  });
}
