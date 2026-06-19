import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';
import '../theme/typography.dart';
import '../models/article.dart';
import 'glass_card.dart';

/// 文章卡片 — 双列瀑布流 Pinterest/小红书样式
class ArticleCard extends StatelessWidget {
  final Article article;
  final VoidCallback? onTap;

  const ArticleCard({super.key, required this.article, this.onTap});

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    // 根据文章ID哈希计算一个美妙的瀑布流随机高宽比
    final double ratio = (article.id.hashCode % 3 == 0)
        ? 1.0     // 正方形 1:1
        : (article.id.hashCode % 3 == 1)
            ? 0.8  // 竖向高卡 4:5
            : 1.25; // 横向矮卡 5:4

    final hasCover =
        article.coverImage != null && article.coverImage!.isNotEmpty;

    return GlassCard(
      onTap: onTap,
      radius: Tok.radiusLg,
      margin: const EdgeInsets.only(bottom: Tok.space3),
      padding: EdgeInsets.zero,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 封面图片或马卡龙占位渐变色块
          ClipRRect(
            borderRadius: const BorderRadius.only(
              topLeft: Radius.circular(Tok.radiusLg),
              topRight: Radius.circular(Tok.radiusLg),
            ),
            child: AspectRatio(
              aspectRatio: ratio,
              child: hasCover
                  ? CachedNetworkImage(
                      imageUrl: article.coverImage!,
                      fit: BoxFit.cover,
                      placeholder: (context, url) => Container(color: p.canvasDeep),
                      errorWidget: (context, url, error) => _pastelPlaceholder(p),
                    )
                  : _pastelPlaceholder(p),
            ),
          ),
          // 文字内容区域
          Padding(
            padding: const EdgeInsets.all(Tok.space3),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 标题 (最多2行)
                Text(
                  article.title,
                  style: Typo.body(p.ink).copyWith(
                    fontWeight: FontWeight.w600,
                    height: 1.3,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: Tok.space2),
                // 作者信息与喜欢（点赞）行
                Row(
                  children: [
                    // 圆形作者头像 (首字母)
                    _buildAvatar(p),
                    const SizedBox(width: 6),
                    // 作者名字
                    Expanded(
                      child: Text(
                        article.authorName.isNotEmpty ? article.authorName : '匿名用户',
                        style: Typo.caption(p.inkSoft),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    // 喜欢心形按钮与随机赞数
                    Icon(
                      Icons.favorite_border_rounded,
                      size: 14,
                      color: p.inkMuted,
                    ),
                    const SizedBox(width: 2),
                    Text(
                      '${(article.viewCount * 0.4).round()}',
                      style: Typo.caption(p.inkMuted),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _pastelPlaceholder(StarlorePalette p) {
    // 经典马卡龙渐变底色
    final colors = [
      const Color(0xFFFFB7B2), // 樱花粉
      const Color(0xFFFFDAC1), // 蜜桃橘
      const Color(0xFFE2F0CB), // 薄荷绿
      const Color(0xFFB5EAD7), // 青提绿
      const Color(0xFFC7CEEA), // 熏衣紫
    ];
    final color = colors[article.id.hashCode % colors.length];

    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            color,
            color.withValues(alpha: 0.7),
          ],
        ),
      ),
      alignment: Alignment.center,
      child: Icon(
        Icons.auto_awesome_rounded,
        color: Colors.white.withValues(alpha: 0.9),
        size: 28,
      ),
    );
  }

  Widget _buildAvatar(StarlorePalette p) {
    final avatarColors = [
      const Color(0xFFFF9A9E),
      const Color(0xFFA1C4FD),
      const Color(0xFF84FAB0),
      const Color(0xFFFEE140),
      const Color(0xFFE0C3FC),
    ];
    final bgColor = avatarColors[article.authorName.hashCode % avatarColors.length];
    final firstChar = article.authorName.isNotEmpty ? article.authorName[0] : 'S';

    return Container(
      width: 18,
      height: 18,
      decoration: BoxDecoration(
        color: bgColor,
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: Text(
        firstChar.toUpperCase(),
        style: const TextStyle(
          color: Colors.white,
          fontSize: 10,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
