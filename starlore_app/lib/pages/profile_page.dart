import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../theme/app_theme.dart';
import '../main.dart';
import '../services/api_service.dart';
import '../models/article_model.dart';
import 'login_page.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  UserInfo? _user;

  @override
  void initState() { super.initState(); _loadUser(); }

  Future<void> _loadUser() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('auth_token');
    if (token != null) { ApiService.setToken(token); final u = await ApiService.getMe(); if (mounted) setState(() => _user = u); }
  }

  Future<void> _logout() async {
    final p = paletteOf(context);
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: p.surface1,
        shape: RoundedRectangleBorder(side: BorderSide(color: p.line, width: 0.5)),
        title: Text('CONFIRM', style: F.label(p.accent)),
        content: Text('确定退出登录？', style: F.body(p.text1)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text('CANCEL', style: F.label(p.text2))),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: Text('EXIT', style: F.label(p.accent))),
        ],
      ),
    );
    if (ok == true) {
      ApiService.setToken(null);
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('auth_token');
      await prefs.remove('username');
      if (mounted) setState(() => _user = null);
    }
  }

  Future<void> _login() async {
    final r = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => const LoginPage()));
    if (r == true) _loadUser();
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    return SingleChildScrollView(
      physics: const BouncingScrollPhysics(),
      padding: const EdgeInsets.only(bottom: 120),
      child: Column(
        children: [
          _header(p),
          if (_user != null) ...[const SizedBox(height: T.lg), _stats(p)],
          const SizedBox(height: T.xxl),
          _themeSwitcher(p),
          const SizedBox(height: T.xxl),
          _settings(p),
        ],
      ),
    );
  }

  Widget _header(StarlorePalette p) {
    final logged = _user != null;
    return SCard(
      margin: const EdgeInsets.fromLTRB(T.xxl, T.md, T.xxl, 0),
      padding: const EdgeInsets.all(T.lg),
      child: Row(
        children: [
          Container(
            width: 48, height: 48,
            decoration: BoxDecoration(border: Border.all(color: logged ? p.accent.withValues(alpha: 0.3) : p.lineStrong, width: 0.5)),
            child: Icon(logged ? Icons.person_rounded : Icons.auto_awesome_rounded, size: 20, color: logged ? p.accent : p.text2),
          ),
          const SizedBox(width: T.lg),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(logged ? (_user!.nickname ?? _user!.username).toUpperCase() : 'OFFLINE', style: F.label(p.text0)),
                Text(logged ? (_user!.bio ?? '// 用户档案已同步') : '// 未认证', style: F.mono(p.text2).copyWith(fontSize: 10)),
              ],
            ),
          ),
          GestureDetector(
            onTap: logged ? _logout : _login,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(T.rFull),
                border: Border.all(color: logged ? p.lineStrong : p.accent.withValues(alpha: 0.3), width: 0.5),
              ),
              child: Text(logged ? 'EXIT' : 'LOGIN', style: F.mono(logged ? p.text2 : p.accent).copyWith(fontSize: 10)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _stats(StarlorePalette p) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: T.xxl),
      child: Row(
        children: [
          _statItem('AI剩余', '${_user!.aiDailyLimit - _user!.aiTodayCount}', p.accent, p),
          const SizedBox(width: T.sm),
          _statItem('身份', _user!.role == 'admin' ? 'ADMIN' : 'USER', p.accentDim, p),
          const SizedBox(width: T.sm),
          _statItem('日限额', '${_user!.aiDailyLimit}', p.text1, p),
        ],
      ),
    );
  }

  Widget _statItem(String label, String value, Color color, StarlorePalette p) {
    return Expanded(
      child: SCard(
        padding: const EdgeInsets.symmetric(vertical: T.md, horizontal: T.sm),
        child: Column(
          children: [
            Text(label.toUpperCase(), style: F.mono(p.text3).copyWith(fontSize: 9)),
            const SizedBox(height: 4),
            Text(value, style: F.monoLg(color)),
          ],
        ),
      ),
    );
  }

  Widget _themeSwitcher(StarlorePalette current) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: T.xxl),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('THEME', style: F.mono(current.text2).copyWith(fontSize: 10)),
          const SizedBox(height: T.md),
          SCard(
            padding: const EdgeInsets.symmetric(horizontal: T.lg, vertical: T.lg),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: allPalettes.map((pal) {
                final active = current.name == pal.name;
                return GestureDetector(
                  onTap: () => themeNotifier.setTheme(pal),
                  child: Column(
                    children: [
                      AnimatedContainer(
                        duration: const Duration(milliseconds: 200),
                        width: active ? 32 : 24,
                        height: active ? 32 : 24,
                        decoration: BoxDecoration(
                          color: pal.dotColor.withValues(alpha: active ? 1 : 0.4),
                          border: Border.all(color: active ? pal.accent : pal.line, width: active ? 1.5 : 0.5),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(pal.label.toUpperCase(), style: F.mono(active ? pal.accent : pal.text3).copyWith(fontSize: 8)),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _settings(StarlorePalette p) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: T.xxl),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('SYSTEM', style: F.mono(p.text2).copyWith(fontSize: 10)),
          const SizedBox(height: T.md),
          SCard(
            padding: EdgeInsets.zero,
            child: Column(
              children: [
                _settingRow(Icons.bookmark_border_rounded, '收藏列表', p, onTap: () {
                  if (_user != null) {
                    Navigator.push(context, MaterialPageRoute(builder: (_) => _BookmarksPage(palette: p)));
                  } else {
                    _login();
                  }
                }),
                _settingRow(Icons.auto_awesome_rounded, 'AI 配额', p, subtitle: _user != null ? '${_user!.aiTodayCount}/${_user!.aiDailyLimit}' : '--'),
                _settingRow(Icons.edit_outlined, '编辑资料', p, onTap: () {
                  if (_user != null) {
                    Navigator.push(context, MaterialPageRoute(builder: (_) => _EditProfilePage(user: _user!, palette: p, onSaved: _loadUser)));
                  } else {
                    _login();
                  }
                }),
              ],
            ),
          ),
          const SizedBox(height: T.lg),
          SCard(
            padding: EdgeInsets.zero,
            child: _settingRow(Icons.info_outline_rounded, '关于 STARLORE', p, subtitle: 'v1.0.0'),
          ),
        ],
      ),
    );
  }

  Widget _settingRow(IconData icon, String title, StarlorePalette p, {String? subtitle, VoidCallback? onTap}) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: T.lg, vertical: T.md + 2),
          child: Row(
            children: [
              Icon(icon, size: 16, color: p.text2),
              const SizedBox(width: T.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: F.bodySmall(p.text0)),
                    if (subtitle != null) Text(subtitle, style: F.mono(p.text3).copyWith(fontSize: 9)),
                  ],
                ),
              ),
              Icon(Icons.chevron_right_rounded, size: 16, color: p.text3),
            ],
          ),
        ),
      ),
    );
  }
}

