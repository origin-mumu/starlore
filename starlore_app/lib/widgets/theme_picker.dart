import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';

/// 主题选择器 — 6 色圆点
class ThemePicker extends StatelessWidget {
  final ThemeNotifier notifier;

  const ThemePicker({super.key, required this.notifier});

  @override
  Widget build(BuildContext context) {
    final current = paletteOf(context);

    return ValueListenableBuilder<StarlorePalette>(
      valueListenable: notifier,
      builder: (context, selected, child) {
        return Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: allPalettes.map((palette) {
            final isSelected = palette.key == selected.key;
            return GestureDetector(
              onTap: () => notifier.setTheme(palette),
              child: AnimatedContainer(
                duration: Tok.fast,
                curve: Tok.easeOutQuart,
                margin: const EdgeInsets.symmetric(horizontal: 6),
                width: isSelected ? 28 : 22,
                height: isSelected ? 28 : 22,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: palette.dot,
                  border: Border.all(
                    color: isSelected
                        ? current.ink.withValues(alpha: 0.3)
                        : current.border.withValues(alpha: 0.3),
                    width: isSelected ? 2.5 : 1,
                  ),
                  boxShadow: isSelected
                      ? [
                          BoxShadow(
                            color: palette.dot.withValues(alpha: 0.3),
                            blurRadius: 12,
                            spreadRadius: 1,
                          ),
                        ]
                      : null,
                ),
              ),
            );
          }).toList(),
        );
      },
    );
  }
}
