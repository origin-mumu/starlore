import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';

/// 统一 HTTP 客户端 — Token 管理 + 请求封装
class ApiClient {
  static String? _token;
  static final http.Client _client = http.Client();

  static void setToken(String? token) => _token = token;
  static String? get token => _token;
  static bool get isAuthenticated => _token != null && _token!.isNotEmpty;

  static Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        if (_token != null) 'Authorization': 'Bearer $_token',
      };

  // ─── GET ───
  static Future<Map<String, dynamic>?> get(
    String path, {
    Map<String, String>? queryParams,
    Duration? timeout,
  }) async {
    try {
      var uri = Uri.parse('${ApiConfig.apiBase}$path');
      if (queryParams != null) {
        uri = uri.replace(queryParameters: queryParams);
      }
      final res = await _client
          .get(uri, headers: _headers)
          .timeout(timeout ?? ApiConfig.timeout);
      return _handleResponse(res);
    } catch (e) {
      return null;
    }
  }

  // ─── POST ───
  static Future<Map<String, dynamic>?> post(
    String path, {
    Map<String, dynamic>? body,
    Duration? timeout,
  }) async {
    try {
      final res = await _client
          .post(
            Uri.parse('${ApiConfig.apiBase}$path'),
            headers: _headers,
            body: body != null ? json.encode(body) : null,
          )
          .timeout(timeout ?? ApiConfig.timeout);
      return _handleResponse(res);
    } catch (e) {
      return null;
    }
  }

  // ─── PUT ───
  static Future<Map<String, dynamic>?> put(
    String path, {
    Map<String, dynamic>? body,
  }) async {
    try {
      final res = await _client
          .put(
            Uri.parse('${ApiConfig.apiBase}$path'),
            headers: _headers,
            body: body != null ? json.encode(body) : null,
          )
          .timeout(ApiConfig.timeout);
      return _handleResponse(res);
    } catch (e) {
      return null;
    }
  }

  // ─── DELETE ───
  static Future<Map<String, dynamic>?> delete(String path) async {
    try {
      final res = await _client
          .delete(Uri.parse('${ApiConfig.apiBase}$path'), headers: _headers)
          .timeout(ApiConfig.timeout);
      return _handleResponse(res);
    } catch (e) {
      return null;
    }
  }

  // ─── SSE 流式请求 ───
  static Stream<String> sse(String path, {Map<String, dynamic>? body}) async* {
    final request = http.Request(
      body != null ? 'POST' : 'GET',
      Uri.parse('${ApiConfig.apiBase}$path'),
    );
    request.headers.addAll(_headers);
    if (body != null) {
      request.body = json.encode(body);
    }

    final client = http.Client();
    try {
      final response = await client.send(request);

      if (response.statusCode != 200) {
        final errorBody = await response.stream.bytesToString();
        try {
          final err = json.decode(errorBody);
          yield '\n\n[错误: ${err['message'] ?? errorBody}]';
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
            if (parsed['error'] != null) {
              yield '\n\n[错误: ${parsed['error']}]';
              return;
            }
            final done = parsed['done'];
            if (done == true || done == 'true') return;
            if (parsed['content'] != null &&
                (parsed['content'] as String).isNotEmpty) {
              yield parsed['content'] as String;
            }
          } catch (_) {}
        }
      }
    } finally {
      client.close();
    }
  }

  static Map<String, dynamic>? _handleResponse(http.Response res) {
    if (res.statusCode >= 200 && res.statusCode < 300) {
      if (res.body.isEmpty) return {};
      return json.decode(res.body) as Map<String, dynamic>;
    }
    if (res.statusCode == 401) {
      _token = null;
    }
    return null;
  }
}
