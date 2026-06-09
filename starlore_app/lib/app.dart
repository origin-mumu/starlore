import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'theme/app_theme.dart';
import 'pages/home_page.dart';
import 'pages/explore_page.dart';
import 'pages/chat_page.dart';
import 'pages/profile_page.dart';
import 'pages/login_page.dart';
import 'package:shared_preferences/shared_preferences.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _idx = 0;
  bool _loggedIn = false;

  final _pages = const [HomePage(), ExplorePage(), ChatPage(), ProfilePage()];

  @override
  void initState() {
    super.initState();
    _checkLogin();
  }

  Future<void> _checkLogin() async {
    final prefs = await SharedPreferences.getInstance();
    if (mounted) setState(() => _loggedIn = prefs.getString('auth_token') != null);
  }

  Future<void> _tap(int i) async {
    if ((i == 2 || i == 3) && !_loggedIn) {
      final r = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => const LoginPage()));
      if (r == true) { setState(() => _loggedIn = true); } else { return; }
    }
    setState(() => _idx = i);
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light.copyWith(statusBarColor: Colors.transparent, systemNavigationBarColor: p.surface0),
      child: Scaffold(
        backgroundColor: p.surface0,
        body: Stack(
          children: [
            const SpaceBackground(),
            SafeArea(child: IndexedStack(index: _idx, children: _pages)),
            _nav(p),
          ],
        ),
      ),
    );
  }

  Widget _nav(StarlorePalette p) {
    final bottom = MediaQuery.of(context).padding.bottom;
    return Positioned(
      left: 0, right: 0, bottom: 0,
      child: Container(
        padding: EdgeInsets.fromLTRB(0, 0, 0, bottom + 20),
        child: Center(
          child: Container(
            margin: const EdgeInsets.symmetric(horizontal: 48),
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
            decoration: BoxDecoration(
              color: p.surface1.withValues(alpha: 0.7),
              borderRadius: BorderRadius.circular(T.rFull),
              border: Border.all(color: p.line.withValues(alpha: 0.6), width: 0.5),
              boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.3), blurRadius: 24, offset: const Offset(0, 8))],
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                _navItem(Icons.home_outlined, Icons.home_rounded, 0, p),
                _navItem(Icons.explore_outlined, Icons.explore_rounded, 1, p),
                _navItem(Icons.chat_bubble_outline_rounded, Icons.chat_bubble_rounded, 2, p),
                _navItem(Icons.person_outline_rounded, Icons.person_rounded, 3, p),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _navItem(IconData outline, IconData filled, int i, StarlorePalette p) {
    final active = _idx == i;
    return GestureDetector(
      onTap: () => _tap(i),
      behavior: HitTestBehavior.opaque,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        width: 44, height: 36,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(T.r8),
          color: active ? p.accent.withValues(alpha: 0.1) : Colors.transparent,
        ),
        child: Icon(active ? filled : outline, size: 20, color: active ? p.accent : p.text2),
      ),
    );
  }
}
