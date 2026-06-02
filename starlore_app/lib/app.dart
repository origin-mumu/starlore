import 'package:flutter/material.dart';
import 'theme/app_theme.dart';
import 'pages/home_page.dart';
import 'pages/explore_page.dart';
import 'pages/chat_page.dart';
import 'pages/profile_page.dart';
import 'widgets/bottom_nav.dart';

/// Starelore App 主组件
class StareloreApp extends StatelessWidget {
  const StareloreApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Starelore',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.theme,
      home: const MainScreen(),
    );
  }
}

/// 主屏幕（带底部导航）
class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _currentIndex = 0;

  final List<Widget> _pages = const [
    HomePage(),
    ExplorePage(),
    ChatPage(),
    ProfilePage(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.bgBase,
      body: SafeArea(
        child: Stack(
          children: [
            // 页面内容
            Positioned.fill(
              child: Padding(
                padding: const EdgeInsets.only(bottom: 72),
                child: IndexedStack(
                  index: _currentIndex,
                  children: _pages,
                ),
              ),
            ),

            // 底部导航
            Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              child: BottomNav(
                currentIndex: _currentIndex,
                onTap: (index) {
                  setState(() {
                    _currentIndex = index;
                  });
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
