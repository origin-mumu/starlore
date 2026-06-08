import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'theme/app_theme.dart';
import 'pages/splash_page.dart';

final themeNotifier = ThemeNotifier();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await themeNotifier.load();
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.light,
    systemNavigationBarColor: Colors.transparent,
    systemNavigationBarIconBrightness: Brightness.light,
  ));
  runApp(const StarloreApp());
}

class StarloreApp extends StatelessWidget {
  const StarloreApp({super.key});

  @override
  Widget build(BuildContext context) {
    return StarloreTheme(
      notifier: themeNotifier,
      child: Builder(
        builder: (context) {
          final p = paletteOf(context);
          return MaterialApp(
            title: 'STARLORE',
            debugShowCheckedModeBanner: false,
            theme: buildAppTheme(p),
            home: const SplashPage(),
          );
        },
      ),
    );
  }
}
