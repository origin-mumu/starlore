import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../widgets/glass_card.dart';

/// 我的页面
class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});

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
                // 个人信息卡
                _buildProfileCard(),

                // 我的收藏标题
                _buildSectionHeader('我的收藏', Icons.favorite_rounded),
                const SizedBox(height: 8),

                // 收藏列表
                _buildFavoriteItem(
                  icon: '♌',
                  title: '狮子座深度解析',
                  subtitle: '性格、爱情、事业全攻略',
                  color: AppTheme.accentSage,
                ),
                _buildFavoriteItem(
                  icon: '♏',
                  title: '天蝎座速配指南',
                  subtitle: '最佳伴侣排行榜',
                  color: AppTheme.accentLavender,
                ),

                // 设置标题
                _buildSectionHeader('设置', Icons.settings_rounded),
                const SizedBox(height: 8),

                // 设置项
                _buildSettingsCard(),

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
              Text('我的星语', style: AppTheme.headingLarge),
              const SizedBox(height: 4),
              Text('管理你的星辰档案', style: AppTheme.bodySmall),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildProfileCard() {
    return GlassCard(
      padding: const EdgeInsets.all(32),
      child: Column(
        children: [
          const CircleAvatar(
            radius: 40,
            backgroundColor: AppTheme.accentSage,
            child: Text('🌟', style: TextStyle(fontSize: 36)),
          ),
          const SizedBox(height: 16),
          const Text(
            '星语者',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: AppTheme.textDark,
            ),
          ),
          const SizedBox(height: 4),
          Text('狮子座 · 生日 08.15', style: AppTheme.bodySmall),
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

  Widget _buildFavoriteItem({
    required String icon,
    required String title,
    required String subtitle,
    required Color color,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.bgCard,
        borderRadius: BorderRadius.circular(AppTheme.radiusMD),
        boxShadow: AppTheme.shadowSoft,
      ),
      child: Row(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: color.withValues(alpha: 0.15),
            ),
            child: Center(
              child: Text(icon, style: const TextStyle(fontSize: 24)),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: AppTheme.bodyLarge),
                const SizedBox(height: 4),
                Text(subtitle, style: AppTheme.bodySmall),
              ],
            ),
          ),
          const Icon(
            Icons.chevron_right_rounded,
            color: AppTheme.textLight,
            size: 24,
          ),
        ],
      ),
    );
  }

  Widget _buildSettingsCard() {
    return GlassCard(
      padding: EdgeInsets.zero,
      child: Column(
        children: [
          _buildSettingItem(
            icon: Icons.notifications_rounded,
            iconColor: AppTheme.accentSage,
            title: '每日运势提醒',
            trailing: Container(
              width: 48,
              height: 28,
              decoration: BoxDecoration(
                color: AppTheme.accentSage,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Align(
                alignment: Alignment.centerRight,
                child: Container(
                  width: 24,
                  height: 24,
                  margin: const EdgeInsets.only(right: 2),
                  decoration: const BoxDecoration(
                    shape: BoxShape.circle,
                    color: AppTheme.white,
                  ),
                ),
              ),
            ),
          ),
          _buildSettingItem(
            icon: Icons.dark_mode_rounded,
            iconColor: AppTheme.accentLavender,
            title: '深色模式',
            trailing: Container(
              width: 48,
              height: 28,
              decoration: BoxDecoration(
                color: AppTheme.textLight.withValues(alpha: 0.3),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Container(
                  width: 24,
                  height: 24,
                  margin: const EdgeInsets.only(left: 2),
                  decoration: const BoxDecoration(
                    shape: BoxShape.circle,
                    color: AppTheme.white,
                  ),
                ),
              ),
            ),
            showDivider: true,
          ),
          _buildSettingItem(
            icon: Icons.info_outline_rounded,
            iconColor: AppTheme.accentTerracotta,
            title: '关于 Starelore',
            trailing: const Icon(
              Icons.chevron_right_rounded,
              color: AppTheme.textLight,
              size: 20,
            ),
            showDivider: false,
          ),
        ],
      ),
    );
  }

  Widget _buildSettingItem({
    required IconData icon,
    required Color iconColor,
    required String title,
    required Widget trailing,
    bool showDivider = true,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      decoration: showDivider
          ? BoxDecoration(
              border: Border(
                bottom: BorderSide(
                  color: Colors.black.withValues(alpha: 0.04),
                  width: 1,
                ),
              ),
            )
          : null,
      child: Row(
        children: [
          Icon(icon, color: iconColor, size: 22),
          const SizedBox(width: 12),
          Expanded(
            child: Text(title, style: AppTheme.bodyMedium),
          ),
          trailing,
        ],
      ),
    );
  }
}
