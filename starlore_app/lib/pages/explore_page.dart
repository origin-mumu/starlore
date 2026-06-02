import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../data/mock_data.dart';
import '../widgets/category_item.dart';

/// 探索页：分组风格
class ExplorePage extends StatelessWidget {
  const ExplorePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // 头部
        _buildHeader(context),

        // 滚动内容
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 分类网格
                _buildCategoryGrid(),

                // 自定义按钮
                const SizedBox(height: 24),
                _buildCustomizeButton(),

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
        left: 20,
        right: 20,
        bottom: 20,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          // 标题
          Text(
            '分组',
            style: AppTheme.headingLarge,
          ),

          // 右侧图标
          Row(
            children: [
              // 搜索按钮
              _buildIconButton(
                icon: Icons.search_rounded,
                onTap: () {},
              ),
              const SizedBox(width: 8),
              // 更多按钮
              _buildIconButton(
                icon: Icons.more_horiz_rounded,
                onTap: () {},
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildIconButton({
    required IconData icon,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 44,
        height: 44,
        decoration: const BoxDecoration(
          shape: BoxShape.circle,
          color: AppTheme.bgBase,
        ),
        child: Icon(
          icon,
          color: AppTheme.textDark,
          size: 22,
        ),
      ),
    );
  }

  Widget _buildCategoryGrid() {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 1.1,
      ),
      itemCount: MockData.categories.length + 1, // +1 for add button
      itemBuilder: (context, index) {
        if (index == 0) {
          // 第一个位置是添加按钮
          return AddCategoryItem(
            onTap: () {
              // TODO: 添加新分类
            },
          );
        }
        return CategoryGridItem(
          category: MockData.categories[index - 1],
          onTap: () {
            // TODO: 进入分类详情
          },
        );
      },
    );
  }

  Widget _buildCustomizeButton() {
    return Center(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        decoration: BoxDecoration(
          color: AppTheme.bgCard,
          borderRadius: BorderRadius.circular(AppTheme.radiusFull),
          boxShadow: AppTheme.shadowSoft,
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.format_list_bulleted,
              size: 18,
              color: AppTheme.textDark,
            ),
            const SizedBox(width: 8),
            Text(
              '自定义',
              style: AppTheme.bodyMedium.copyWith(
                fontWeight: FontWeight.w500,
                fontSize: 14,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
