import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../services/api_service.dart';
import '../models/article_model.dart';
import 'article_detail_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  List<Article> _articles = [];
  List<Category> _categories = [];
  int _catIdx = 0;
  bool _loading = true;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    final a = await ApiService.getArticles(limit: 30);
    final c = await ApiService.getCategories();
    if (mounted) setState(() { _articles = a; _categories = [const Category(id: 0, name: '全部')] + c; _loading = false; });
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    return Column(
      children: [
        _header(p),
        Expanded(
          child: _loading
              ? Center(child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 1, color: p.accent)))
              : RefreshIndicator(
                  color: p.accent,
                  backgroundColor: p.surface1,
                  onRefresh: _load,
                  child: CustomScrollView(
                    physics: const BouncingScrollPhysics(),
                    slivers: [
                      SliverToBoxAdapter(child: _statusCard(p)),
                      SliverToBoxAdapter(child: _sectionBar('数据概览', p)),
                      SliverToBoxAdapter(child: _dataGrid(p)),
                      SliverToBoxAdapter(child: _sectionBar('文章流', p)),
                      SliverToBoxAdapter(child: _cats(p)),
                      _list(p),
                      const SliverToBoxAdapter(child: SizedBox(height: 120)),
                    ],
                  ),
                ),
        ),
      ],
    );
  }

  Widget _header(StarlorePalette p) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(T.xxl, T.md, T.xxl, T.sm),
      child: Row(
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('STARLORE', style: F.h1(p.text0)),
              const SizedBox(height: 2),
              Text('// 星辰协议 v1.0', style: F.mono(p.text2)),
            ],
          ),
          const Spacer(),
          Container(
            width: 36, height: 36,
            decoration: BoxDecoration(border: Border.all(color: p.lineStrong, width: 0.5)),
            child: Icon(Icons.notifications_none_rounded, size: 16, color: p.text2),
          ),
        ],
      ),
    );
  }

  Widget _statusCard(StarlorePalette p) {
    return FrostedCard(
      margin: const EdgeInsets.fromLTRB(T.xxl, T.sm, T.xxl, T.lg),
      glow: true,
      padding: const EdgeInsets.all(T.lg),
      child: Row(
        children: [
          Container(width: 2, height: 48, decoration: BoxDecoration(borderRadius: BorderRadius.circular(1), color: p.accent.withValues(alpha: 0.4))),
          const SizedBox(width: T.lg),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('系统状态', style: F.caption(p.text2)),
                const SizedBox(height: 4),
                Row(
                  children: [
                    GlowDot(color: p.accent, size: 5),
                    const SizedBox(width: 8),
                    Text('ONLINE', style: F.mono(p.accent)),
                    const SizedBox(width: T.lg),
                    Text('${_articles.length} 篇文章', style: F.mono(p.text1)),
                  ],
                ),
              ],
            ),
          ),
          // 右侧数据
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text('92', style: F.h1(p.accent)),
              Text('运势指数', style: F.caption(p.text2)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _sectionBar(String title, StarlorePalette p) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(T.xxl, T.lg, T.xxl, T.md),
      child: Row(
        children: [
          Container(width: 3, height: 12, color: p.accent),
          const SizedBox(width: T.sm),
          Text(title.toUpperCase(), style: F.label(p.text1)),
          const Spacer(),
          Container(width: 40, height: 1, color: p.line),
        ],
      ),
    );
  }

  Widget _dataGrid(StarlorePalette p) {
    final items = [
      ('LOVE', '88%', p.accent),
      ('WEALTH', '75%', p.accentDim),
      ('HEALTH', '90%', p.text1),
    ];
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: T.xxl),
      child: Row(
        children: items.map((e) => Expanded(
          child: Container(
            margin: EdgeInsets.only(left: e == items.first ? 0 : T.sm, right: e == items.last ? 0 : T.sm),
            padding: const EdgeInsets.symmetric(vertical: T.md, horizontal: T.sm),
            decoration: BoxDecoration(
              color: p.surface1.withValues(alpha: 0.6),
              borderRadius: BorderRadius.circular(T.r12),
              border: Border.all(color: p.line, width: 0.5),
            ),
            child: Column(
              children: [
                Text(e.$1, style: F.mono(p.text2).copyWith(fontSize: 10)),
                const SizedBox(height: 4),
                Text(e.$2, style: F.monoLg(e.$3)),
              ],
            ),
          ),
        )).toList(),
      ),
    );
  }

  Widget _cats(StarlorePalette p) {
    return SizedBox(
      height: 32,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: T.xxl),
        itemCount: _categories.length,
        separatorBuilder: (_, _) => const SizedBox(width: T.sm),
        itemBuilder: (_, i) {
          final c = _categories[i];
          final sel = _catIdx == i;
          return GestureDetector(
            onTap: () => setState(() => _catIdx = i),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                color: sel ? p.accent.withValues(alpha: 0.1) : p.surface1.withValues(alpha: 0.6),
                borderRadius: BorderRadius.circular(T.rFull),
                border: Border.all(color: sel ? p.accent.withValues(alpha: 0.3) : p.line, width: 0.5),
              ),
              alignment: Alignment.center,
              child: Text(c.name, style: F.label(sel ? p.accent : p.text2).copyWith(fontSize: 10)),
            ),
          );
        },
      ),
    );
  }

  Widget _list(StarlorePalette p) {
    final articles = _catIdx == 0 ? _articles : _articles.where((a) => a.category == _categories[_catIdx].name).toList();
    if (articles.isEmpty) return SliverFillRemaining(child: Center(child: Text('NO DATA', style: F.mono(p.text2))));

    return SliverPadding(
      padding: const EdgeInsets.fromLTRB(T.xxl, T.md, T.xxl, 0),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate(
          (_, i) => _articleCard(articles[i], p),
          childCount: articles.length,
        ),
      ),
    );
  }

  Widget _articleCard(Article a, StarlorePalette p) {
    return GestureDetector(
      onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ArticleDetailPage(article: a))),
      child: Container(
        margin: const EdgeInsets.only(bottom: T.sm),
        padding: const EdgeInsets.all(T.lg),
        decoration: BoxDecoration(
          color: p.surface1.withValues(alpha: 0.7),
          borderRadius: BorderRadius.circular(T.r12),
          border: Border.all(color: p.line, width: 0.5),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 标签行
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(T.r4),
                    border: Border.all(color: p.accent.withValues(alpha: 0.2), width: 0.5),
                  ),
                  child: Text(a.category, style: F.mono(p.accent).copyWith(fontSize: 9)),
                ),
                const Spacer(),
                Text(_fmtTime(a.createdAt), style: F.mono(p.text3).copyWith(fontSize: 9)),
              ],
            ),
            const SizedBox(height: T.md),
            // 标题
            Text(a.title, style: F.h2(p.text0), maxLines: 2, overflow: TextOverflow.ellipsis),
            const SizedBox(height: T.sm),
            // 摘要
            Text(a.description, style: F.bodySmall(p.text1), maxLines: 2, overflow: TextOverflow.ellipsis),
            const SizedBox(height: T.md),
            // 底部
            Row(
              children: [
                Text(a.authorName, style: F.mono(p.text2).copyWith(fontSize: 10)),
                const SizedBox(width: T.lg),
                Icon(Icons.remove_red_eye_outlined, size: 12, color: p.text3),
                const SizedBox(width: 4),
                Text('${a.viewCount}', style: F.mono(p.text3).copyWith(fontSize: 10)),
                const Spacer(),
                Container(width: 20, height: 1, color: p.line),
                const SizedBox(width: 4),
                Icon(Icons.arrow_forward_rounded, size: 12, color: p.text3),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _fmtTime(DateTime d) {
    final diff = DateTime.now().difference(d);
    if (diff.inMinutes < 60) return '${diff.inMinutes}m';
    if (diff.inHours < 24) return '${diff.inHours}h';
    if (diff.inDays < 7) return '${diff.inDays}d';
    return '${d.month}/${d.day}';
  }
}
