import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../theme/app_theme.dart';
import '../services/api_service.dart';
import '../models/article_model.dart';

class ChatPage extends StatefulWidget {
  const ChatPage({super.key});

  @override
  State<ChatPage> createState() => _ChatPageState();
}

class _ChatPageState extends State<ChatPage> {
  final List<ChatMessage> _msgs = [];
  final _inputCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  bool _sending = false;

  @override
  void initState() { super.initState(); _loadHistory(); }
  @override
  void dispose() { _inputCtrl.dispose(); _scrollCtrl.dispose(); super.dispose(); }

  Future<void> _loadHistory() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString('chat_history');
    if (raw != null && mounted) {
      final list = json.decode(raw) as List;
      setState(() => _msgs.addAll(list.map((e) => ChatMessage(role: e['role'], content: e['content']))));
    }
  }

  Future<void> _saveHistory() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('chat_history', json.encode(_msgs.map((m) => {'role': m.role, 'content': m.content}).toList()));
  }

  void _scroll() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollCtrl.hasClients) _scrollCtrl.animateTo(_scrollCtrl.position.maxScrollExtent, duration: const Duration(milliseconds: 200), curve: Curves.easeOut);
    });
  }

  Future<void> _send(String text) async {
    if (text.trim().isEmpty || _sending) return;
    HapticFeedback.lightImpact();
    _inputCtrl.clear();
    setState(() { _msgs.add(ChatMessage(role: 'user', content: text.trim())); _msgs.add(ChatMessage(role: 'assistant', content: '')); _sending = true; });
    _scroll();

    try {
      await for (final chunk in ApiService.chatStream(text.trim())) {
        if (mounted) { setState(() => _msgs.last = ChatMessage(role: 'assistant', content: _msgs.last.content + chunk)); _scroll(); }
      }
    } catch (e) {
      if (mounted) setState(() => _msgs.last = ChatMessage(role: 'assistant', content: '[ERROR] $e'));
    }
    if (mounted) setState(() => _sending = false);
    _saveHistory();
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    return Column(
      children: [
        _header(p),
        Expanded(child: _msgs.isEmpty ? _empty(p) : _list(p)),
        _input(p),
      ],
    );
  }

  Widget _header(StarlorePalette p) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(T.xxl, T.md, T.xxl, T.sm),
      child: Row(
        children: [
          Container(
            width: 32, height: 32,
            decoration: BoxDecoration(border: Border.all(color: p.accent.withValues(alpha: 0.3), width: 0.5)),
            child: Icon(Icons.auto_awesome_rounded, size: 14, color: p.accent),
          ),
          const SizedBox(width: T.md),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('AI CORE', style: F.label(p.text0)),
              Row(children: [
                GlowDot(color: _sending ? p.accentGlow : p.accent, size: 4),
                const SizedBox(width: 6),
                Text(_sending ? 'PROCESSING' : 'READY', style: F.mono(_sending ? p.accentGlow : p.text2).copyWith(fontSize: 10)),
              ]),
            ],
          ),
        ],
      ),
    );
  }

  Widget _empty(StarlorePalette p) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 40),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 64, height: 64,
              decoration: BoxDecoration(border: Border.all(color: p.accent.withValues(alpha: 0.2), width: 0.5)),
              child: Icon(Icons.auto_awesome_rounded, size: 24, color: p.accent),
            ),
            const SizedBox(height: T.xl),
            Text('INITIALIZE QUERY', style: F.label(p.text0)),
            const SizedBox(height: T.sm),
            Text('// 输入指令开始交互', style: F.mono(p.text2)),
            const SizedBox(height: T.xxxl),
            Wrap(
              spacing: T.sm, runSpacing: T.sm, alignment: WrapAlignment.center,
              children: [
                _chip('今日运势', p),
                _chip('星座配对', p),
                _chip('塔罗占卜', p),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _chip(String label, StarlorePalette p) {
    return GestureDetector(
      onTap: () => _send(label),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: p.surface1.withValues(alpha: 0.7),
          borderRadius: BorderRadius.circular(T.rFull),
          border: Border.all(color: p.line, width: 0.5),
        ),
        child: Text(label, style: F.label(p.text1).copyWith(fontSize: 11)),
      ),
    );
  }

  Widget _list(StarlorePalette p) {
    return ListView.builder(
      controller: _scrollCtrl,
      padding: const EdgeInsets.fromLTRB(T.xxl, 0, T.xxl, T.lg),
      itemCount: _msgs.length,
      itemBuilder: (_, i) {
        final m = _msgs[i];
        return m.role == 'user' ? _userBubble(m.content, p) : _aiBubble(m.content, p, _sending && i == _msgs.length - 1);
      },
    );
  }

  Widget _userBubble(String text, StarlorePalette p) {
    return Align(
      alignment: Alignment.centerRight,
      child: Container(
        margin: const EdgeInsets.only(bottom: T.md),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.72),
        decoration: BoxDecoration(
          color: p.accent.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(T.r12),
          border: Border.all(color: p.accent.withValues(alpha: 0.2), width: 0.5),
        ),
        child: Text(text, style: F.body(p.text0)),
      ),
    );
  }

  Widget _aiBubble(String text, StarlorePalette p, bool streaming) {
    return Align(
      alignment: Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: T.md),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.72),
        decoration: BoxDecoration(
          color: p.surface1.withValues(alpha: 0.7),
          borderRadius: BorderRadius.circular(T.r12),
          border: Border.all(color: p.line, width: 0.5),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(text.isEmpty ? '...' : text, style: F.body(text.isEmpty ? p.text3 : p.text0)),
            if (streaming) Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Row(mainAxisSize: MainAxisSize.min, children: List.generate(3, (i) => _dot(p, i))),
            ),
          ],
        ),
      ),
    );
  }

  Widget _dot(StarlorePalette p, int i) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.2, end: 1.0),
      duration: Duration(milliseconds: 400 + i * 150),
      curve: Curves.easeInOut,
      builder: (_, v, _) => Container(
        margin: EdgeInsets.only(right: i < 2 ? 4 : 0),
        width: 4, height: 4,
        decoration: BoxDecoration(color: p.accent.withValues(alpha: v), shape: BoxShape.circle),
      ),
    );
  }

  Widget _input(StarlorePalette p) {
    final bottom = MediaQuery.of(context).padding.bottom;
    return Container(
      decoration: BoxDecoration(color: p.surface1, border: Border(top: BorderSide(color: p.line, width: 0.5))),
      padding: EdgeInsets.fromLTRB(T.xxl, T.md, T.xxl, bottom + 110),
      child: Row(
        children: [
          Expanded(
            child: Container(
              height: 44,
              decoration: BoxDecoration(
                color: p.surface0,
                borderRadius: BorderRadius.circular(T.rFull),
                border: Border.all(color: p.line, width: 0.5),
              ),
              child: TextField(
                controller: _inputCtrl,
                style: F.body(p.text0),
                decoration: InputDecoration(
                  hintText: '// 输入指令...',
                  hintStyle: F.mono(p.text3),
                  border: InputBorder.none, enabledBorder: InputBorder.none, focusedBorder: InputBorder.none,
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                ),
                onSubmitted: _send,
              ),
            ),
          ),
          const SizedBox(width: T.md),
          GestureDetector(
            onTap: () => _send(_inputCtrl.text),
            child: Container(
              width: 44, height: 44,
              decoration: BoxDecoration(
                color: _sending ? p.text3 : p.accent.withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(T.rFull),
                border: Border.all(color: _sending ? p.text3 : p.accent.withValues(alpha: 0.4), width: 0.5),
              ),
              child: _sending
                  ? SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 1, color: p.accent))
                  : Icon(Icons.arrow_upward_rounded, size: 18, color: p.accent),
            ),
          ),
        ],
      ),
    );
  }
}
