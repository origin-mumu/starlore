import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../data/mock_data.dart';
import '../widgets/zodiac_card.dart';
import '../widgets/fortune_grid.dart';
import '../widgets/celeb_card.dart';

/// 首页：今日星历
class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // 头部
        _buildHeader(context),

        // 滚动内容
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 星座主卡
                ZodiacCard(zodiac: MockData.currentZodiac),

                // 运势概览
                FortuneGrid(fortunes: MockData.fortunes),

                // 明星运势标题
                _buildSectionHeader('明星运势榜', Icons.emoji_events_rounded),
                const SizedBox(height: 8),

                // 明星列表
                ...MockData.celebrities.map(
                  (celeb) => CelebCard(celebrity: celeb),
                ),

                // 底部占位
                const SizedBox(height: 120),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Container(
      padding: const EdgeInsets.only(
        top: 8,
        left: 24,
        right: 24,
        bottom: 20,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('早安，星语者 ✨', style: AppTheme.headingLarge),
              const SizedBox(height: 4),
              Text('今日星辰为你闪耀', style: AppTheme.bodySmall),
            ],
          ),
          Container(
            width: 48,
            height: 48,
            decoration: const BoxDecoration(
              shape: BoxShape.circle,
              color: AppTheme.accentSage,
            ),
            child: const Center(
              child: Text('🌟', style: TextStyle(fontSize: 20)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title, IconData icon) {
    return Row(
      children: [
        Icon(icon, color: AppTheme.accentSage, size: 20),
        const SizedBox(width: 8),
        Text(title, style: AppTheme.headingMedium),
      ],
    );
  }
}
