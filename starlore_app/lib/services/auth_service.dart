import 'package:shared_preferences/shared_preferences.dart';
import '../models/user.dart';
import 'api_client.dart';

/// 认证服务 — 登录/注册/Token 持久化
class AuthService {
  static const _tokenKey = 'auth_token';
  static UserInfo? _currentUser;

  static UserInfo? get currentUser => _currentUser;
  static bool get isLoggedIn => ApiClient.isAuthenticated && _currentUser != null;

  /// 初始化：从本地加载 Token 并验证
  static Future<bool> init() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString(_tokenKey);
    if (token == null || token.isEmpty) return false;

    ApiClient.setToken(token);
    final user = await getMe();
    if (user != null) {
      _currentUser = user;
      return true;
    }
    // Token 过期
    await _clearToken();
    return false;
  }

  /// 登录
  static Future<({bool ok, String? error})> login(
      String username, String password) async {
    final res = await ApiClient.post('/auth/login', body: {
      'username': username,
      'password': password,
    });

    if (res == null) return (ok: false, error: '网络错误，请检查网络连接');

    final data = res['data'] as Map<String, dynamic>?;
    if (data == null || data['token'] == null) {
      return (ok: false, error: res['message']?.toString() ?? '登录失败');
    }

    final token = data['token'] as String;
    await _saveToken(token);
    ApiClient.setToken(token);

    if (data['user'] != null) {
      _currentUser = UserInfo.fromJson(data['user'] as Map<String, dynamic>);
    } else {
      _currentUser = await getMe();
    }

    return (ok: true, error: null);
  }

  /// 注册
  static Future<({bool ok, String? error})> register(
      String username, String password) async {
    final res = await ApiClient.post('/auth/register', body: {
      'username': username,
      'password': password,
    });

    if (res == null) return (ok: false, error: '网络错误，请检查网络连接');

    final data = res['data'] as Map<String, dynamic>?;
    if (data == null || data['token'] == null) {
      return (ok: false, error: res['message']?.toString() ?? '注册失败');
    }

    final token = data['token'] as String;
    await _saveToken(token);
    ApiClient.setToken(token);

    if (data['user'] != null) {
      _currentUser = UserInfo.fromJson(data['user'] as Map<String, dynamic>);
    } else {
      _currentUser = await getMe();
    }

    return (ok: true, error: null);
  }

  /// 获取当前用户信息
  static Future<UserInfo?> getMe() async {
    final res = await ApiClient.get('/auth/me');
    if (res == null) return null;
    final data = res['data'];
    if (data == null) return null;
    return UserInfo.fromJson(data as Map<String, dynamic>);
  }

  /// 登出
  static Future<void> logout() async {
    _currentUser = null;
    await _clearToken();
  }

  static Future<void> _saveToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);
  }

  static Future<void> _clearToken() async {
    ApiClient.setToken(null);
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
  }
}
