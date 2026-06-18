import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../theme/tokens.dart';
import '../../theme/typography.dart';
import '../../services/auth_service.dart';
import '../../services/bookmark_service.dart';
import '../../models/article.dart';
import '../../widgets/glass_card.dart';
import '../../widgets/theme_picker.dart';
import '../../widgets/article_card.dart';
import '../../widgets/fade_in_widget.dart';
import '../../main.dart';
import '../article/article_detail.dart';
import 'login_page.dart';

/// 个人中心
class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  List<Article> _bookmarks = [];
  bool _loadingBookmarks = false;

  @override
  void initState() {
    super.initState();
    if (AuthService.isLoggedIn) _loadBookmarks();
  }

  Future<void> _loadBookmarks() async {
    setState(() => _loadingBookmarks = true);
    final bookmarks = await BookmarkService.getAll();
    if (mounted) {
      setState(() {
        _bookmarks = bookmarks;
        _loadingBookmarks = false;
      });
    }
  }

  Future<void> _logout() async {
    await AuthService.logout();
    if (mounted) setState(() => _bookmarks = []);
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return ListView(
      physics: const BouncingScrollPhysics(),
      padding: EdgeInsets.fromLTRB(
          Tok.horizontalPadding, Tok.space3, Tok.horizontalPadding, 120),
      children: [
        FadeInUp(child: _buildHeader(p)),
        const SizedBox(height: Tok.space5),
        FadeInUp(
          delay: const Duration(milliseconds: 80),
          child: _buildUserCard(p),
        ),
        const SizedBox(height: Tok.space5),
        // 主题选择
        FadeInUp(
          delay: const Duration(milliseconds: 160),
          child: GlassCard(
            blur: false,
            padding: const EdgeInsets.all(Tok.space5),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('主题', style: Typo.h3(p.ink)),
                const SizedBox(height: Tok.space4),
                ThemePicker(notifier: themeNotifier),
              ],
            ),
          ),
        ),
        // 收藏列表
        if (AuthService.isLoggedIn) ...[
          const SizedBox(height: Tok.space5),
          FadeInUp(
            delay: const Duration(milliseconds: 240),
            child: _buildBookmarks(p),
          ),
        ],
        // 设置项
        const SizedBox(height: Tok.space5),
        FadeInUp(
          delay: const Duration(milliseconds: 320),
          child: _buildSettings(p),
        ),
      ],
    );
  }

  Widget _buildHeader(StarlorePalette p) {
    return Row(
      children: [
        Text('我的', style: Typo.h1(p.ink)),
        const Spacer(),
        if (AuthService.isLoggedIn)
          GestureDetector(
            onTap: _logout,
            child: Text('退出登录', style: Typo.caption(p.inkMuted)),
          ),
      ],
    );
  }

  Widget _buildUserCard(StarlorePalette p) {
    if (!AuthService.isLoggedIn) {
      return GlassCard(
        blur: false,
        glow: true,
        padding: const EdgeInsets.all(Tok.space5),
        child: Column(
          children: [
            Container(
              width: 64,
              height: 64,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: p.accentSoft,
              ),
              child: Icon(Icons.person_outline_rounded,
                  size: 32, color: p.accent),
            ),
            const SizedBox(height: Tok.space4),
            Text('登录解锁更多功能', style: Typo.h3(p.ink)),
            const SizedBox(height: Tok.space2),
            Text('收藏文章 · AI 对话 · 主题切换',
                style: Typo.bodySmall(p.inkMuted)),
            const SizedBox(height: Tok.space5),
            GestureDetector(
              onTap: () async {
                final result = await Navigator.push<bool>(
                  context,
                  MaterialPageRoute(builder: (_) => const LoginPage()),
                );
                if (result == true && mounted) {
                  setState(() {});
                  _loadBookmarks();
                }
              },
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(vertical: Tok.space3),
                decoration: BoxDecoration(
                  color: p.accent,
                  borderRadius: BorderRadius.circular(Tok.radiusFull),
                ),
                alignment: Alignment.center,
                child: Text('登录 / 注册',
                    style: Typo.label(
                        p.brightness == Brightness.dark
                            ? p.canvas
                            : Colors.white)),
              ),
            ),
          ],
        ),
      );
    }

    final user = AuthService.currentUser!;
    return GlassCard(
      blur: false,
      padding: const EdgeInsets.all(Tok.space5),
      child: Row(
        children: [
          // 头像
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: p.accentSoft,
              border: Border.all(
                  color: p.accent.withValues(alpha: 0.2), width: 1.5),
            ),
            child: user.avatar != null
                ? ClipOval(
                    child: Image.network(user.avatar!, fit: BoxFit.cover,
                        errorBuilder: (context, error, stackTrace) =>
                            Icon(Icons.person, size: 28, color: p.accent)))
                : Icon(Icons.person, size: 28, color: p.accent),
          ),
          const SizedBox(width: Tok.space4),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(user.displayName, style: Typo.h2(p.ink)),
                if (user.bio != null && user.bio!.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(user.bio!,
                        style: Typo.bodySmall(p.inkSoft),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBookmarks(StarlorePalette p) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text('我的收藏', style: Typo.h3(p.ink)),
            const Spacer(),
            Text('${_bookmarks.length} 篇', style: Typo.caption(p.inkMuted)),
          ],
        ),
        const SizedBox(height: Tok.space3),
        if (_loadingBookmarks)
          Center(
            child: Padding(
              padding: const EdgeInsets.all(Tok.space6),
              child: CircularProgressIndicator(
                  strokeWidth: 1.5, color: p.accent),
            ),
          )
        else if (_bookmarks.isEmpty)
          GlassCard(
            blur: false,
            child: Center(
              child: Padding(
                padding: const EdgeInsets.all(Tok.space6),
                child: Column(
                  children: [
                    Icon(Icons.bookmark_outline_rounded,
                        size: 36, color: p.inkMuted),
                    const SizedBox(height: Tok.space3),
                    Text('还没有收藏文章', style: Typo.bodySmall(p.inkMuted)),
                  ],
                ),
              ),
            ),
          )
        else
          ...List.generate(
            _bookmarks.length > 5 ? 5 : _bookmarks.length,
            (i) => ArticleCard(
              article: _bookmarks[i],
              onTap: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) =>
                      ArticleDetailPage(articleId: _bookmarks[i].id),
                ),
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildSettings(StarlorePalette p) {
    final items = [
      ('关于 Starlore', Icons.info_outline_rounded),
      ('隐私政策', Icons.shield_outlined),
      ('版本 1.0.0', Icons.code_rounded),
    ];

    return GlassCard(
      blur: false,
      padding: EdgeInsets.zero,
      child: Column(
        children: items.asMap().entries.map((e) {
          final i = e.key;
          final item = e.value;
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(
                    horizontal: Tok.space4, vertical: Tok.space4),
                child: Row(
                  children: [
                    Icon(item.$2, size: 18, color: p.inkSoft),
                    const SizedBox(width: Tok.space3),
                    Text(item.$1, style: Typo.body(p.ink)),
                    const Spacer(),
                    Icon(Icons.chevron_right_rounded,
                        size: 18, color: p.inkMuted),
                  ],
                ),
              ),
              if (i < items.length - 1)
                Divider(
                    height: 0.5,
                    indent: Tok.space4,
                    endIndent: Tok.space4,
                    color: p.border.withValues(alpha: 0.5)),
            ],
          );
        }).toList(),
      ),
    );
  }
}