/// 收藏列表
class _BookmarksPage extends StatefulWidget {
  final StarlorePalette palette;
  const _BookmarksPage({required this.palette});
  @override
  State<_BookmarksPage> createState() => _BookmarksPageState();
}

class _BookmarksPageState extends State<_BookmarksPage> {
  List<Article> _list = [];
  bool _loading = true;
  @override
  void initState() { super.initState(); _load(); }
  Future<void> _load() async { final l = await ApiService.getBookmarks(); if (mounted) setState(() { _list = l; _loading = false; }); }

  @override
  Widget build(BuildContext context) {
    final p = widget.palette;
    return Scaffold(
      backgroundColor: p.surface0,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        title: Text('BOOKMARKS', style: F.label(p.text0)),
        leading: IconButton(icon: Icon(Icons.arrow_back_rounded, size: 16, color: p.text0), onPressed: () => Navigator.pop(context)),
      ),
      body: _loading
          ? Center(child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 1, color: p.accent)))
          : _list.isEmpty
              ? Center(child: Text('NO DATA', style: F.mono(p.text2)))
              : ListView.builder(
                  padding: const EdgeInsets.all(T.xxl),
                  itemCount: _list.length,
                  itemBuilder: (_, i) {
                    final a = _list[i];
                    return SCard(
                      margin: const EdgeInsets.only(bottom: T.sm),
                      padding: const EdgeInsets.all(T.lg),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text(a.title, style: F.h2(p.text0), maxLines: 2, overflow: TextOverflow.ellipsis),
                        const SizedBox(height: T.sm),
                        Text(a.description, style: F.bodySmall(p.text1), maxLines: 2, overflow: TextOverflow.ellipsis),
                      ]),
                    );
                  },
                ),
    );
  }
}

