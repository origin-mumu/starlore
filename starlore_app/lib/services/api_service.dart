import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/article_model.dart';

class ApiService {
  static const String baseUrl = 'http://47.94.128.65:5000';
  static String? _token;

  static void setToken(String? token) => _token = token;
  static String? get token => _token;

  static Map<String, String> get _headers => {
    'Content-Type': 'application/json',
    if (_token != null) 'Authorization': 'Bearer $_token',
  };

  // ─── 认证 ───

  static Future<UserInfo?> getMe() async {
    try {
      final res = await http.get(
        Uri.parse('$baseUrl/api/auth/me'),
        headers: _headers,
      ).timeout(const Duration(seconds: 5));
      if (res.statusCode == 200) {
        final data = json.decode(res.body);
        return UserInfo.fromJson(data['data']);
      }
    } catch (_) {}
    return null;
  }

  static Future<Map<String, dynamic>?> login(String username, String password) async {
    try {
      final res = await http.post(
        Uri.parse('$baseUrl/api/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({'username': username, 'password': password}),
      ).timeout(const Duration(seconds: 10));
      final body = json.decode(res.body);
      if (res.statusCode == 200 && body['data']?['token'] != null) {
        return body;
      }
      return {'error': body['message'] ?? '登录失败'};
    } catch (e) {
      return {'error': '网络错误: $e'};
    }
  }

  static Future<Map<String, dynamic>?> register(String username, String password) async {
    try {
      final res = await http.post(
        Uri.parse('$baseUrl/api/auth/register'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({'username': username, 'password': password}),
      ).timeout(const Duration(seconds: 10));
      final body = json.decode(res.body);
      if ((res.statusCode == 200 || res.statusCode == 201) && body['data']?['token'] != null) {
        return body;
      }
      return {'error': body['message'] ?? '注册失败'};
    } catch (e) {
      return {'error': '网络错误: $e'};
    }
  }

  // ─── 文章 ───

  static Future<List<Article>> getArticles({int page = 1, int limit = 20, String? category}) async {
    try {
      var url = '$baseUrl/api/articles?page=$page&limit=$limit';
      if (category != null && category.isNotEmpty) url += '&category=${Uri.encodeComponent(category)}';

      final res = await http.get(
        Uri.parse(url),
        headers: _headers,
      ).timeout(const Duration(seconds: 8));

      if (res.statusCode == 200) {
        final data = json.decode(res.body);
        final list = data['data'] ?? [];
        return (list as List).map((e) => Article.fromJson(e)).toList();
      }
      if (res.statusCode == 401) return [];
    } catch (_) {}
    return [];
  }

  static Future<ArticleDetail?> getArticleDetail(int id) async {
    try {
      final res = await http.get(
        Uri.parse('$baseUrl/api/articles/$id'),
        headers: _headers,
      ).timeout(const Duration(seconds: 8));
      if (res.statusCode == 200) {
        final data = json.decode(res.body);
        return ArticleDetail.fromJson(data['data']);
      }
    } catch (_) {}
    return null;
  }

  // ─── 分类 ───

  static Future<List<Category>> getCategories() async {
    try {
      final res = await http.get(
        Uri.parse('$baseUrl/api/categories'),
        headers: _headers,
      ).timeout(const Duration(seconds: 8));
      if (res.statusCode == 200) {
        final data = json.decode(res.body);
        final list = data['data'] ?? [];
        return (list as List).map((e) => Category.fromJson(e)).toList();
      }
    } catch (_) {}
    return [];
  }

  // ─── 收藏 ───

  static Future<bool> checkBookmark(int articleId) async {
    try {
      final res = await http.get(
        Uri.parse('$baseUrl/api/bookmarks/check/$articleId'),
        headers: _headers,
      ).timeout(const Duration(seconds: 5));
      if (res.statusCode == 200) {
        final data = json.decode(res.body);
        return data['bookmarked'] == true;
      }
    } catch (_) {}
    return false;
  }

  static Future<bool> addBookmark(int articleId) async {
    try {
      final res = await http.post(
        Uri.parse('$baseUrl/api/bookmarks'),
        headers: _headers,
        body: json.encode({'articleId': articleId}),
      ).timeout(const Duration(seconds: 5));
      return res.statusCode == 200;
    } catch (_) {}
    return false;
  }

  static Future<bool> removeBookmark(int articleId) async {
    try {
      final res = await http.delete(
        Uri.parse('$baseUrl/api/bookmarks/$articleId'),
        headers: _headers,
      ).timeout(const Duration(seconds: 5));
      return res.statusCode == 200;
    } catch (_) {}
    return false;
  }

  static Future<List<Article>> getBookmarks() async {
    try {
      final res = await http.get(
        Uri.parse('$baseUrl/api/bookmarks'),
        headers: _headers,
      ).timeout(const Duration(seconds: 8));
      if (res.statusCode == 200) {
        final data = json.decode(res.body);
        final list = data['data'] ?? [];
        return (list as List).map((e) => Article.fromJson(e)).toList();
      }
    } catch (_) {}
    return [];
  }

  // ─── AI 对话（SSE 流式） ───

  static Stream<String> chatStream(String message, {String model = 'deepseek-v4-flash'}) async* {
    final history = json.encode([
      {'role': 'user', 'content': message},
    ]);

    final request = http.Request('GET', Uri.parse(
      '$baseUrl/api/ai/sse?model=$model&messages=${Uri.encodeComponent(history)}',
    ));
    request.headers.addAll(_headers);

    final client = http.Client();
    try {
      final response = await client.send(request);

      if (response.statusCode != 200) {
        // 读取错误信息
        final body = await response.stream.bytesToString();
        try {
          final err = json.decode(body);
          yield '\n\n[错误: ${err['message'] ?? body}]';
        } catch (_) {
          yield '\n\n[错误: HTTP ${response.statusCode}]';
        }
        return;
      }

      String buffer = '';
      await for (final chunk in response.stream.transform(utf8.decoder)) {
        buffer += chunk;
        final lines = buffer.split('\n');
        buffer = lines.removeLast();

        for (final line in lines) {
          final trimmed = line.trim();
          if (!trimmed.startsWith('data:')) continue;
          final data = trimmed.substring(5).trim();
          if (data == '[DONE]') continue;

          try {
            final parsed = json.decode(data);
            // 检查错误
            if (parsed['error'] != null) {
              yield '\n\n[错误: ${parsed['error']}]';
              return;
            }
            // 检查完成
            final done = parsed['done'];
            if (done == true || done == 'true') return;
            // 输出内容
            if (parsed['content'] != null && (parsed['content'] as String).isNotEmpty) {
              yield parsed['content'] as String;
            }
          } catch (_) {}
        }
      }
    } finally {
      client.close();
    }
  }

  // ─── 统计 ───

  static Future<Map<String, dynamic>?> getStats() async {
    try {
      final res = await http.get(
        Uri.parse('$baseUrl/api/articles/stats/summary'),
        headers: _headers,
      ).timeout(const Duration(seconds: 8));
      if (res.statusCode == 200) {
        return json.decode(res.body)['data'];
      }
    } catch (_) {}
    return null;
  }
}
