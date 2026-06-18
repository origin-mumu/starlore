import 'dart:ui';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';

/// 浮动底部导航栏 — 胶囊形毛玻璃
class FloatingNavBar extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;

  const FloatingNavBar({
    super.key,
    required this.currentIndex,
    required this.onTap,
  });

  static const _items = [
    _NavItem(Icons.home_outlined, Icons.home_rounded, '首页'),
    _NavItem(Icons.explore_outlined, Icons.explore_rounded, '探索'),
    _NavItem(Icons.chat_bubble_outline_rounded, Icons.chat_bubble_rounded, 'AI'),
    _NavItem(Icons.person_outline_rounded, Icons.person_rounded, '我的'),
  ];

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    final bottom = MediaQuery.of(context).padding.bottom;
    final isDark = p.brightness == Brightness.dark;

    return Positioned(
      left: 0,
      right: 0,
      bottom: 0,
      child: Padding(
        padding: EdgeInsets.fromLTRB(
            Tok.space7, 0, Tok.space7, bottom + Tok.navBarBottomPadding),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(Tok.navBarRadius),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: Tok.blurLg, sigmaY: Tok.blurLg),
            child: Container(
              height: Tok.navBarHeight,
              padding: const EdgeInsets.symmetric(horizontal: Tok.space2),
              decoration: BoxDecoration(
                color: isDark
                    ? p.surface.withValues(alpha: 0.7)
                    : p.surface.withValues(alpha: 0.85),
                borderRadius: BorderRadius.circular(Tok.navBarRadius),
                border: Border.all(
                  color: p.border.withValues(alpha: isDark ? 0.4 : 0.6),
                  width: 0.5,
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: isDark ? 0.4 : 0.12),
                    blurRadius: 24,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: List.generate(_items.length, (i) {
                  final item = _items[i];
                  final active = currentIndex == i;
                  return _buildItem(context, item, active, () => onTap(i), p);
                }),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildItem(BuildContext context, _NavItem item, bool active,
      VoidCallback onTap, p) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: AnimatedContainer(
        duration: Tok.fast,
        curve: Tok.easeOutQuart,
        width: 56,
        height: 40,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(Tok.radiusMd),
          color: active
              ? p.accent.withValues(alpha: 0.1)
              : Colors.transparent,
        ),
        child: AnimatedSwitcher(
          duration: Tok.fast,
          child: Icon(
            active ? item.filled : item.outline,
            key: ValueKey(active),
            size: 22,
            color: active ? p.accent : p.inkMuted,
          ),
        ),
      ),
    );
  }
}

class _NavItem {
  final IconData outline;
  final IconData filled;
  final String label;
  const _NavItem(this.outline, this.filled, this.label);
}
