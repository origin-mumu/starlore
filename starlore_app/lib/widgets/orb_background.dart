import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// 光球渐变背景 — 页面底层装饰
class OrbBackground extends StatelessWidget {
  const OrbBackground({super.key});

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    final isDark = p.brightness == Brightness.dark;

    return Positioned.fill(
      child: Container(
        decoration: BoxDecoration(
          color: p.canvas,
        ),
        child: Stack(
          children: [
            // 主光球 — 右上方
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
                      p.accent.withValues(alpha: isDark ? 0.06 : 0.10),
                      p.accent.withValues(alpha: 0),
                    ],
                  ),
                ),
              ),
            ),
            // 暖色光球 — 左下方
            Positioned(
              left: -100,
              bottom: 100,
              child: Container(
                width: 280,
                height: 280,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      p.warm.withValues(alpha: isDark ? 0.05 : 0.08),
                      p.warm.withValues(alpha: 0),
                    ],
                  ),
                ),
              ),
            ),
            // 微弱中心光球
            Positioned(
              left: 100,
              top: 300,
              child: Container(
                width: 200,
                height: 200,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      p.accentSoft.withValues(alpha: isDark ? 0.04 : 0.06),
                      p.accentSoft.withValues(alpha: 0),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
