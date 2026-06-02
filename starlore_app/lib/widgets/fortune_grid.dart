import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../models/zodiac_model.dart';
import 'glass_card.dart';

/// 运势概览网格
class FortuneGrid extends StatelessWidget {
  final List<Fortune> fortunes;

  const FortuneGrid({super.key, required this.fortunes});

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 标题
          Row(
            children: [
              const Icon(Icons.star_rounded, color: AppTheme.accentSage, size: 20),
              const SizedBox(width: 8),
              Text('运势概览', style: AppTheme.headingMedium),
            ],
          ),
          const SizedBox(height: 12),

          // 网格
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              childAspectRatio: 0.95,
            ),
            itemCount: fortunes.length,
            itemBuilder: (context, index) {
              return _FortuneItem(fortune: fortunes[index]);
            },
          ),
        ],
      ),
    );
  }
}

class _FortuneItem extends StatelessWidget {
  final Fortune fortune;

  const _FortuneItem({required this.fortune});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {},
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 6),
        decoration: BoxDecoration(
          color: AppTheme.bgBase,
          borderRadius: BorderRadius.circular(AppTheme.radiusSM),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(fortune.icon, style: const TextStyle(fontSize: 24)),
            const SizedBox(height: 8),
            Text(
              fortune.label,
              style: AppTheme.caption,
            ),
            const SizedBox(height: 4),
            Text(
              fortune.value,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: fortune.isWarning
                    ? AppTheme.accentTerracotta
                    : AppTheme.accentSage,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
