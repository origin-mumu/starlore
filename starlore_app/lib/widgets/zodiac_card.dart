import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../models/zodiac_model.dart';

/// 星座主卡组件
class ZodiacCard extends StatelessWidget {
  final Zodiac zodiac;

  const ZodiacCard({super.key, required this.zodiac});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 20),
      padding: const EdgeInsets.all(28),
      decoration: BoxDecoration(
        color: AppTheme.bgCard,
        borderRadius: BorderRadius.circular(AppTheme.radiusMD),
        boxShadow: AppTheme.shadowSoft,
      ),
      child: Column(
        children: [
          // 星座图标
          Container(
            width: 80,
            height: 80,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: AppTheme.gradientTerracotta,
            ),
            child: Center(
              child: Text(
                zodiac.symbol,
                style: const TextStyle(fontSize: 36),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 星座名称
          Text(
            zodiac.name,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w700,
              letterSpacing: 2,
              color: AppTheme.textDark,
            ),
          ),
          const SizedBox(height: 8),

          // 日期范围
          Text(
            zodiac.dateRange,
            style: AppTheme.bodySmall,
          ),
          const SizedBox(height: 20),

          // 运势分数
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
            decoration: BoxDecoration(
              color: AppTheme.bgBase,
              borderRadius: BorderRadius.circular(AppTheme.radiusFull),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  '今日运势',
                  style: AppTheme.bodyMedium.copyWith(color: AppTheme.textMedium),
                ),
                const SizedBox(width: 8),
                Text(
                  '${zodiac.score}',
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.w700,
                    color: AppTheme.accentTerracotta,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
