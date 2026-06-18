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
import '../article/article_editor_page.dart';
import 'login_page.dart';
import 'profile_edit_page.dart';

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
        // 发布文章
        if (AuthService.isLoggedIn) ...[
          const SizedBox(height: Tok.space5),
          FadeInUp(
            delay: const Duration(milliseconds: 280),
            child: _buildPublishButton(p),
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
          GestureDetector(
            onTap: () async {
              final result = await Navigator.push<bool>(
                context,
                MaterialPageRoute(builder: (_) => const ProfileEditPage()),
              );
              if (result == true && mounted) setState(() {});
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: p.surface,
                borderRadius: BorderRadius.circular(Tok.radiusFull),
                border: Border.all(
                    color: p.border.withValues(alpha: 0.5), width: 0.5),
              ),
              child: Text('编辑', style: Typo.caption(p.inkSoft)),
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

  Widget _buildPublishButton(StarlorePalette p) {
    return GestureDetector(
      onTap: () async {
        final result = await Navigator.push<bool>(
          context,
          MaterialPageRoute(builder: (_) => const ArticleEditorPage()),
        );
        if (result == true && mounted) setState(() {});
      },
      child: GlassCard(
        blur: false,
        padding: const EdgeInsets.all(Tok.space5),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.edit_note_rounded, size: 22, color: p.accent),
            const SizedBox(width: Tok.space3),
            Text('发布新文章', style: Typo.h3(p.accent)),
          ],
        ),
      ),
    );
  }

  Widget _buildSettings(StarlorePalette p) {
    return GlassCard(
      blur: false,
      padding: EdgeInsets.zero,
      child: Column(
        children: [
          _settingItem(p, '关于 Starlore', Icons.info_outline_rounded, () {
            _showAboutDialog(p);
          }),
          Divider(
              height: 0.5,
              indent: Tok.space4,
              endIndent: Tok.space4,
              color: p.border.withValues(alpha: 0.5)),
          _settingItem(p, '隐私政策', Icons.shield_outlined, () {
            _showSnackBar('隐私政策页面即将上线');
          }),
          Divider(
              height: 0.5,
              indent: Tok.space4,
              endIndent: Tok.space4,
              color: p.border.withValues(alpha: 0.5)),
          _settingItem(p, '版本 1.0.0', Icons.code_rounded, null),
        ],
      ),
    );
  }

  Widget _settingItem(StarlorePalette p, String title, IconData icon, VoidCallback? onTap) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Padding(
        padding: const EdgeInsets.symmetric(
            horizontal: Tok.space4, vertical: Tok.space4),
        child: Row(
          children: [
            Icon(icon, size: 18, color: p.inkSoft),
            const SizedBox(width: Tok.space3),
            Text(title, style: Typo.body(p.ink)),
            const Spacer(),
            if (onTap != null)
              Icon(Icons.chevron_right_rounded,
                  size: 18, color: p.inkMuted),
          ],
        ),
      ),
    );
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  void _showAboutDialog(StarlorePalette p) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: p.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(Tok.radiusLg),
        ),
        title: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(
                  colors: [p.accent, p.warm],
                ),
              ),
              child: Icon(Icons.auto_awesome_rounded, size: 20, color: Colors.white),
            ),
            const SizedBox(width: Tok.space3),
            Text('Starlore', style: Typo.h2(p.ink)),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('星语治愈 · 每日精选', style: Typo.body(p.inkSoft)),
            const SizedBox(height: Tok.space4),
            Text(
              'Starlore 是一款星座主题的内容平台，提供星座运势、情感分析、塔罗占卜等精选内容，搭配 AI 星语助手为你答疑解惑。',
              style: Typo.bodySmall(p.inkMuted),
            ),
            const SizedBox(height: Tok.space4),
            Text('版本 1.0.0', style: Typo.caption(p.inkMuted)),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: Text('知道了', style: Typo.label(p.accent)),
          ),
        ],
      ),
    );
  }
}
