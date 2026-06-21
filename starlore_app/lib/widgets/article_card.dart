import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';
import '../theme/typography.dart';
import '../models/article.dart';
import 'glass_card.dart';

/// 文章卡片 — 双列瀑布流 Pinterest/小红书样式
class ArticleCard extends StatefulWidget {
  final Article article;
  final VoidCallback? onTap;

  const ArticleCard({super.key, required this.article, this.onTap});

  @override
  State<ArticleCard> createState() => _ArticleCardState();
}

class _ArticleCardState extends State<ArticleCard> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    final article = widget.article;

    // 根据文章ID哈希计算一个美妙的瀑布流随机高宽比
    final double ratio = (article.id.hashCode % 3 == 0)
        ? 1.0 // 正方形 1:1
        : (article.id.hashCode % 3 == 1)
            ? 0.8 // 竖向高卡 4:5
            : 1.25; // 横向矮卡 5:4

    final hasCover =
        article.coverImage != null && article.coverImage!.isNotEmpty;

    return GestureDetector(
      onTap: widget.onTap,
      onTapDown: (_) => setState(() => _pressed = true),
      onTapUp: (_) => setState(() => _pressed = false),
      onTapCancel: () => setState(() => _pressed = false),
      child: AnimatedScale(
        scale: _pressed ? 0.97 : 1.0,
        duration: Tok.fast,
        curve: Curves.easeOut,
        child: GlassCard(
          radius: Tok.radiusLg,
          margin: const EdgeInsets.only(bottom: Tok.space3),
          padding: EdgeInsets.zero,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 封面图片 (如果没有封面就不要展示封面)
              if (hasCover)
                ClipRRect(
                  borderRadius: const BorderRadius.only(
                    topLeft: Radius.circular(Tok.radiusLg),
                    topRight: Radius.circular(Tok.radiusLg),
                  ),
                  child: AspectRatio(
                    aspectRatio: ratio,
                    child: CachedNetworkImage(
                      imageUrl: article.coverImage!,
                      fit: BoxFit.cover,
                      placeholder: (context, url) =>
                          Container(color: p.canvasDeep),
                      errorWidget: (context, url, error) =>
                          Container(color: p.canvasDeep),
                    ),
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
                    // 作者信息行（仅头像+名字，移除虚假点赞数）
                    Row(
                      children: [
                        _buildAvatar(p),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            article.authorName.isNotEmpty
                                ? article.authorName
                                : '匿名用户',
                            style: Typo.caption(p.inkSoft),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
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
    final bgColor =
        avatarColors[widget.article.authorName.hashCode % avatarColors.length];
    final firstChar =
        widget.article.authorName.isNotEmpty ? widget.article.authorName[0] : 'S';

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
