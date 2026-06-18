import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';
import '../theme/typography.dart';
import '../models/article.dart';
import 'glass_card.dart';

/// 文章卡片 — 两种样式：有封面 / 无封面
class ArticleCard extends StatelessWidget {
  final Article article;
  final VoidCallback? onTap;

  const ArticleCard({super.key, required this.article, this.onTap});

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    final hasCover =
        article.coverImage != null && article.coverImage!.isNotEmpty;

    return GlassCard(
      onTap: onTap,
      radius: Tok.radiusLg,
      margin: const EdgeInsets.only(bottom: Tok.space3),
      padding: EdgeInsets.zero,
      child: hasCover ? _withCover(p) : _textOnly(p),
    );
  }

  Widget _withCover(StarlorePalette p) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 封面图
        AspectRatio(
          aspectRatio: 16 / 9,
          child: CachedNetworkImage(
            imageUrl: article.coverImage!,
            fit: BoxFit.cover,
            placeholder: (context, url) => Container(color: p.canvasDeep),
            errorWidget: (context, url, error) => Container(
              color: p.canvasDeep,
              child: Icon(Icons.image_outlined, color: p.inkMuted, size: 32),
            ),
          ),
        ),
        // 文字内容
        Padding(
          padding: const EdgeInsets.all(Tok.space4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _tagRow(p),
              const SizedBox(height: Tok.space2),
              Text(article.title,
                  style: Typo.h3(p.ink), maxLines: 2, overflow: TextOverflow.ellipsis),
              const SizedBox(height: Tok.space1),
              Text(article.description,
                  style: Typo.bodySmall(p.inkSoft),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis),
              const SizedBox(height: Tok.space3),
              _metaRow(p),
            ],
          ),
        ),
      ],
    );
  }

  Widget _textOnly(StarlorePalette p) {
    return Padding(
      padding: const EdgeInsets.all(Tok.space4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _tagRow(p),
          const SizedBox(height: Tok.space3),
          Text(article.title,
              style: Typo.h2(p.ink), maxLines: 2, overflow: TextOverflow.ellipsis),
          const SizedBox(height: Tok.space2),
          Text(article.description,
              style: Typo.body(p.inkSoft),
              maxLines: 3,
              overflow: TextOverflow.ellipsis),
          const SizedBox(height: Tok.space4),
          _metaRow(p),
        ],
      ),
    );
  }

  Widget _tagRow(StarlorePalette p) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
          decoration: BoxDecoration(
            color: p.accentSoft,
            borderRadius: BorderRadius.circular(Tok.radiusFull),
          ),
          child: Text(
            article.category,
            style: Typo.caption(p.accent).copyWith(fontWeight: FontWeight.w500),
          ),
        ),
        const Spacer(),
        Text(_formatTime(article.createdAt), style: Typo.caption(p.inkMuted)),
      ],
    );
  }

  Widget _metaRow(StarlorePalette p) {
    return Row(
      children: [
        Icon(Icons.person_outline_rounded, size: 14, color: p.inkMuted),
        const SizedBox(width: 4),
        Text(article.authorName, style: Typo.caption(p.inkMuted)),
        const SizedBox(width: Tok.space4),
        Icon(Icons.visibility_outlined, size: 14, color: p.inkMuted),
        const SizedBox(width: 4),
        Text('${article.viewCount}', style: Typo.caption(p.inkMuted)),
        const Spacer(),
        Icon(Icons.arrow_forward_ios_rounded, size: 12, color: p.inkMuted),
      ],
    );
  }

  String _formatTime(DateTime d) {
    final diff = DateTime.now().difference(d);
    if (diff.inMinutes < 60) return '${diff.inMinutes} 分钟前';
    if (diff.inHours < 24) return '${diff.inHours} 小时前';
    if (diff.inDays < 7) return '${diff.inDays} 天前';
    if (diff.inDays < 365) return '${d.month}/${d.day}';
    return '${d.year}/${d.month}/${d.day}';
  }
}
