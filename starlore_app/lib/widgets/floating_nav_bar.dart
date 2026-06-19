import 'dart:ui';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';

/// 浮动底部导航栏 — 胶囊形毛玻璃 (带液体玻璃质感和滑动气泡微动效)
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
    _NavItem(Icons.add_circle_outline_rounded, Icons.add_circle_rounded, '发布'),
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
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: isDark
                      ? [
                          Colors.black.withValues(alpha: 0.45),
                          Colors.black.withValues(alpha: 0.20),
                        ]
                      : [
                          Colors.white.withValues(alpha: 0.35),
                          Colors.white.withValues(alpha: 0.10),
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
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final totalWidth = constraints.maxWidth;
                  final itemWidth = totalWidth / _items.length;
                  const indicatorWidth = 58.0;
                  const indicatorHeight = 40.0;
                  final left = currentIndex * itemWidth + (itemWidth - indicatorWidth) / 2;

                  return Stack(
                    children: [
                      // 滑动液体玻璃气泡指示器
                      AnimatedPositioned(
                        duration: Tok.normal,
                        curve: Tok.easeOutQuart,
                        left: left,
                        top: (Tok.navBarHeight - indicatorHeight) / 2,
                        width: indicatorWidth,
                        height: indicatorHeight,
                        child: Container(
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(Tok.radiusFull),
                            gradient: LinearGradient(
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                              colors: isDark
                                  ? [
                                      Colors.white.withValues(alpha: 0.20),
                                      Colors.white.withValues(alpha: 0.05),
                                    ]
                                  : [
                                      Colors.white.withValues(alpha: 0.70),
                                      Colors.white.withValues(alpha: 0.30),
                                    ],
                            ),
                            border: Border.all(
                              color: isDark
                                  ? Colors.white.withValues(alpha: 0.25)
                                  : Colors.white.withValues(alpha: 0.65),
                              width: 1.0,
                            ),
                            boxShadow: [
                              BoxShadow(
                                color: p.accent.withValues(alpha: isDark ? 0.30 : 0.12),
                                blurRadius: 16,
                                offset: const Offset(0, 4),
                              ),
                            ],
                          ),
                        ),
                      ),
                      // 交互图标行
                      Positioned.fill(
                        child: Row(
                          children: List.generate(_items.length, (i) {
                            final item = _items[i];
                            final active = currentIndex == i;
                            return Expanded(
                              child: GestureDetector(
                                onTap: () => onTap(i),
                                behavior: HitTestBehavior.opaque,
                                child: Container(
                                  height: Tok.navBarHeight,
                                  alignment: Alignment.center,
                                  child: AnimatedScale(
                                    scale: active ? 1.18 : 1.0,
                                    duration: Tok.fast,
                                    curve: Curves.easeOutBack,
                                    child: TweenAnimationBuilder<Color?>(
                                      duration: Tok.fast,
                                      tween: ColorTween(
                                        end: active ? p.accent : p.inkMuted,
                                      ),
                                      builder: (context, color, child) {
                                        return Icon(
                                          active ? item.filled : item.outline,
                                          size: 22,
                                          color: color ?? (active ? p.accent : p.inkMuted),
                                        );
                                      },
                                    ),
                                  ),
                                ),
                              ),
                            );
                          }),
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),
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
