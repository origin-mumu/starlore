import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../models/zodiac_model.dart';

/// 明星运势卡片
class CelebCard extends StatelessWidget {
  final Celebrity celebrity;
  final VoidCallback? onTap;

  const CelebCard({super.key, required this.celebrity, this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppTheme.bgCard,
          borderRadius: BorderRadius.circular(AppTheme.radiusMD),
          boxShadow: AppTheme.shadowSoft,
        ),
        child: Row(
          children: [
            // 排名
            Container(
              width: 32,
              height: 32,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                color: AppTheme.accentSage,
              ),
              child: Center(
                child: Text(
                  '${celebrity.rank}',
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppTheme.white,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),

            // 头像
            Container(
              width: 56,
              height: 56,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                color: AppTheme.accentTerracotta,
              ),
              child: Center(
                child: Text(
                  celebrity.emoji,
                  style: const TextStyle(fontSize: 24),
                ),
              ),
            ),
            const SizedBox(width: 16),

            // 信息
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(celebrity.name, style: AppTheme.bodyLarge),
                  const SizedBox(height: 4),
                  Text(
                    '${celebrity.zodiac} · ${celebrity.description}',
                    style: AppTheme.bodySmall,
                  ),
                ],
              ),
            ),

            // 分数
            Text(
              '${celebrity.score}',
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: AppTheme.accentTerracotta,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
