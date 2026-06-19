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

    // 阴影装饰 (独立于裁剪层，防止阴影边缘被 ClipRRect 截断产生硬线)
    final shadowDecoration = BoxDecoration(
      borderRadius: BorderRadius.circular(radius),
      boxShadow: [
        BoxShadow(
          color: glow
              ? p.accent.withValues(alpha: 0.08)
              : p.shadowCard,
          blurRadius: glow ? 24 : 20, // 稍微增加模糊半径使阴影更柔和
          offset: const Offset(0, 6),
        ),
      ],
    );

    // 卡片主体装饰 (背景色与边框)
    final bodyDecoration = BoxDecoration(
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
    );

    Widget cardBody = Container(
      decoration: bodyDecoration,
      padding: padding ?? const EdgeInsets.all(Tok.space4),
      child: child,
    );

    if (blur) {
      cardBody = ClipRRect(
        borderRadius: BorderRadius.circular(radius),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: Tok.blurMd, sigmaY: Tok.blurMd),
          child: cardBody,
        ),
      );
    }

    // 将阴影层和磨砂主体合并为 Stack，将 margin 应用于最外层
    Widget finalCard = Container(
      margin: margin,
      child: Stack(
        clipBehavior: Clip.none, // 允许阴影溢出 Stack 边界绘制
        children: [
          // 底层：阴影 (不进行 ClipRRect 裁剪)
          Positioned.fill(
            child: Container(
              decoration: shadowDecoration,
            ),
          ),
          // 顶层：磨砂卡片主体
          cardBody,
        ],
      ),
    );

    if (onTap != null) {
      return GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: finalCard,
      );
    }

    return finalCard;
  }
}
