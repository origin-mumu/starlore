import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';
import '../theme/typography.dart';

/// 分类标签（横滑选择）
class CategoryChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const CategoryChip({
    super.key,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: Tok.fast,
        curve: Tok.easeOutQuart,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: selected ? p.accentSoft : p.surface,
          borderRadius: BorderRadius.circular(Tok.radiusFull),
          border: Border.all(
            color: selected
                ? p.accent.withValues(alpha: 0.3)
                : p.border.withValues(alpha: 0.5),
            width: 0.5,
          ),
          boxShadow: selected
              ? [
                  BoxShadow(
                    color: p.accent.withValues(alpha: 0.08),
                    blurRadius: 12,
                    offset: const Offset(0, 2),
                  ),
                ]
              : null,
        ),
        child: Center(
          child: Text(
            label,
            style: Typo.label(selected ? p.accent : p.inkSoft),
          ),
        ),
      ),
    );
  }
}
