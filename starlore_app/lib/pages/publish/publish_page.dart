import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../theme/tokens.dart';
import '../../theme/typography.dart';
import '../../widgets/glass_card.dart';
import '../../widgets/fade_in_widget.dart';
import '../../widgets/empty_state.dart';
import '../../services/auth_service.dart';
import '../article/article_editor_page.dart';
import '../profile/login_page.dart';

/// 发布页 — 创作入口
class PublishPage extends StatelessWidget {
  const PublishPage({super.key});

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: Tok.horizontalPadding),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: Tok.space4),
          FadeInUp(
            child: Text('记录灵感', style: Typo.h1(p.ink)),
          ),
          const SizedBox(height: 2),
          FadeInUp(
            delay: const Duration(milliseconds: 50),
            child: Text('倾听内心的声音，记录当下的星语感悟', style: Typo.caption(p.inkMuted)),
          ),
          const SizedBox(height: Tok.space6),
          Expanded(
            child: AuthService.isLoggedIn
                ? ListView(
                    physics: const BouncingScrollPhysics(),
                    children: [
                      FadeInUp(
                        delay: const Duration(milliseconds: 100),
                        child: GlassCard(
                          glow: true,
                          padding: const EdgeInsets.all(Tok.space5),
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => const ArticleEditorPage(),
                              ),
                            );
                          },
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Container(
                                width: 48,
                                height: 48,
                                decoration: BoxDecoration(
                                  color: p.accentSoft,
                                  shape: BoxShape.circle,
                                ),
                                child: Icon(Icons.edit_note_rounded,
                                    size: 26, color: p.accent),
                              ),
                              const SizedBox(height: Tok.space4),
                              Text('撰写文章', style: Typo.h2(p.ink)),
                              const SizedBox(height: Tok.space2),
                              Text(
                                '创作属于你的星座运势、情感分析或心灵体验，与广大星友分享治愈力量。',
                                style: Typo.body(p.inkSoft),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  )
                : Center(
                    child: EmptyState(
                      icon: Icons.lock_outline_rounded,
                      title: '登录后即可创作',
                      subtitle: '登录解锁文章发布功能，分享你的星语感悟',
                      actionLabel: '登录 / 注册',
                      onAction: () => Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const LoginPage()),
                      ),
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}
