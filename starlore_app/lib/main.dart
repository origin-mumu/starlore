import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'theme/app_theme.dart';
import 'pages/splash_page.dart';

/// 全局主题状态 — Profile 页面的 ThemePicker 需要访问
final themeNotifier = ThemeNotifier();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 加载持久化的主题
  await themeNotifier.load();

  // 透明状态栏
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    systemNavigationBarColor: Colors.transparent,
  ));

  // Edge-to-edge
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

  runApp(const StarloreApp());
}

class StarloreApp extends StatelessWidget {
  const StarloreApp({super.key});

  @override
  Widget build(BuildContext context) {
    return StarloreThemeProvider(
      notifier: themeNotifier,
      child: Builder(
        builder: (context) {
          final palette = paletteOf(context);
          return MaterialApp(
            title: 'Starlore',
            debugShowCheckedModeBanner: false,
            theme: buildThemeData(palette),
            home: const SplashPage(),
          );
        },
      ),
    );
  }
}
