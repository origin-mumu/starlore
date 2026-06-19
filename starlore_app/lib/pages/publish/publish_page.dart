import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../theme/tokens.dart';
import '../../theme/typography.dart';
import '../../widgets/glass_card.dart';
import '../../widgets/fade_in_widget.dart';
import '../article/article_editor_page.dart';

/// 发布页 — 替换原 AI 界面
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
            child: ListView(
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
                          child: Icon(Icons.edit_note_rounded, size: 26, color: p.accent),
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
                const SizedBox(height: Tok.space4),
                FadeInUp(
                  delay: const Duration(milliseconds: 150),
                  child: GlassCard(
                    glow: false,
                    padding: const EdgeInsets.all(Tok.space5),
                    onTap: () {
                      _showQuickInspirationDialog(context, p);
                    },
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          width: 48,
                          height: 48,
                          decoration: BoxDecoration(
                            color: p.warmSoft,
                            shape: BoxShape.circle,
                          ),
                          child: Icon(Icons.lightbulb_outline_rounded, size: 24, color: p.warm),
                        ),
                        const SizedBox(height: Tok.space4),
                        Text('捕捉闪光灵感', style: Typo.h2(p.ink)),
                        const SizedBox(height: Tok.space2),
                        Text(
                          '用简短的文字快速记下此刻突发的灵感、情绪波动，存为你的创作草稿。',
                          style: Typo.body(p.inkSoft),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _showQuickInspirationDialog(BuildContext context, StarlorePalette p) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: p.surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(Tok.radiusLg),
        ),
        title: Text('捕捉灵感', style: Typo.h2(p.ink)),
        content: TextField(
          controller: controller,
          maxLines: 3,
          style: Typo.body(p.ink),
          decoration: InputDecoration(
            hintText: '写下此刻的想法...',
            hintStyle: Typo.body(p.inkMuted),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(Tok.radiusMd),
              borderSide: BorderSide(color: p.border),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('取消', style: TextStyle(color: p.inkMuted)),
          ),
          ElevatedButton(
            onPressed: () {
              final txt = controller.text.trim();
              if (txt.isNotEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('灵感已存入草稿箱～')),
                );
              }
              Navigator.pop(context);
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: p.accent,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(Tok.radiusFull),
              ),
            ),
            child: const Text('保存', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }
}
