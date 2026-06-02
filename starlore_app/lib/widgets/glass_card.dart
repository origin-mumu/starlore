import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// 简洁白色卡片组件
class GlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final double borderRadius;
  final Color? backgroundColor;
  final VoidCallback? onTap;

  const GlassCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
    this.borderRadius = AppTheme.radiusMD,
    this.backgroundColor,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: margin ?? const EdgeInsets.only(bottom: 20),
        padding: padding ?? const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: backgroundColor ?? AppTheme.bgCard,
          borderRadius: BorderRadius.circular(borderRadius),
          boxShadow: AppTheme.shadowSoft,
        ),
        child: child,
      ),
    );
  }
}
