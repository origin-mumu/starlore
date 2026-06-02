import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../models/zodiac_model.dart';

/// 分类网格项 - 分组风格卡片
class CategoryGridItem extends StatelessWidget {
  final Category category;
  final VoidCallback? onTap;

  const CategoryGridItem({super.key, required this.category, this.onTap});

  Color get _iconBgColor {
    switch (category.colorType) {
      case 'sage':
        return AppTheme.accentSage;
      case 'terracotta':
        return AppTheme.accentTerracotta;
      case 'lavender':
        return AppTheme.accentLavender;
      case 'sand':
        return AppTheme.accentSand;
      default:
        return AppTheme.accentSage;
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: AppTheme.white,
          borderRadius: BorderRadius.circular(AppTheme.radiusMD),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.04),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // 图标
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _iconBgColor.withValues(alpha: 0.15),
              ),
              child: Center(
                child: Text(
                  category.icon,
                  style: const TextStyle(fontSize: 26),
                ),
              ),
            ),
            const SizedBox(height: 12),

            // 名称
            Text(
              category.name,
              style: AppTheme.bodyMedium.copyWith(
                fontWeight: FontWeight.w600,
                fontSize: 13,
              ),
              textAlign: TextAlign.center,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 4),

            // 数量
            Text(
              category.count,
              style: AppTheme.caption.copyWith(fontSize: 11),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

/// 添加按钮卡片
class AddCategoryItem extends StatelessWidget {
  final VoidCallback? onTap;

  const AddCategoryItem({super.key, this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: AppTheme.white,
          borderRadius: BorderRadius.circular(AppTheme.radiusMD),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.04),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppTheme.bgBase,
              ),
              child: const Center(
                child: Icon(
                  Icons.add_rounded,
                  color: AppTheme.textLight,
                  size: 28,
                ),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              '添加',
              style: AppTheme.bodyMedium.copyWith(
                fontWeight: FontWeight.w500,
                fontSize: 13,
                color: AppTheme.textLight,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
