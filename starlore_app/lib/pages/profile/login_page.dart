import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../theme/tokens.dart';
import '../../theme/typography.dart';
import '../../services/auth_service.dart';
import '../../widgets/glass_card.dart';
import '../../widgets/fade_in_widget.dart';

/// 登录 / 注册页
class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  bool _isLogin = true;
  final _usernameCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _loading = false;
  String? _error;
  bool _obscure = true;

  @override
  void dispose() {
    _usernameCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final username = _usernameCtrl.text.trim();
    final password = _passwordCtrl.text.trim();

    if (username.isEmpty || password.isEmpty) {
      setState(() => _error = '请填写用户名和密码');
      return;
    }
    if (password.length < 6) {
      setState(() => _error = '密码至少 6 位');
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
    });

    final result = _isLogin
        ? await AuthService.login(username, password)
        : await AuthService.register(username, password);

    if (mounted) {
      setState(() => _loading = false);
      if (result.ok) {
        Navigator.of(context).pop(true);
      } else {
        setState(() => _error = result.error);
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
          // 背景光球
          Positioned(
            right: -60,
            top: -40,
            child: Container(
              width: 240,
              height: 240,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [
                    p.accent.withValues(alpha: 0.1),
                    p.accent.withValues(alpha: 0),
                  ],
                ),
              ),
            ),
          ),
          SafeArea(
            child: Column(
              children: [
                // 顶部返回
                Padding(
                  padding: const EdgeInsets.symmetric(
                      horizontal: Tok.space3, vertical: Tok.space2),
                  child: Row(
                    children: [
                      IconButton(
                        onPressed: () => Navigator.of(context).pop(),
                        icon: Icon(Icons.arrow_back_ios_rounded,
                            size: 18, color: p.ink),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: SingleChildScrollView(
                    physics: const BouncingScrollPhysics(),
                    padding: const EdgeInsets.symmetric(
                        horizontal: Tok.horizontalPadding),
                    child: FadeInUp(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const SizedBox(height: Tok.space7),
                          // Logo
                          Container(
                            width: 56,
                            height: 56,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: p.accentSoft,
                            ),
                            child: Icon(Icons.auto_awesome_rounded,
                                size: 26, color: p.accent),
                          ),
                          const SizedBox(height: Tok.space5),
                          Text(
                            _isLogin ? '欢迎回来' : '创建账号',
                            style: Typo.display(p.ink),
                          ),
                          const SizedBox(height: Tok.space2),
                          Text(
                            _isLogin
                                ? '登录后享受完整的星语体验'
                                : '加入星语，开始你的星座之旅',
                            style: Typo.body(p.inkSoft),
                          ),
                          const SizedBox(height: Tok.space7),
                          // 表单
                          GlassCard(
                            blur: false,
                            padding: const EdgeInsets.all(Tok.space5),
                            child: Column(
                              children: [
                                _buildField(
                                  p,
                                  controller: _usernameCtrl,
                                  hint: '用户名',
                                  icon: Icons.person_outline_rounded,
                                ),
                                const SizedBox(height: Tok.space4),
                                _buildField(
                                  p,
                                  controller: _passwordCtrl,
                                  hint: '密码',
                                  icon: Icons.lock_outline_rounded,
                                  obscure: _obscure,
                                  suffixIcon: GestureDetector(
                                    onTap: () =>
                                        setState(() => _obscure = !_obscure),
                                    child: Icon(
                                      _obscure
                                          ? Icons.visibility_off_outlined
                                          : Icons.visibility_outlined,
                                      size: 18,
                                      color: p.inkMuted,
                                    ),
                                  ),
                                ),
                                if (_error != null) ...[
                                  const SizedBox(height: Tok.space3),
                                  Text(_error!,
                                      style: Typo.caption(
                                          const Color(0xFFD94040))),
                                ],
                                const SizedBox(height: Tok.space5),
                                // 提交按钮
                                GestureDetector(
                                  onTap: _loading ? null : _submit,
                                  child: AnimatedContainer(
                                    duration: Tok.fast,
                                    width: double.infinity,
                                    padding: const EdgeInsets.symmetric(
                                        vertical: Tok.space4),
                                    decoration: BoxDecoration(
                                      color: _loading
                                          ? p.accent.withValues(alpha: 0.6)
                                          : p.accent,
                                      borderRadius: BorderRadius.circular(
                                          Tok.radiusFull),
                                      boxShadow: [
                                        BoxShadow(
                                          color:
                                              p.accent.withValues(alpha: 0.2),
                                          blurRadius: 16,
                                          offset: const Offset(0, 4),
                                        ),
                                      ],
                                    ),
                                    alignment: Alignment.center,
                                    child: _loading
                                        ? SizedBox(
                                            width: 20,
                                            height: 20,
                                            child: CircularProgressIndicator(
                                              strokeWidth: 2,
                                              color:
                                                  p.brightness == Brightness.dark
                                                      ? p.canvas
                                                      : Colors.white,
                                            ),
                                          )
                                        : Text(
                                            _isLogin ? '登录' : '注册',
                                            style: Typo.label(
                                              p.brightness == Brightness.dark
                                                  ? p.canvas
                                                  : Colors.white,
                                            ),
                                          ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: Tok.space5),
                          // 切换登录/注册
                          Center(
                            child: GestureDetector(
                              onTap: () => setState(() {
                                _isLogin = !_isLogin;
                                _error = null;
                              }),
                              child: RichText(
                                text: TextSpan(
                                  style: Typo.bodySmall(p.inkMuted),
                                  children: [
                                    TextSpan(
                                        text: _isLogin
                                            ? '还没有账号？'
                                            : '已有账号？'),
                                    TextSpan(
                                      text: _isLogin ? '立即注册' : '去登录',
                                      style: Typo.bodySmall(p.accent)
                                          .copyWith(
                                              fontWeight: FontWeight.w600),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
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

  Widget _buildField(
    StarlorePalette p, {
    required TextEditingController controller,
    required String hint,
    required IconData icon,
    bool obscure = false,
    Widget? suffixIcon,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: p.canvasDeep.withValues(alpha: 0.6),
        borderRadius: BorderRadius.circular(Tok.radiusMd),
        border: Border.all(color: p.border.withValues(alpha: 0.5), width: 0.5),
      ),
      child: TextField(
        controller: controller,
        obscureText: obscure,
        style: Typo.body(p.ink),
        decoration: InputDecoration(
          hintText: hint,
          hintStyle: Typo.body(p.inkMuted),
          prefixIcon: Icon(icon, size: 18, color: p.inkMuted),
          suffixIcon: suffixIcon,
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(
              horizontal: Tok.space4, vertical: Tok.space4),
        ),
      ),
    );
  }
}
