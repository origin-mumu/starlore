import '../models/article.dart';
import '../models/category.dart';
import 'api_client.dart';

/// 文章 + 分类服务
class ArticleService {
  /// 获取公开文章列表
  static Future<List<Article>> getPublicArticles({
    int page = 1,
    int limit = 20,
    String? category,
  }) async {
    final params = <String, String>{
      'page': page.toString(),
      'limit': limit.toString(),
    };
    if (category != null && category.isNotEmpty) {
      params['category'] = category;
    }

    final res = await ApiClient.get('/public/articles', queryParams: params);
    if (res == null) return [];

    final list = res['data'];
    if (list is List) {
      return list
          .map((e) => Article.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  /// 获取文章详情（公开）
  static Future<ArticleDetail?> getPublicArticleDetail(int id) async {
    final res = await ApiClient.get('/public/articles/$id');
    if (res == null) return null;
    final data = res['data'];
    if (data == null) return null;
    return ArticleDetail.fromJson(data as Map<String, dynamic>);
  }

  /// 获取公开分类列表
  static Future<List<Category>> getPublicCategories() async {
    final res = await ApiClient.get('/public/categories');
    if (res == null) return [];

    final list = res['data'];
    if (list is List) {
      return list
          .map((e) => Category.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  /// 获取公开统计
  static Future<Map<String, dynamic>?> getPublicStats() async {
    return await ApiClient.get('/public/stats');
  }
}
