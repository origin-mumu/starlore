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
    AuthService.authState.addListener(_onAuthChange);
    _messages.add(ChatMessage(
      role: 'assistant',
      content: AuthService.isLoggedIn
          ? '你好 ✨ 我是星语助手，由 DeepSeek 云端 AI 驱动。你可以问我任何关于星座、运势、情感的问题～'
          : '你好 ✨ 我是星语助手！目前你在体验游客模式，回复为本地预设。登录后可享受 DeepSeek 云端 AI 的真实对话体验～',
    ));
  }

  void _onAuthChange() {
    if (mounted) {
      setState(() {
        if (_messages.isNotEmpty &&
            _messages[0].role == 'assistant' &&
            _messages.length == 1) {
          _messages[0] = ChatMessage(
            role: 'assistant',
            content: AuthService.isLoggedIn
                ? '你好 ✨ 我是星语助手，由 DeepSeek 云端 AI 驱动。你可以问我任何关于星座、运势、情感的问题～'
                : '你好 ✨ 我是星语助手！目前你在体验游客模式，回复为本地预设。登录后可享受 DeepSeek 云端 AI 的真实对话体验～',
          );
        }
      });
    }
  }

  @override
  void dispose() {
    AuthService.authState.removeListener(_onAuthChange);
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
    final bool isKeyboardOpen = MediaQuery.of(context).viewInsets.bottom > 0;

    return Column(
      children: [
        _buildHeader(p),
        Expanded(child: _buildMessages(p)),
        _buildInput(p, isKeyboardOpen),
        SizedBox(
          height: isKeyboardOpen
              ? Tok.space3
              : MediaQuery.of(context).padding.bottom +
                  Tok.navBarHeight +
                  Tok.navBarBottomPadding +
                  Tok.space2,
        ),
      ],
    );
  }

  Widget _buildHeader(StarlorePalette p) {
    return FadeInUp(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(
            Tok.space2, Tok.space3, Tok.horizontalPadding, Tok.space2),
        child: Row(
          children: [
            // 返回按钮（从悬浮入口 push 进入时使用）
            GestureDetector(
              onTap: () => Navigator.of(context).maybePop(),
              child: Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: p.surface,
                  borderRadius: BorderRadius.circular(Tok.radiusMd),
                  border: Border.all(color: p.border.withValues(alpha: 0.5), width: 0.5),
                ),
                child: Icon(Icons.arrow_back_ios_rounded, size: 16, color: p.ink),
              ),
            ),
            const SizedBox(width: Tok.space2),
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
    if (_messages.length == 1) {
      // 只有欢迎消息时显示空状态引导
      return _buildEmptyState(p);
    }
    return ListView.builder(
      controller: _scrollCtrl,
      physics: const BouncingScrollPhysics(),
      padding:
          const EdgeInsets.symmetric(horizontal: Tok.horizontalPadding, vertical: Tok.space3),
      itemCount: _messages.length,
      itemBuilder: (_, i) => _buildBubble(_messages[i], p),
    );
  }

  Widget _buildEmptyState(StarlorePalette p) {
    return ListView(
      physics: const BouncingScrollPhysics(),
      padding: const EdgeInsets.symmetric(
          horizontal: Tok.horizontalPadding, vertical: Tok.space3),
      children: [
        _buildBubble(_messages[0], p),
        const SizedBox(height: Tok.space5),
        // 快捷问题
        FadeInUp(
          delay: const Duration(milliseconds: 200),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('试试问我', style: Typo.caption(p.inkMuted)),
              const SizedBox(height: Tok.space3),
              ..._quickQuestions(p),
            ],
          ),
        ),
      ],
    );
  }

  List<Widget> _quickQuestions(StarlorePalette p) {
    final questions = [
      '今天的星座运势怎么样？',
      '白羊座的性格特点是什么？',
      '最近感情运如何？',
      '帮我分析一下塔罗牌',
    ];
    return questions.map((q) {
      return Padding(
        padding: const EdgeInsets.only(bottom: Tok.space2),
        child: GestureDetector(
          onTap: () {
            _inputCtrl.text = q;
            _send();
          },
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(
                horizontal: Tok.space4, vertical: Tok.space3),
            decoration: BoxDecoration(
              color: p.surface,
              borderRadius: BorderRadius.circular(Tok.radiusMd),
              border: Border.all(
                  color: p.border.withValues(alpha: 0.5), width: 0.5),
            ),
            child: Row(
              children: [
                Icon(Icons.auto_awesome_rounded, size: 14, color: p.accent),
                const SizedBox(width: Tok.space2),
                Expanded(
                  child: Text(q, style: Typo.bodySmall(p.inkSoft)),
                ),
                Icon(Icons.arrow_forward_ios_rounded,
                    size: 12, color: p.inkMuted),
              ],
            ),
          ),
        ),
      );
    }).toList();
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
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(
                  colors: [p.accent, p.warm],
                ),
              ),
              child: Icon(Icons.auto_awesome_rounded,
                  size: 16, color: Colors.white),
            ),
            const SizedBox(width: Tok.space2),
          ],
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(
                  horizontal: Tok.space4, vertical: Tok.space3),
              decoration: BoxDecoration(
                color: isUser
                    ? p.accent
                    : p.surface,
                borderRadius: BorderRadius.circular(Tok.radiusLg).copyWith(
                  bottomRight: isUser
                      ? const Radius.circular(4)
                      : null,
                  bottomLeft: !isUser
                      ? const Radius.circular(4)
                      : null,
                ),
                border: isUser
                    ? null
                    : Border.all(
                        color: p.border.withValues(alpha: 0.5),
                        width: 0.5,
                      ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    msg.content.isEmpty && msg.isStreaming ? '' : msg.content,
                    style: Typo.body(isUser
                        ? Colors.white
                        : p.ink),
                  ),
                  if (msg.isStreaming)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: _TypingIndicator(
                          color: isUser ? Colors.white : p.accent),
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

  Widget _buildInput(StarlorePalette p, bool isKeyboardOpen) {
    final isDark = p.brightness == Brightness.dark;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: Tok.horizontalPadding),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(Tok.radiusXl),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: Tok.blurMd, sigmaY: Tok.blurMd),
          child: Container(
            padding: const EdgeInsets.fromLTRB(Tok.space4, Tok.space2, Tok.space2, Tok.space2),
            decoration: BoxDecoration(
              color: isDark
                  ? p.surface.withValues(alpha: 0.65)
                  : p.surface.withValues(alpha: 0.85),
              borderRadius: BorderRadius.circular(Tok.radiusXl),
              border: Border.all(
                color: p.border.withValues(alpha: isDark ? 0.3 : 0.5),
                width: 0.5,
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: isDark ? 0.25 : 0.05),
                  blurRadius: 16,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Padding(
                  padding: const EdgeInsets.only(bottom: Tok.space2 + 2),
                  child: Icon(Icons.auto_awesome_rounded, size: 18, color: p.accent),
                ),
                const SizedBox(width: Tok.space2),
                Expanded(
                  child: TextField(
                    controller: _inputCtrl,
                    style: Typo.body(p.ink),
                    maxLines: 4,
                    minLines: 1,
                    keyboardType: TextInputType.multiline,
                    decoration: InputDecoration(
                      hintText: '输入你的问题...',
                      hintStyle: Typo.body(p.inkMuted),
                      border: InputBorder.none,
                      contentPadding: const EdgeInsets.symmetric(vertical: Tok.space2),
                    ),
                  ),
                ),
                const SizedBox(width: Tok.space2),
                GestureDetector(
                  onTap: _send,
                  child: AnimatedContainer(
                    duration: Tok.fast,
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: _sending
                          ? null
                          : LinearGradient(
                              colors: [p.accent, p.warm],
                            ),
                      color: _sending ? p.accent.withValues(alpha: 0.5) : null,
                    ),
                    child: const Icon(
                      Icons.arrow_upward_rounded,
                      size: 18,
                      color: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
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
