import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import '../theme/app_theme.dart';
import '../models/article_model.dart';
import '../services/api_service.dart';

class ArticleDetailPage extends StatefulWidget {
  final Article article;
  const ArticleDetailPage({super.key, required this.article});

  @override
  State<ArticleDetailPage> createState() => _ArticleDetailPageState();
}

class _ArticleDetailPageState extends State<ArticleDetailPage> {
  String? _content;
  bool _loading = true;
  bool _bookmarked = false;

  @override
  void initState() { super.initState(); _loadDetail(); _checkBookmark(); }

  Future<void> _loadDetail() async {
    final d = await ApiService.getArticleDetail(widget.article.id);
    if (mounted) setState(() { _content = d?.content ?? widget.article.description; _loading = false; });
  }

  Future<void> _checkBookmark() async {
    if (ApiService.token == null) return;
    final r = await ApiService.checkBookmark(widget.article.id);
    if (mounted) setState(() => _bookmarked = r);
  }

  Future<void> _toggleBookmark() async {
    HapticFeedback.lightImpact();
    if (_bookmarked) {
      if (await ApiService.removeBookmark(widget.article.id) && mounted) setState(() => _bookmarked = false);
    } else {
      if (await ApiService.addBookmark(widget.article.id) && mounted) setState(() => _bookmarked = true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    final a = widget.article;
    return Scaffold(
      backgroundColor: p.surface0,
      body: Stack(
        children: [
          const SpaceBackground(),
          CustomScrollView(
            physics: const BouncingScrollPhysics(),
            slivers: [
              SliverAppBar(
                expandedHeight: 0, pinned: true, backgroundColor: Colors.transparent,
                leading: GestureDetector(
                  onTap: () => Navigator.pop(context),
                  child: Container(margin: const EdgeInsets.all(8), padding: const EdgeInsets.all(4), decoration: BoxDecoration(color: p.surface1.withValues(alpha: 0.7), borderRadius: BorderRadius.circular(T.r8), border: Border.all(color: p.line, width: 0.5)), child: Icon(Icons.arrow_back_rounded, size: 16, color: p.text0)),
                ),
                actions: [
                  GestureDetector(
                    onTap: _toggleBookmark,
                    child: Container(margin: const EdgeInsets.all(8), padding: const EdgeInsets.all(6), decoration: BoxDecoration(color: p.surface1.withValues(alpha: 0.7), borderRadius: BorderRadius.circular(T.r8), border: Border.all(color: _bookmarked ? p.accent.withValues(alpha: 0.3) : p.line, width: 0.5)), child: Icon(_bookmarked ? Icons.bookmark_rounded : Icons.bookmark_border_rounded, size: 16, color: _bookmarked ? p.accent : p.text2)),
                  ),
                  const SizedBox(width: 4),
                ],
              ),
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(T.xxl, 0, T.xxl, T.xxxl),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // 分类标签
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(T.r4),
                          border: Border.all(color: p.accent.withValues(alpha: 0.2), width: 0.5),
                        ),
                        child: Text(a.category, style: F.mono(p.accent).copyWith(fontSize: 10)),
                      ),
                      const SizedBox(height: T.xl),
                      // 标题
                      Text(a.title, style: F.h1(p.text0).copyWith(height: 1.3)),
                      const SizedBox(height: T.xl),
                      // 作者信息栏
                      Container(
                        padding: const EdgeInsets.all(T.md),
                        decoration: BoxDecoration(
                          color: p.surface1.withValues(alpha: 0.7),
                          borderRadius: BorderRadius.circular(T.r12),
                          border: Border.all(color: p.line, width: 0.5),
                        ),
                        child: Row(
                          children: [
                            Container(
                              width: 32, height: 32,
                              decoration: BoxDecoration(border: Border.all(color: p.accent.withValues(alpha: 0.2), width: 0.5)),
                              child: Center(child: Text(a.authorName.isNotEmpty ? a.authorName[0] : '?', style: F.mono(p.accent).copyWith(fontSize: 12))),
                            ),
                            const SizedBox(width: T.md),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(a.authorName, style: F.body(p.text0)),
                                  Text(_fmtDate(a.createdAt), style: F.mono(p.text3).copyWith(fontSize: 10)),
                                ],
                              ),
                            ),
                            Icon(Icons.remove_red_eye_outlined, size: 14, color: p.text3),
                            const SizedBox(width: 4),
                            Text('${a.viewCount}', style: F.mono(p.text3).copyWith(fontSize: 10)),
                          ],
                        ),
                      ),
                      const SizedBox(height: T.xxl),
                      const GlowLine(),
                      const SizedBox(height: T.xxl),
                      // 内容
                      if (_loading)
                        Center(child: Padding(padding: const EdgeInsets.all(T.xxxl), child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 1, color: p.accent))))
                      else
                        MarkdownBody(
                          data: _content!,
                          styleSheet: MarkdownStyleSheet(
                            p: F.body(p.text0).copyWith(height: 1.8),
                            h1: F.h1(p.text0), h2: F.h2(p.text0), h3: F.label(p.text0),
                            strong: F.body(p.text0).copyWith(fontWeight: FontWeight.w600),
                            blockquotePadding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
                            blockquoteDecoration: BoxDecoration(
                              color: p.accent.withValues(alpha: 0.04),
                              border: Border(left: BorderSide(color: p.accent.withValues(alpha: 0.3), width: 2)),
                            ),
                            code: F.mono(p.accent).copyWith(backgroundColor: p.accent.withValues(alpha: 0.06)),
                            codeblockDecoration: BoxDecoration(color: p.surface1, border: Border.all(color: p.line, width: 0.5)),
                            codeblockPadding: const EdgeInsets.all(16),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  String _fmtDate(DateTime d) => '${d.year}.${d.month.toString().padLeft(2, '0')}.${d.day.toString().padLeft(2, '0')}';
}
