import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';
import '../widgets/floating_nav_bar.dart';
import '../widgets/orb_background.dart';
import '../widgets/fade_in_widget.dart';
import 'home/home_page.dart';
import 'explore/explore_page.dart';
import 'publish/publish_page.dart';
import 'profile/profile_page.dart';
import 'chat/chat_page.dart';

/// 主壳 — 底部导航 + 页面切换 + AI 悬浮入口
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
    final isDark = p.brightness == Brightness.dark;

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
          // AI 悬浮入口（发布页已有写作入口，该 tab 下隐藏 FAB 避免重叠）
          if (!isKeyboardOpen && _currentIndex != 2)
            Positioned(
              right: Tok.horizontalPadding,
              bottom: MediaQuery.of(context).padding.bottom +
                  Tok.navBarHeight +
                  Tok.navBarBottomPadding +
                  Tok.space3,
              child: FadeInUp(
                delay: const Duration(milliseconds: 400),
                offsetY: 24,
                child: _AiFab(onTap: () => _openChat()),
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

  void _openChat() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const ChatPage()),
    );
  }
}

/// 星语助手悬浮按钮 — 毛玻璃 + accent→warm 渐变，契合整体视觉语言
class _AiFab extends StatefulWidget {
  final VoidCallback onTap;
  const _AiFab({required this.onTap});

  @override
  State<_AiFab> createState() => _AiFabState();
}

class _AiFabState extends State<_AiFab> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    final isDark = p.brightness == Brightness.dark;

    return GestureDetector(
      onTap: widget.onTap,
      onTapDown: (_) => setState(() => _pressed = true),
      onTapUp: (_) => setState(() => _pressed = false),
      onTapCancel: () => setState(() => _pressed = false),
      child: AnimatedScale(
        scale: _pressed ? 0.92 : 1.0,
        duration: Tok.fast,
        curve: Curves.easeOut,
        child: ClipOval(
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: Tok.blurMd, sigmaY: Tok.blurMd),
            child: Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [p.accent, p.warm],
                ),
                border: Border.all(
                  color: isDark
                      ? Colors.white.withValues(alpha: 0.25)
                      : Colors.white.withValues(alpha: 0.55),
                  width: 1.2,
                ),
                boxShadow: [
                  BoxShadow(
                    color: p.accent.withValues(alpha: isDark ? 0.45 : 0.3),
                    blurRadius: 20,
                    offset: const Offset(0, 6),
                  ),
                ],
              ),
              child: const Icon(
                Icons.auto_awesome_rounded,
                size: 26,
                color: Colors.white,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
