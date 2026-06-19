import 'dart:ui';
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
import '../../services/auth_service.dart';
import '../article/article_detail.dart';
import '../article/article_editor_page.dart';

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
    AuthService.authState.addListener(_onAuthChange);
    _load();
    _scrollCtrl.addListener(_onScroll);
  }

  void _onAuthChange() {
    if (mounted) {
      setState(() {});
    }
  }

  @override
  void dispose() {
    AuthService.authState.removeListener(_onAuthChange);
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

    return Stack(
      children: [
        // 滚动的内容区域 (在底层，填充整个页面并向上延伸到状态栏)
        Positioned.fill(
          child: _loading
              ? Padding(
                  padding: const EdgeInsets.only(top: 144),
                  child: _buildSkeleton(),
                )
              : RefreshIndicator(
                  color: p.accent,
                  backgroundColor: p.surface,
                  edgeOffset: 130,
                  onRefresh: _load,
                  child: ListView(
                    controller: _scrollCtrl,
                    physics: const AlwaysScrollableScrollPhysics(
                      parent: BouncingScrollPhysics(),
                    ),
                    padding: const EdgeInsets.fromLTRB(
                      Tok.horizontalPadding,
                      144, // 顶部留空给浮动的毛玻璃 Header
                      Tok.horizontalPadding,
                      120, // 底部留空给导航栏
                    ),
                    children: [
                      _buildStaggeredGrid(context, p),
                      if (_loadingMore)
                        Center(
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
                        ),
                    ],
                  ),
                ),
        ),
        // 顶部的浮动毛玻璃背景 (高斯模糊与背景色渐变羽化，独立于内容以保证背景过渡平滑)
        Positioned(
          top: 0,
          left: 0,
          right: 0,
          height: 165, // 延伸背景高度，给渐变留出充足的羽化过渡空间
          child: ShaderMask(
            shaderCallback: (rect) {
              return const LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Colors.black,
                  Colors.black,
                  Colors.transparent,
                ],
                stops: [0.0, 0.6, 1.0], // 从 60% (99px) 开始渐变消失，到 165px 完全透明，过渡更自然
              ).createShader(rect);
            },
            blendMode: BlendMode.dstIn,
            child: ClipRect(
              child: BackdropFilter(
                filter: ImageFilter.blur(
                  sigmaX: 16.0,
                  sigmaY: 16.0,
                  tileMode: TileMode.decal, // 使用 decal 模式，防止边缘出现硬边或夹取模糊像素
                ),
                child: Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [
                        p.canvas.withValues(alpha: 0.95),
                        p.canvas.withValues(alpha: 0.65),
                        p.canvas.withValues(alpha: 0.0),
                      ],
                      stops: const [0.0, 0.6, 1.0],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
        // 顶部的浮动 Header 内容 (保持清晰，不受 ShaderMask 的渐变透明影响)
        Positioned(
          top: 0,
          left: 0,
          right: 0,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildHeader(p),
              _buildCategories(p),
              const SizedBox(height: Tok.space2),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildStaggeredGrid(BuildContext context, StarlorePalette p) {
    final leftArticles = <Article>[];
    final rightArticles = <Article>[];
    for (int i = 0; i < _articles.length; i++) {
      if (i % 2 == 0) {
        leftArticles.add(_articles[i]);
      } else {
        rightArticles.add(_articles[i]);
      }
    }

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: List.generate(leftArticles.length, (index) {
              final article = leftArticles[index];
              return StaggeredFadeIn(
                index: index * 2,
                child: ArticleCard(
                  article: article,
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => ArticleDetailPage(articleId: article.id),
                    ),
                  ),
                ),
              );
            }),
          ),
        ),
        const SizedBox(width: Tok.space3), // Spacing between columns
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: List.generate(rightArticles.length, (index) {
              final article = rightArticles[index];
              return StaggeredFadeIn(
                index: index * 2 + 1,
                child: ArticleCard(
                  article: article,
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => ArticleDetailPage(articleId: article.id),
                    ),
                  ),
                ),
              );
            }),
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
            if (AuthService.isLoggedIn)
              GestureDetector(
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (_) => const ArticleEditorPage()),
                ),
                child: Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    color: p.accent,
                    borderRadius: BorderRadius.circular(Tok.radiusMd),
                  ),
                  child: Icon(Icons.add_rounded, size: 20, color: Colors.white),
                ),
              ),
            const SizedBox(width: Tok.space2),
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
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              children: List.generate(3, (_) => const ArticleCardSkeleton()),
            ),
          ),
          const SizedBox(width: Tok.space3),
          Expanded(
            child: Column(
              children: List.generate(3, (_) => const ArticleCardSkeleton()),
            ),
          ),
        ],
      ),
    );
  }
}