/// 编辑资料
class _EditProfilePage extends StatefulWidget {
  final UserInfo user; final StarlorePalette palette; final VoidCallback onSaved;
  const _EditProfilePage({required this.user, required this.palette, required this.onSaved});
  @override
  State<_EditProfilePage> createState() => _EditProfilePageState();
}

class _EditProfilePageState extends State<_EditProfilePage> {
  late TextEditingController _nickCtrl, _bioCtrl, _emailCtrl;
  bool _saving = false;

  @override
  void initState() { super.initState(); _nickCtrl = TextEditingController(text: widget.user.nickname ?? ''); _bioCtrl = TextEditingController(text: widget.user.bio ?? ''); _emailCtrl = TextEditingController(text: widget.user.email ?? ''); }
  @override
  void dispose() { _nickCtrl.dispose(); _bioCtrl.dispose(); _emailCtrl.dispose(); super.dispose(); }

  Future<void> _save() async {
    setState(() => _saving = true);
    try {
      final res = await http.put(
        Uri.parse('${ApiService.baseUrl}/api/auth/profile'),
        headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ${ApiService.token}'},
        body: json.encode({'nickname': _nickCtrl.text.trim(), 'bio': _bioCtrl.text.trim(), 'email': _emailCtrl.text.trim()}),
      ).timeout(const Duration(seconds: 10));
      if (res.statusCode == 200) { widget.onSaved(); if (mounted) { ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('SAVED', style: F.mono(Colors.white)), backgroundColor: widget.palette.accent)); Navigator.pop(context); } }
      else { final b = json.decode(res.body); if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(b['message'] ?? 'ERROR'), backgroundColor: widget.palette.accent)); }
    } catch (e) { if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('NETWORK ERROR'), backgroundColor: widget.palette.accent)); }
    if (mounted) setState(() => _saving = false);
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.palette;
    return Scaffold(
      backgroundColor: p.surface0,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        title: Text('EDIT PROFILE', style: F.label(p.text0)),
        leading: IconButton(icon: Icon(Icons.arrow_back_rounded, size: 16, color: p.text0), onPressed: () => Navigator.pop(context)),
        actions: [TextButton(onPressed: _saving ? null : _save, child: _saving ? SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 1, color: p.accent)) : Text('SAVE', style: F.label(p.accent)))],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(T.xxl),
        child: Column(children: [
          _field('NICKNAME', _nickCtrl, p), const SizedBox(height: T.lg),
          _field('BIO', _bioCtrl, p, maxLines: 3), const SizedBox(height: T.lg),
          _field('EMAIL', _emailCtrl, p),
        ]),
      ),
    );
  }

  Widget _field(String label, TextEditingController ctrl, StarlorePalette p, {int maxLines = 1}) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(label, style: F.mono(p.text2).copyWith(fontSize: 10)),
      const SizedBox(height: 6),
      Container(
        decoration: BoxDecoration(
          color: p.surface1.withValues(alpha: 0.7),
          borderRadius: BorderRadius.circular(T.r12),
          border: Border.all(color: p.line, width: 0.5),
        ),
        child: TextField(controller: ctrl, maxLines: maxLines, style: F.mono(p.text0).copyWith(fontSize: 14), decoration: InputDecoration(border: InputBorder.none, enabledBorder: InputBorder.none, focusedBorder: InputBorder.none, contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12))),
      ),
    ]);
  }
}
