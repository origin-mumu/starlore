import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../theme/tokens.dart';
import '../../theme/typography.dart';
import '../../models/article.dart';
import '../../models/category.dart' as models;
import '../../services/article_service.dart';
import '../../widgets/article_card.dart';
import '../../widgets/category_chip.dart';
import '../../widgets/shimmer_loading.dart';
import '../../widgets/fade_in_widget.dart';
import '../article/article_detail.dart';

/// 首页 — 文章信息流
class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  List<Article> _articles = [];
  List<models.Category> _categories = [];
  int _selectedCatIndex = 0;
  bool _loading = true;
  bool _loadingMore = false;
  int _page = 1;
  bool _hasMore = true;
  final _scrollCtrl = ScrollController();

  @override
  void initState() {
    super.initState();
    _load();
    _scrollCtrl.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollCtrl.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollCtrl.position.pixels >=
            _scrollCtrl.position.maxScrollExtent - 200 &&
        !_loadingMore &&
        _hasMore) {
      _loadMore();
    }
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final results = await Future.wait([
      ArticleService.getPublicArticles(page: 1, limit: 20),
      ArticleService.getPublicCategories(),
    ]);

    if (mounted) {
      setState(() {
        _articles = results[0] as List<Article>;
        _categories = [
              const models.Category(id: 0, name: '全部'),
            ] +
            (results[1] as List<models.Category>);
        _loading = false;
        _page = 1;
        _hasMore = _articles.length >= 20;
      });
    }
  }

  Future<void> _loadMore() async {
    setState(() => _loadingMore = true);
    final next = await ArticleService.getPublicArticles(
      page: _page + 1,
      limit: 20,
      category: _selectedCatIndex == 0
          ? null
          : _categories[_selectedCatIndex].name,
    );
    if (mounted) {
      setState(() {
        _articles.addAll(next);
        _page++;
        _hasMore = next.length >= 20;
        _loadingMore = false;
      });
    }
  }

  Future<void> _onCategoryTap(int index) async {
    setState(() {
      _selectedCatIndex = index;
      _loading = true;
    });
    final category = index == 0 ? null : _categories[index].name;
    final articles =
        await ArticleService.getPublicArticles(page: 1, limit: 20, category: category);
    if (mounted) {
      setState(() {
        _articles = articles;
        _page = 1;
        _hasMore = articles.length >= 20;
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return Column(
      children: [
        _buildHeader(p),
        _buildCategories(p),
        Expanded(
          child: _loading
              ? _buildSkeleton()
              : RefreshIndicator(
                  color: p.accent,
                  backgroundColor: p.surface,
                  onRefresh: _load,
                  child: ListView.builder(
                    controller: _scrollCtrl,
                    physics: const AlwaysScrollableScrollPhysics(
                      parent: BouncingScrollPhysics(),
                    ),
                    padding: EdgeInsets.fromLTRB(
                      Tok.horizontalPadding,
                      Tok.space3,
                      Tok.horizontalPadding,
                      120, // 底部留白给导航栏
                    ),
                    itemCount: _articles.length + (_loadingMore ? 1 : 0),
                    itemBuilder: (_, i) {
                      if (i >= _articles.length) {
                        return Center(
                          child: Padding(
                            padding: const EdgeInsets.all(Tok.space5),
                            child: SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 1.5,
                                color: p.accent,
                              ),
                            ),
                          ),
                        );
                      }
                      return StaggeredFadeIn(
                        index: i,
                        child: ArticleCard(
                          article: _articles[i],
                          onTap: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) =>
                                  ArticleDetailPage(articleId: _articles[i].id),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
        ),
      ],
    );
  }

  Widget _buildHeader(StarlorePalette p) {
    return FadeInUp(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(
            Tok.horizontalPadding, Tok.space3, Tok.horizontalPadding, Tok.space2),
        child: Row(
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('STARLORE', style: Typo.h1(p.ink)),
                const SizedBox(height: 2),
                Text('星语治愈 · 每日精选', style: Typo.caption(p.inkMuted)),
              ],
            ),
            const Spacer(),
            Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(
                color: p.surface,
                borderRadius: BorderRadius.circular(Tok.radiusMd),
                border: Border.all(color: p.border, width: 0.5),
              ),
              child: Icon(Icons.search_rounded, size: 18, color: p.inkSoft),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCategories(StarlorePalette p) {
    if (_categories.isEmpty) return const SizedBox.shrink();

    return FadeInUp(
      delay: const Duration(milliseconds: 100),
      child: SizedBox(
        height: 44,
        child: ListView.separated(
          scrollDirection: Axis.horizontal,
          padding:
              const EdgeInsets.symmetric(horizontal: Tok.horizontalPadding),
          itemCount: _categories.length,
          separatorBuilder: (context, index) => const SizedBox(width: Tok.space2),
          itemBuilder: (_, i) {
            return CategoryChip(
              label: _categories[i].name,
              selected: _selectedCatIndex == i,
              onTap: () => _onCategoryTap(i),
            );
          },
        ),
      ),
    );
  }

  Widget _buildSkeleton() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Tok.horizontalPadding),
      child: ListView.builder(
        physics: const NeverScrollableScrollPhysics(),
        itemCount: 5,
        itemBuilder: (context, index) => const ArticleCardSkeleton(),
      ),
    );
  }
}
