import 'dart:ui';
import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../theme/tokens.dart';
import '../../theme/typography.dart';
import '../../models/chat_message.dart';
import '../../services/ai_service.dart';
import '../../services/auth_service.dart';
import '../../widgets/fade_in_widget.dart';
import '../profile/login_page.dart';

/// AI 对话页
class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  final List<ChatMessage> _messages = [];
  final _inputCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    // 添加欢迎消息
    _messages.add(ChatMessage(
      role: 'assistant',
      content: AuthService.isLoggedIn
          ? '你好 ✨ 我是星语助手，由 DeepSeek 云端 AI 驱动。你可以问我任何关于星座、运势、情感的问题～'
          : '你好 ✨ 我是星语助手！目前你在体验游客模式，回复为本地预设。登录后可享受 DeepSeek 云端 AI 的真实对话体验～',
    ));
  }

  @override
  void dispose() {
    _inputCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    final text = _inputCtrl.text.trim();
    if (text.isEmpty || _sending) return;

    _inputCtrl.clear();
    setState(() {
      _messages.add(ChatMessage(role: 'user', content: text));
      _messages.add(ChatMessage(
          role: 'assistant', content: '', isStreaming: true));
      _sending = true;
    });
    _scrollToBottom();

    // 选择流式源
    final stream = AuthService.isLoggedIn
        ? AiService.chatStream(text)
        : AiService.mockChatStream(text);

    String accumulated = '';
    await for (final chunk in stream) {
      accumulated += chunk;
      if (mounted) {
        setState(() {
          _messages.last = _messages.last.copyWith(
            content: accumulated,
          );
        });
        _scrollToBottom();
      }
    }

    if (mounted) {
      setState(() {
        _messages.last = _messages.last.copyWith(isStreaming: false);
        _sending = false;
      });
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollCtrl.hasClients) {
        _scrollCtrl.animateTo(
          _scrollCtrl.position.maxScrollExtent,
          duration: Tok.normal,
          curve: Tok.easeOutQuart,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return Column(
      children: [
        _buildHeader(p),
        Expanded(child: _buildMessages(p)),
        _buildInput(p),
        SizedBox(
            height: MediaQuery.of(context).padding.bottom +
                Tok.navBarHeight +
                Tok.navBarBottomPadding),
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
            Text('星语助手', style: Typo.h1(p.ink)),
            const SizedBox(width: Tok.space2),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: AuthService.isLoggedIn
                    ? p.accentSoft
                    : p.warmSoft,
                borderRadius: BorderRadius.circular(Tok.radiusFull),
              ),
              child: Text(
                AuthService.isLoggedIn ? 'AI 在线' : '游客模式',
                style: Typo.caption(
                    AuthService.isLoggedIn ? p.accent : p.warm),
              ),
            ),
            const Spacer(),
            if (!AuthService.isLoggedIn)
              GestureDetector(
                onTap: () => Navigator.push(context,
                    MaterialPageRoute(builder: (_) => const LoginPage())),
                child: Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: p.accent,
                    borderRadius: BorderRadius.circular(Tok.radiusFull),
                  ),
                  child: Text('登录',
                      style: Typo.label(
                          p.brightness == Brightness.dark
                              ? p.canvas
                              : Colors.white)),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildMessages(StarlorePalette p) {
    return ListView.builder(
      controller: _scrollCtrl,
      physics: const BouncingScrollPhysics(),
      padding:
          const EdgeInsets.symmetric(horizontal: Tok.horizontalPadding, vertical: Tok.space3),
      itemCount: _messages.length,
      itemBuilder: (_, i) => _buildBubble(_messages[i], p),
    );
  }

  Widget _buildBubble(ChatMessage msg, p) {
    final isUser = msg.isUser;

    return Padding(
      padding: const EdgeInsets.only(bottom: Tok.space3),
      child: Row(
        mainAxisAlignment:
            isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!isUser) ...[
            // AI 头像
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: p.accentSoft,
              ),
              child: Icon(Icons.auto_awesome_rounded,
                  size: 16, color: p.accent),
            ),
            const SizedBox(width: Tok.space2),
          ],
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(
                  horizontal: Tok.space4, vertical: Tok.space3),
              decoration: BoxDecoration(
                color: isUser
                    ? p.accent.withValues(alpha: 0.1)
                    : p.surface,
                borderRadius: BorderRadius.circular(Tok.radiusLg).copyWith(
                  bottomRight: isUser
                      ? const Radius.circular(4)
                      : null,
                  bottomLeft: !isUser
                      ? const Radius.circular(4)
                      : null,
                ),
                border: Border.all(
                  color: isUser
                      ? p.accent.withValues(alpha: 0.15)
                      : p.border.withValues(alpha: 0.5),
                  width: 0.5,
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    msg.content.isEmpty && msg.isStreaming ? '...' : msg.content,
                    style: Typo.body(p.ink),
                  ),
                  if (msg.isStreaming)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: _TypingIndicator(color: p.accent),
                    ),
                ],
              ),
            ),
          ),
          if (isUser) const SizedBox(width: Tok.space2 + 32),
        ],
      ),
    );
  }

  Widget _buildInput(StarlorePalette p) {
    final isDark = p.brightness == Brightness.dark;

    return ClipRRect(
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: Tok.blurMd, sigmaY: Tok.blurMd),
        child: Container(
          padding: const EdgeInsets.fromLTRB(
              Tok.horizontalPadding, Tok.space3, Tok.horizontalPadding, Tok.space3),
          decoration: BoxDecoration(
            color: isDark
                ? p.surface.withValues(alpha: 0.7)
                : p.surface.withValues(alpha: 0.85),
            border: Border(
              top: BorderSide(color: p.border.withValues(alpha: 0.5), width: 0.5),
            ),
          ),
          child: Row(
            children: [
              Expanded(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: Tok.space4),
                  decoration: BoxDecoration(
                    color: p.canvasDeep.withValues(alpha: 0.6),
                    borderRadius: BorderRadius.circular(Tok.radiusFull),
                    border: Border.all(
                        color: p.border.withValues(alpha: 0.5), width: 0.5),
                  ),
                  child: TextField(
                    controller: _inputCtrl,
                    style: Typo.body(p.ink),
                    decoration: InputDecoration(
                      hintText: '输入你的问题...',
                      hintStyle: Typo.body(p.inkMuted),
                      border: InputBorder.none,
                      contentPadding:
                          const EdgeInsets.symmetric(vertical: Tok.space3),
                    ),
                    onSubmitted: (_) => _send(),
                  ),
                ),
              ),
              const SizedBox(width: Tok.space2),
              GestureDetector(
                onTap: _send,
                child: AnimatedContainer(
                  duration: Tok.fast,
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: _sending
                        ? p.accent.withValues(alpha: 0.5)
                        : p.accent,
                  ),
                  child: Icon(
                    Icons.arrow_upward_rounded,
                    size: 20,
                    color: isDark ? p.canvas : Colors.white,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 打字指示器动画
class _TypingIndicator extends StatefulWidget {
  final Color color;
  const _TypingIndicator({required this.color});

  @override
  State<_TypingIndicator> createState() => _TypingIndicatorState();
}

class _TypingIndicatorState extends State<_TypingIndicator>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _ctrl,
      builder: (context, child) {
        return Row(
          mainAxisSize: MainAxisSize.min,
          children: List.generate(3, (i) {
            final delay = i * 0.2;
            final t = ((_ctrl.value - delay) % 1.0).clamp(0.0, 1.0);
            final opacity = (1 - (t * 2 - 1).abs()).clamp(0.3, 1.0);
            return Container(
              margin: const EdgeInsets.symmetric(horizontal: 2),
              width: 5,
              height: 5,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: widget.color.withValues(alpha: opacity),
              ),
            );
          }),
        );
      },
    );
  }
}
