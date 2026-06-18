import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';
import '../theme/typography.dart';
import '../services/auth_service.dart';
import 'main_shell.dart';

/// 启动屏 — 品牌展示 + 预加载
class SplashPage extends StatefulWidget {
  const SplashPage({super.key});

  @override
  State<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends State<SplashPage>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _opacity;
  late Animation<Offset> _offset;

  @override
  void initState() {
    super.initState();

    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.dark,
    ));

    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    );
    _opacity = CurvedAnimation(parent: _ctrl, curve: Tok.easeOutQuart);
    _offset = Tween<Offset>(
      begin: const Offset(0, 20),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _ctrl, curve: Tok.easeOutQuart));

    _ctrl.forward();
    _init();
  }

  Future<void> _init() async {
    await Future.wait([
      AuthService.init(),
      Future.delayed(const Duration(milliseconds: 1500)),
    ]);

    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      PageRouteBuilder(
        pageBuilder: (context, animation, secondaryAnimation) => const MainShell(),
        transitionsBuilder: (context, anim, secondaryAnimation, child) {
          return FadeTransition(opacity: anim, child: child);
        },
        transitionDuration: const Duration(milliseconds: 600),
      ),
    );
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return Scaffold(
      backgroundColor: p.canvas,
      body: Stack(
        children: [
          Positioned(
            right: -80,
            top: -60,
            child: Container(
              width: 300,
              height: 300,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    p.accent.withValues(alpha: 0.12),
                    p.accent.withValues(alpha: 0),
                  ],
                ),
              ),
            ),
          ),
          Positioned(
            left: -100,
            bottom: 80,
            child: Container(
              width: 280,
              height: 280,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    p.warm.withValues(alpha: 0.1),
                    p.warm.withValues(alpha: 0),
                  ],
                ),
              ),
            ),
          ),
          Center(
            child: AnimatedBuilder(
              animation: _ctrl,
              builder: (_, child) => Opacity(
                opacity: _opacity.value,
                child: Transform.translate(
                  offset: _offset.value,
                  child: child,
                ),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 72,
                    height: 72,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [
                          p.accent.withValues(alpha: 0.15),
                          p.warm.withValues(alpha: 0.1),
                        ],
                      ),
                      border: Border.all(
                        color: p.accent.withValues(alpha: 0.2),
                        width: 1,
                      ),
                    ),
                    child: Icon(
                      Icons.auto_awesome_rounded,
                      size: 32,
                      color: p.accent,
                    ),
                  ),
                  const SizedBox(height: Tok.space5),
                  Text('STARLORE', style: Typo.display(p.ink)),
                  const SizedBox(height: Tok.space2),
                  Text('星语治愈', style: Typo.bodySmall(p.inkMuted)),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
