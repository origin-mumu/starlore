import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../theme/app_theme.dart';
import '../widgets/floating_nav_bar.dart';
import '../widgets/orb_background.dart';
import 'home/home_page.dart';
import 'explore/explore_page.dart';
import 'publish/publish_page.dart';
import 'profile/profile_page.dart';

/// 主壳 — 底部导航 + 页面切换
class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _currentIndex = 0;

  final List<Widget> _pages = const [
    HomePage(),
    ExplorePage(),
    PublishPage(),
    ProfilePage(),
  ];

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    final bool isKeyboardOpen = MediaQuery.of(context).viewInsets.bottom > 0;

    // 根据主题动态设置状态栏样式
    SystemChrome.setSystemUIOverlayStyle(SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness:
          p.brightness == Brightness.dark ? Brightness.light : Brightness.dark,
      systemNavigationBarColor: Colors.transparent,
    ));

    return Scaffold(
      backgroundColor: p.canvas,
      body: Stack(
        children: [
          const OrbBackground(),
          SafeArea(
            bottom: false,
            child: IndexedStack(
              index: _currentIndex,
              children: _pages,
            ),
          ),
          if (!isKeyboardOpen)
            FloatingNavBar(
              currentIndex: _currentIndex,
              onTap: (i) => setState(() => _currentIndex = i),
            ),
        ],
      ),
    );
  }
}
