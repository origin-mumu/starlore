import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../services/api_service.dart';
import '../models/article_model.dart';
import 'article_detail_page.dart';

class ExplorePage extends StatefulWidget {
  const ExplorePage({super.key});

  @override
  State<ExplorePage> createState() => _ExplorePageState();
}

class _ExplorePageState extends State<ExplorePage> {
  List<Category> _cats = [];
  List<Article> _hot = [];
  bool _loading = true;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    final c = await ApiService.getCategories();
    final a = await ApiService.getArticles(limit: 30);
    if (mounted) setState(() { _cats = c; _hot = a..sort((x, y) => y.viewCount.compareTo(x.viewCount)); _loading = false; });
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    return _loading
        ? Center(child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 1, color: p.accent)))
        : RefreshIndicator(
            color: p.accent, backgroundColor: p.surface1,
            onRefresh: _load,
            child: CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: [
                SliverToBoxAdapter(child: _header(p)),
                SliverToBoxAdapter(child: _grid(p)),
                SliverToBoxAdapter(child: _hotHeader(p)),
                _hotList(p),
                const SliverToBoxAdapter(child: SizedBox(height: 120)),
              ],
            ),
          );
  }

  Widget _header(StarlorePalette p) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(T.xxl, T.md, T.xxl, T.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('EXPLORE', style: F.h1(p.text0)),
          const SizedBox(height: 2),
          Text('// 数据索引', style: F.mono(p.text2)),
        ],
      ),
    );
  }

  Widget _grid(StarlorePalette p) {
    final icons = ['♈', '🔮', '💕', '🌙', '⭐', '🃏'];
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: T.xxl),
      child: GridView.builder(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, mainAxisSpacing: T.sm, crossAxisSpacing: T.sm, childAspectRatio: 1.0),
        itemCount: _cats.length,
        itemBuilder: (_, i) {
          final c = _cats[i];
          final icon = i < icons.length ? icons[i] : '◈';
          return Container(
            decoration: BoxDecoration(
              color: p.surface1.withValues(alpha: 0.7),
              borderRadius: BorderRadius.circular(T.r12),
              border: Border.all(color: p.line, width: 0.5),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(icon, style: TextStyle(fontSize: 20, color: p.accent)),
                const SizedBox(height: T.sm),
                Text(c.name, style: F.label(p.text0).copyWith(fontSize: 10)),
                const SizedBox(height: 2),
                Text('${c.articleCount}', style: F.mono(p.text2).copyWith(fontSize: 9)),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _hotHeader(StarlorePalette p) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(T.xxl, T.xxl, T.xxl, T.md),
      child: Row(
        children: [
          Container(width: 3, height: 12, color: p.accent),
          const SizedBox(width: T.sm),
          Text('TRENDING', style: F.label(p.text1)),
          const Spacer()],
      ),
    );
  }

  Widget _hotList(StarlorePalette p) {
    return SliverPadding(
      padding: const EdgeInsets.symmetric(horizontal: T.xxl),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate((_, i) {
          final a = _hot[i];
          final top = i < 3;
          return GestureDetector(
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => ArticleDetailPage(article: a))),
            child: Container(
              margin: const EdgeInsets.only(bottom: T.sm),
              padding: const EdgeInsets.all(T.md),
              decoration: BoxDecoration(
                color: p.surface1.withValues(alpha: 0.7),
                borderRadius: BorderRadius.circular(T.r12),
                border: Border.all(color: top ? p.accent.withValues(alpha: 0.15) : p.line, width: 0.5),
              ),
              child: Row(
                children: [
                  SizedBox(
                    width: 28,
                    child: Text('${i + 1}'.padLeft(2, '0'), style: F.mono(top ? p.accent : p.text2)),
                  ),
                  const SizedBox(width: T.md),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(a.title, style: F.body(p.text0), maxLines: 1, overflow: TextOverflow.ellipsis),
                        const SizedBox(height: 2),
                        Text('${a.category}  ·  ${a.viewCount} views', style: F.mono(p.text3).copyWith(fontSize: 9)),
                      ],
                    ),
                  ),
                  Icon(Icons.chevron_right_rounded, size: 16, color: p.text3),
                ],
              ),
            ),
          );
        }, childCount: _hot.length),
      ),
    );
  }
}
