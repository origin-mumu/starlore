import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../theme/app_theme.dart';
import '../services/api_service.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> with SingleTickerProviderStateMixin {
  final _userCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  bool _isLogin = true;
  bool _loading = false;
  String? _error;
  late AnimationController _c;
  late Animation<double> _fade;

  @override
  void initState() {
    super.initState();
    _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 600));
    _fade = CurvedAnimation(parent: _c, curve: Curves.easeOut);
    _c.forward();
  }

  @override
  void dispose() { _userCtrl.dispose(); _passCtrl.dispose(); _c.dispose(); super.dispose(); }

  Future<void> _submit() async {
    final u = _userCtrl.text.trim(), p = _passCtrl.text.trim();
    if (u.isEmpty || p.isEmpty) { setState(() => _error = '请填写完整信息'); return; }
    setState(() { _loading = true; _error = null; });
    HapticFeedback.lightImpact();

    final raw = _isLogin ? await ApiService.login(u, p) : await ApiService.register(u, p);
    if (raw == null) { setState(() { _error = '请求失败'; _loading = false; }); return; }
    if (raw.containsKey('error')) { setState(() { _error = raw['error'] as String? ?? '未知错误'; _loading = false; }); return; }

    final token = raw['data']?['token'] as String?;
    if (token != null) {
      ApiService.setToken(token);
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('auth_token', token);
      await prefs.setString('username', u);
      if (mounted) Navigator.of(context).pop(true);
    } else {
      setState(() { _error = '响应格式异常'; _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    return Scaffold(
      backgroundColor: p.surface0,
      body: Stack(
        children: [
          const SpaceBackground(),
          SafeArea(
            child: FadeTransition(
              opacity: _fade,
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(T.xxxl, 80, T.xxxl, T.xxxl),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Logo
                    Transform.rotate(
                      angle: 0.785398,
                      child: Container(
                        width: 40, height: 40,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(T.r8),
                          border: Border.all(color: p.accent.withValues(alpha: 0.3), width: 1),
                        ),
                        child: Transform.rotate(angle: -0.785398, child: Icon(Icons.auto_awesome_rounded, size: 16, color: p.accent)),
                      ),
                    ),
                    const SizedBox(height: T.xxl),
                    Text(_isLogin ? 'AUTHENTICATE' : 'REGISTER', style: F.h1(p.text0)),
                    const SizedBox(height: T.sm),
                    Text(_isLogin ? '// 输入凭据以访问系统' : '// 创建新用户档案', style: F.mono(p.text2)),
                    const SizedBox(height: T.xxxl),
                    _field(_userCtrl, 'USERNAME', p),
                    const SizedBox(height: T.md),
                    _field(_passCtrl, 'PASSWORD', p, obscure: true),
                    if (_error != null) ...[
                      const SizedBox(height: T.md),
                      Text('[!] $_error', style: F.mono(p.accent).copyWith(fontSize: 11)),
                    ],
                    const SizedBox(height: T.xxl),
                    GestureDetector(
                      onTap: _loading ? null : _submit,
                      child: Container(
                        width: double.infinity, height: 48,
                        decoration: BoxDecoration(
                          color: p.accent.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(T.r12),
                          border: Border.all(color: p.accent.withValues(alpha: 0.3), width: 0.5),
                        ),
                        child: Center(
                          child: _loading
                              ? SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 1, color: p.accent))
                              : Text(_isLogin ? 'LOGIN' : 'REGISTER', style: F.label(p.accent)),
                        ),
                      ),
                    ),
                    const SizedBox(height: T.xl),
                    Center(
                      child: GestureDetector(
                        onTap: () => setState(() { _isLogin = !_isLogin; _error = null; }),
                        child: Text(_isLogin ? '没有账号？注册' : '已有账号？登录', style: F.bodySmall(p.text1)),
                      ),
                    ),
                    const SizedBox(height: T.xxxl),
                    Center(
                      child: GestureDetector(
                        onTap: () => Navigator.of(context).pop(false),
                        child: Text('// SKIP', style: F.mono(p.text3)),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _field(TextEditingController ctrl, String label, StarlorePalette p, {bool obscure = false}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: F.mono(p.text2).copyWith(fontSize: 10)),
        const SizedBox(height: 6),
        Container(
          height: 48,
          decoration: BoxDecoration(
            color: p.surface1.withValues(alpha: 0.7),
            borderRadius: BorderRadius.circular(T.r12),
            border: Border.all(color: p.line, width: 0.5),
          ),
          child: TextField(
            controller: ctrl,
            obscureText: obscure,
            style: F.mono(p.text0).copyWith(fontSize: 14),
            decoration: InputDecoration(
              border: InputBorder.none, enabledBorder: InputBorder.none, focusedBorder: InputBorder.none,
              contentPadding: const EdgeInsets.symmetric(horizontal: 16),
            ),
          ),
        ),
      ],
    );
  }
}
