import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';
import '../theme/typography.dart';
import 'fade_in_widget.dart';

/// 统一空状态组件 — 图标 + 标题 + 副标题 + (可选)操作按钮。
///
/// 用于文章列表、收藏、分类等为空时的占位，替代各页面临时拼凑的 Icon+Text。
class EmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final String? actionLabel;
  final VoidCallback? onAction;

  const EmptyState({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
    this.actionLabel,
    this.onAction,
  });

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return FadeInUp(
      child: Padding(
        padding: const EdgeInsets.symmetric(
            horizontal: Tok.horizontalPadding, vertical: Tok.space7),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 72,
              height: 72,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: p.accentSoft,
              ),
              child: Icon(icon, size: 32, color: p.accent),
            ),
            const SizedBox(height: Tok.space5),
            Text(title, style: Typo.h3(p.ink), textAlign: TextAlign.center),
            if (subtitle != null) ...[
              const SizedBox(height: Tok.space2),
              Text(
                subtitle!,
                style: Typo.bodySmall(p.inkMuted),
                textAlign: TextAlign.center,
              ),
            ],
            if (actionLabel != null && onAction != null) ...[
              const SizedBox(height: Tok.space6),
              GestureDetector(
                onTap: onAction,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: Tok.space5, vertical: Tok.space3),
                  decoration: BoxDecoration(
                    color: p.accent,
                    borderRadius: BorderRadius.circular(Tok.radiusFull),
                    boxShadow: [
                      BoxShadow(
                        color: p.accent.withValues(alpha: 0.2),
                        blurRadius: 16,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Text(
                    actionLabel!,
                    style: Typo.label(
                        p.brightness == Brightness.dark ? p.canvas : Colors.white),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
