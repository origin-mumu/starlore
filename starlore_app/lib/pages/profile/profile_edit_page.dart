import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../theme/tokens.dart';
import '../../theme/typography.dart';
import '../../services/auth_service.dart';
import '../../widgets/fade_in_widget.dart';

/// 个人信息编辑页
class ProfileEditPage extends StatefulWidget {
  const ProfileEditPage({super.key});

  @override
  State<ProfileEditPage> createState() => _ProfileEditPageState();
}

class _ProfileEditPageState extends State<ProfileEditPage> {
  final _nicknameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _bioCtrl = TextEditingController();
  final _locationCtrl = TextEditingController();
  final _websiteCtrl = TextEditingController();
  final _githubCtrl = TextEditingController();
  final _avatarCtrl = TextEditingController();
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _loadUserData();
  }

  void _loadUserData() {
    final user = AuthService.currentUser;
    if (user != null) {
      _nicknameCtrl.text = user.nickname ?? '';
      _emailCtrl.text = user.email ?? '';
      _bioCtrl.text = user.bio ?? '';
      _locationCtrl.text = user.location ?? '';
      _websiteCtrl.text = user.website ?? '';
      _githubCtrl.text = user.github ?? '';
      _avatarCtrl.text = user.avatar ?? '';
    }
  }

  @override
  void dispose() {
    _nicknameCtrl.dispose();
    _emailCtrl.dispose();
    _bioCtrl.dispose();
    _locationCtrl.dispose();
    _websiteCtrl.dispose();
    _githubCtrl.dispose();
    _avatarCtrl.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    setState(() => _saving = true);

    final result = await AuthService.updateProfile(
      nickname: _nicknameCtrl.text.trim(),
      email: _emailCtrl.text.trim(),
      bio: _bioCtrl.text.trim(),
      location: _locationCtrl.text.trim(),
      website: _websiteCtrl.text.trim(),
      github: _githubCtrl.text.trim(),
      avatar: _avatarCtrl.text.trim(),
    );

    if (mounted) {
      setState(() => _saving = false);
      if (result.ok) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('个人信息已更新')),
        );
        Navigator.of(context).pop(true);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result.error ?? '更新失败')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);

    return Scaffold(
      backgroundColor: p.canvas,
      body: Stack(
        children: [
          SafeArea(
            child: Column(
              children: [
                _buildHeader(p),
                Expanded(
                  child: SingleChildScrollView(
                    physics: const BouncingScrollPhysics(),
                    padding: const EdgeInsets.symmetric(
                        horizontal: Tok.horizontalPadding),
                    child: FadeInUp(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const SizedBox(height: Tok.space4),
                          // 头像预览
                          _buildAvatarPreview(p),
                          const SizedBox(height: Tok.space4),
                          // 头像 URL
                          _buildField(p,
                              controller: _avatarCtrl,
                              hint: '头像 URL',
                              icon: Icons.image_outlined),
                          const SizedBox(height: Tok.space4),
                          // 昵称
                          _buildField(p,
                              controller: _nicknameCtrl,
                              hint: '昵称',
                              icon: Icons.person_outline_rounded),
                          const SizedBox(height: Tok.space4),
                          // 邮箱
                          _buildField(p,
                              controller: _emailCtrl,
                              hint: '邮箱',
                              icon: Icons.email_outlined),
                          const SizedBox(height: Tok.space4),
                          // 个人简介
                          _buildField(p,
                              controller: _bioCtrl,
                              hint: '个人简介',
                              icon: Icons.info_outline_rounded,
                              maxLines: 3),
                          const SizedBox(height: Tok.space4),
                          // 所在地
                          _buildField(p,
                              controller: _locationCtrl,
                              hint: '所在地',
                              icon: Icons.location_on_outlined),
                          const SizedBox(height: Tok.space4),
                          // 网站
                          _buildField(p,
                              controller: _websiteCtrl,
                              hint: '个人网站',
                              icon: Icons.language_rounded),
                          const SizedBox(height: Tok.space4),
                          // GitHub
                          _buildField(p,
                              controller: _githubCtrl,
                              hint: 'GitHub 用户名',
                              icon: Icons.code_rounded),
                          const SizedBox(height: Tok.space8),
                        ],
                      ),
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

  Widget _buildHeader(StarlorePalette p) {
    return Padding(
      padding: const EdgeInsets.symmetric(
          horizontal: Tok.space3, vertical: Tok.space2),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.of(context).pop(),
            icon: Icon(Icons.arrow_back_ios_rounded, size: 18, color: p.ink),
          ),
          const Spacer(),
          Text('编辑个人信息', style: Typo.h2(p.ink)),
          const Spacer(),
          GestureDetector(
            onTap: _saving ? null : _save,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: _saving ? p.accent.withValues(alpha: 0.5) : p.accent,
                borderRadius: BorderRadius.circular(Tok.radiusFull),
              ),
              child: _saving
                  ? SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : Text('保存', style: Typo.label(Colors.white)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAvatarPreview(StarlorePalette p) {
    final avatarUrl = _avatarCtrl.text.trim();
    return Center(
      child: Container(
        width: 80,
        height: 80,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: LinearGradient(
            colors: [p.accent, p.warm],
          ),
          border: Border.all(
            color: p.accent.withValues(alpha: 0.2),
            width: 2,
          ),
        ),
        child: avatarUrl.isNotEmpty
            ? ClipOval(
                child: Image.network(
                  avatarUrl,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) =>
                      Icon(Icons.person, size: 40, color: Colors.white),
                ),
              )
            : Icon(Icons.person, size: 40, color: Colors.white),
      ),
    );
  }

  Widget _buildField(
    StarlorePalette p, {
    required TextEditingController controller,
    required String hint,
    required IconData icon,
    int maxLines = 1,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: p.surface,
        borderRadius: BorderRadius.circular(Tok.radiusMd),
        border: Border.all(color: p.border.withValues(alpha: 0.5), width: 0.5),
      ),
      child: TextField(
        controller: controller,
        maxLines: maxLines,
        style: Typo.body(p.ink),
        onChanged: (_) => setState(() {}), // 更新头像预览
        decoration: InputDecoration(
          hintText: hint,
          hintStyle: Typo.body(p.inkMuted),
          prefixIcon: Icon(icon, size: 18, color: p.inkMuted),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(
              horizontal: Tok.space4, vertical: Tok.space4),
        ),
      ),
    );
  }
}
