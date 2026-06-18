/// API 配置常量
class ApiConfig {
  ApiConfig._();

  /// 后端基础 URL
  static const String baseUrl = 'http://47.94.128.65:5000';

  /// API 前缀
  static const String apiPrefix = '/api';

  /// 完整 API 基础路径
  static String get apiBase => '$baseUrl$apiPrefix';

  /// 超时设置
  static const Duration timeout = Duration(seconds: 10);
  static const Duration longTimeout = Duration(seconds: 30);
  static const Duration sseTimeout = Duration(seconds: 120);
}
