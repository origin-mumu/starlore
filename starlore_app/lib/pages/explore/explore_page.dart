import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../theme/tokens.dart';
import '../../theme/typography.dart';
import '../../models/category.dart' as models;
import '../../models/article.dart';
import '../../services/article_service.dart';
import '../../widgets/glass_card.dart';
import '../../widgets/article_card.dart';
import '../../widgets/fade_in_widget.dart';
import '../../widgets/empty_state.dart';
import '../article/article_detail.dart';
import '../../services/auth_service.dart';

/// 探索页 — 分类浏览
class ExplorePage extends StatefulWidget {
  const ExplorePage({super.key});

  @override
  State<ExplorePage> createState() => _ExplorePageState();
}

class _ExplorePageState extends State<ExplorePage> {
  List<models.Category> _categories = [];
  bool _loading = true;
  String? _selectedCategory;
  List<Article> _articles = [];
  bool _loadingArticles = false;

  @override
  void initState() {
    super.initState();
    AuthService.authState.addListener(_onAuthChange);
    _load();
  }

  void _onAuthChange() {
    if (mounted) {
      setState(() {});
    }
  }

  @override
  void dispose() {
    AuthService.authState.removeListener(_onAuthChange);
    super.dispose();
  }

  Future<void> _load() async {
    final cats = await ArticleService.getPublicCategories();
    if (mounted) {
      setState(() {
        _categories = cats;
        _loading = false;
      });
    }
  }

  Future<void> _selectCategory(String name) async {
    setState(() {
      _selectedCategory = name;
      _loadingArticles = true;
    });
    final articles =
        await ArticleService.getPublicArticles(category: name, limit: 30);
    if (mounted) {
      setState(() {
        _articles = articles;
        _loadingArticles = false;
      });
    }
  }

  void _clearSelection() {
    setState(() {
      _selectedCategory = null;
      _articles = [];
    });
  }

  static const _icons = <String, IconData>{
    '星座': Icons.auto_awesome_rounded,
    '运势': Icons.trending_up_rounded,
    '情感': Icons.favorite_outline_rounded,
    '生活': Icons.wb_sunny_outlined,
    '健康': Icons.spa_outlined,
    '财运': Icons.account_balance_wallet_outlined,
    '事业': Icons.work_outline_rounded,
    '塔罗': Icons.style_outlined,
  };

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return Column(
      children: [
        // Header
        FadeInUp(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(
                Tok.horizontalPadding, Tok.space3, Tok.horizontalPadding, Tok.space4),
            child: Row(
              children: [
                if (_selectedCategory != null) ...[
                  GestureDetector(
                    onTap: _clearSelection,
                    child: Icon(Icons.arrow_back_ios_rounded,
                        size: 18, color: p.ink),
                  ),
                  const SizedBox(width: Tok.space3),
                ],
                Text(
                  _selectedCategory ?? '探索',
                  style: Typo.h1(p.ink),
                ),
                const Spacer(),
                Text(
                  '${_categories.length} 个分类',
                  style: Typo.caption(p.inkMuted),
                ),
              ],
            ),
          ),
        ),
        // 内容
        Expanded(
          child: _loading
              ? Center(
                  child: CircularProgressIndicator(
                      strokeWidth: 1.5, color: p.accent))
              : _selectedCategory != null
                  ? _buildArticleList(p)
                  : _buildCategoryGrid(p),
        ),
      ],
    );
  }

  Widget _buildCategoryGrid(StarlorePalette p) {
    return GridView.builder(
      physics: const BouncingScrollPhysics(),
      padding: EdgeInsets.fromLTRB(
          Tok.horizontalPadding, 0, Tok.horizontalPadding, 120),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisSpacing: Tok.space3,
        crossAxisSpacing: Tok.space3,
        childAspectRatio: 1.4,
      ),
      itemCount: _categories.length,
      itemBuilder: (_, i) {
        final cat = _categories[i];
        final icon = _icons[cat.name] ?? Icons.tag_rounded;

        return StaggeredFadeIn(
          index: i,
          child: GlassCard(
            blur: false,
            radius: Tok.radiusLg,
            padding: const EdgeInsets.all(Tok.space4),
            onTap: () => _selectCategory(cat.name),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: p.accentSoft,
                    borderRadius: BorderRadius.circular(Tok.radiusMd),
                  ),
                  child: Icon(icon, size: 20, color: p.accent),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(cat.name, style: Typo.h3(p.ink)),
                    const SizedBox(height: 2),
                    Text(
                      '${cat.articleCount} 篇文章',
                      style: Typo.caption(p.inkMuted),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildArticleList(StarlorePalette p) {
    if (_loadingArticles) {
      return Center(
        child: CircularProgressIndicator(strokeWidth: 1.5, color: p.accent),
      );
    }

    if (_articles.isEmpty) {
      return EmptyState(
        icon: Icons.article_outlined,
        title: '暂无文章',
        subtitle: '该分类下还没有内容，去其他分类看看吧',
        actionLabel: '返回分类',
        onAction: _clearSelection,
      );
    }

    return ListView.builder(
      physics: const BouncingScrollPhysics(),
      padding: EdgeInsets.fromLTRB(
          Tok.horizontalPadding, 0, Tok.horizontalPadding, 120),
      itemCount: _articles.length,
      itemBuilder: (_, i) {
        return StaggeredFadeIn(
          index: i,
          child: ArticleCard(
            article: _articles[i],
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => ArticleDetailPage(articleId: _articles[i].id),
              ),
            ),
          ),
        );
      },
    );
  }
}
