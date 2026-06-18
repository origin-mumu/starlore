import 'dart:ui';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';

/// 磨砂玻璃卡片 — 核心 UI 组件
class GlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final double radius;
  final bool glow;
  final bool blur;
  final VoidCallback? onTap;

  const GlassCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
    this.radius = Tok.radiusLg,
    this.glow = false,
    this.blur = true,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    final isDark = p.brightness == Brightness.dark;

    Widget card = Container(
      margin: margin,
      decoration: BoxDecoration(
        color: isDark
            ? p.surface.withValues(alpha: 0.6)
            : p.surface.withValues(alpha: 0.85),
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(
          color: glow
              ? p.accent.withValues(alpha: 0.2)
              : p.border.withValues(alpha: 0.7),
          width: 0.5,
        ),
        boxShadow: [
          BoxShadow(
            color: glow
                ? p.accent.withValues(alpha: 0.08)
                : p.shadowCard,
            blurRadius: glow ? 24 : 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Padding(
        padding: padding ?? const EdgeInsets.all(Tok.space4),
        child: child,
      ),
    );

    if (blur) {
      card = ClipRRect(
        borderRadius: BorderRadius.circular(radius),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: Tok.blurMd, sigmaY: Tok.blurMd),
          child: card,
        ),
      );
    }

    if (onTap != null) {
      return GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: card,
      );
    }

    return card;
  }
}
