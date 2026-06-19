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
                borderRadius: BorderRadius.circular(Tok.navBarRadius),
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: isDark
                      ? [
                          Colors.black.withValues(alpha: 0.35),
                          Colors.black.withValues(alpha: 0.15),
                        ]
                      : [
                          Colors.white.withValues(alpha: 0.25),
                          Colors.white.withValues(alpha: 0.08),
                        ],
                ),
                border: Border.all(
                  color: isDark
                      ? Colors.white.withValues(alpha: 0.12)
                      : Colors.white.withValues(alpha: 0.45),
                  width: 1.2,
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: isDark ? 0.35 : 0.08),
                    blurRadius: 24,
                    offset: const Offset(0, 10),
                  ),
                  BoxShadow(
                    color: p.accent.withValues(alpha: isDark ? 0.12 : 0.04),
                    blurRadius: 32,
                    offset: const Offset(0, 4),
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
    final isDark = p.brightness == Brightness.dark;
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
          borderRadius: BorderRadius.circular(Tok.radiusFull),
          color: active
              ? (isDark
                  ? Colors.white.withValues(alpha: 0.15)
                  : Colors.white.withValues(alpha: 0.6))
              : Colors.transparent,
          border: Border.all(
            color: active
                ? (isDark
                    ? Colors.white.withValues(alpha: 0.25)
                    : Colors.white.withValues(alpha: 0.7))
                : Colors.transparent,
            width: 1.0,
          ),
          boxShadow: active
              ? [
                  BoxShadow(
                    color: p.accent.withValues(alpha: 0.15),
                    blurRadius: 10,
                    offset: const Offset(0, 3),
                  ),
                ]
              : [],
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
