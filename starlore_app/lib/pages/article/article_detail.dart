import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../theme/app_theme.dart';
import '../../theme/tokens.dart';
import '../../theme/typography.dart';
import '../../models/article.dart';
import '../../services/article_service.dart';
import '../../services/bookmark_service.dart';
import '../../services/auth_service.dart';
import '../../widgets/fade_in_widget.dart';

/// 文章详情 — 沉浸式阅读
class ArticleDetailPage extends StatefulWidget {
  final int articleId;

  const ArticleDetailPage({super.key, required this.articleId});

  @override
  State<ArticleDetailPage> createState() => _ArticleDetailPageState();
}

class _ArticleDetailPageState extends State<ArticleDetailPage> {
  ArticleDetail? _article;
  bool _loading = true;
  bool _bookmarked = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final article =
        await ArticleService.getPublicArticleDetail(widget.articleId);
    if (mounted) {
      setState(() {
        _article = article;
        _loading = false;
      });
    }
    // 检查收藏状态
    if (AuthService.isLoggedIn && article != null) {
      final bookmarked = await BookmarkService.isBookmarked(article.id);
      if (mounted) setState(() => _bookmarked = bookmarked);
    }
  }

  Future<void> _toggleBookmark() async {
    if (!AuthService.isLoggedIn || _article == null) return;
    final result = await BookmarkService.toggle(_article!.id);
    if (result && mounted) {
      setState(() => _bookmarked = !_bookmarked);
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    if (_loading) {
      return Scaffold(
        backgroundColor: p.canvas,
        body: Center(
          child: CircularProgressIndicator(strokeWidth: 1.5, color: p.accent),
        ),
      );
    }

    if (_article == null) {
      return Scaffold(
        backgroundColor: p.canvas,
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.error_outline_rounded, size: 48, color: p.inkMuted),
              const SizedBox(height: Tok.space3),
              Text('文章加载失败', style: Typo.body(p.inkMuted)),
              const SizedBox(height: Tok.space4),
              GestureDetector(
                onTap: () => Navigator.of(context).pop(),
                child: Text('返回', style: Typo.label(p.accent)),
              ),
            ],
          ),
        ),
      );
    }

    final article = _article!;
    final hasCover =
        article.coverImage != null && article.coverImage!.isNotEmpty;

    return Scaffold(
      backgroundColor: p.canvas,
      body: Stack(
        children: [
          CustomScrollView(
            physics: const BouncingScrollPhysics(),
            slivers: [
              // 封面图 + 返回按钮
              SliverAppBar(
                expandedHeight: hasCover ? 260 : 0,
                pinned: true,
                backgroundColor: p.canvas,
                leading: _backButton(p),
                flexibleSpace: hasCover
                    ? FlexibleSpaceBar(
                        background: CachedNetworkImage(
                          imageUrl: article.coverImage!,
                          fit: BoxFit.cover,
                          errorWidget: (context, url, error) =>
                              Container(color: p.canvasDeep),
                        ),
                      )
                    : null,
              ),
              // 文章内容
              SliverToBoxAdapter(
                child: FadeInUp(
                  child: Padding(
                    padding: const EdgeInsets.all(Tok.horizontalPadding),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // 分类标签
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 12, vertical: 5),
                          decoration: BoxDecoration(
                            color: p.accentSoft,
                            borderRadius:
                                BorderRadius.circular(Tok.radiusFull),
                          ),
                          child: Text(article.category,
                              style: Typo.caption(p.accent)
                                  .copyWith(fontWeight: FontWeight.w500)),
                        ),
                        const SizedBox(height: Tok.space4),
                        // 标题
                        Text(article.title, style: Typo.display(p.ink)),
                        const SizedBox(height: Tok.space4),
                        // Meta
                        Row(
                          children: [
                            Icon(Icons.person_outline_rounded,
                                size: 14, color: p.inkMuted),
                            const SizedBox(width: 4),
                            Text(article.authorName,
                                style: Typo.caption(p.inkMuted)),
                            const SizedBox(width: Tok.space4),
                            Icon(Icons.access_time_rounded,
                                size: 14, color: p.inkMuted),
                            const SizedBox(width: 4),
                            Text(
                              _formatDate(article.createdAt),
                              style: Typo.caption(p.inkMuted),
                            ),
                            const SizedBox(width: Tok.space4),
                            Icon(Icons.visibility_outlined,
                                size: 14, color: p.inkMuted),
                            const SizedBox(width: 4),
                            Text('${article.viewCount}',
                                style: Typo.caption(p.inkMuted)),
                          ],
                        ),
                        const SizedBox(height: Tok.space6),
                        // Markdown 正文
                        MarkdownBody(
                          data: article.content,
                          styleSheet: _markdownStyle(p),
                          selectable: true,
                        ),
                        const SizedBox(height: Tok.space8),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
          // 底部操作栏
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: _buildBottomBar(p),
          ),
        ],
      ),
    );
  }

  Widget _backButton(StarlorePalette p) {
    return GestureDetector(
      onTap: () => Navigator.of(context).pop(),
      child: Container(
        margin: const EdgeInsets.all(8),
        width: 36,
        height: 36,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: p.surface.withValues(alpha: 0.8),
          border:
              Border.all(color: p.border.withValues(alpha: 0.5), width: 0.5),
        ),
        child: Icon(Icons.arrow_back_ios_rounded, size: 16, color: p.ink),
      ),
    );
  }

  Widget _buildBottomBar(StarlorePalette p) {
    final bottom = MediaQuery.of(context).padding.bottom;

    return Container(
      padding: EdgeInsets.fromLTRB(
          Tok.horizontalPadding, Tok.space3, Tok.horizontalPadding, bottom + Tok.space3),
      decoration: BoxDecoration(
        color: p.surface.withValues(alpha: 0.9),
        border: Border(
          top: BorderSide(color: p.border.withValues(alpha: 0.5), width: 0.5),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          if (AuthService.isLoggedIn)
            _actionButton(
              p,
              icon: _bookmarked
                  ? Icons.bookmark_rounded
                  : Icons.bookmark_outline_rounded,
              label: _bookmarked ? '已收藏' : '收藏',
              active: _bookmarked,
              onTap: _toggleBookmark,
            ),
          _actionButton(
            p,
            icon: Icons.share_outlined,
            label: '分享',
            onTap: () {},
          ),
        ],
      ),
    );
  }

  Widget _actionButton(StarlorePalette p,
      {required IconData icon,
      required String label,
      bool active = false,
      required VoidCallback onTap}) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 22, color: active ? p.accent : p.inkSoft),
          const SizedBox(height: 4),
          Text(label,
              style: Typo.caption(active ? p.accent : p.inkMuted)),
        ],
      ),
    );
  }

  String _formatDate(DateTime d) {
    return '${d.year}/${d.month.toString().padLeft(2, '0')}/${d.day.toString().padLeft(2, '0')}';
  }

  MarkdownStyleSheet _markdownStyle(StarlorePalette p) {
    return MarkdownStyleSheet(
      p: Typo.body(p.ink).copyWith(height: 1.8),
      h1: Typo.h1(p.ink),
      h2: Typo.h2(p.ink),
      h3: Typo.h3(p.ink),
      code: Typo.mono(p.accent).copyWith(
        backgroundColor: p.accentSoft,
      ),
      codeblockDecoration: BoxDecoration(
        color: p.canvasDeep,
        borderRadius: BorderRadius.circular(Tok.radiusMd),
        border: Border.all(color: p.border, width: 0.5),
      ),
      blockquoteDecoration: BoxDecoration(
        color: p.warmSoft,
        borderRadius: BorderRadius.circular(Tok.radiusSm),
      ),
      blockquotePadding: const EdgeInsets.all(Tok.space4),
      listBullet: Typo.body(p.accent),
      horizontalRuleDecoration: BoxDecoration(
        border: Border(
          top: BorderSide(color: p.border, width: 0.5),
        ),
      ),
    );
  }
}
