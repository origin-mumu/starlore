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

  /// 获取当前用户的文章列表
  static Future<List<Article>> getMyArticles({
    int page = 1,
    int limit = 20,
  }) async {
    final params = <String, String>{
      'page': page.toString(),
      'limit': limit.toString(),
    };
    final res = await ApiClient.get('/articles', queryParams: params);
    if (res == null) return [];

    final list = res['data'];
    if (list is List) {
      return list
          .map((e) => Article.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  /// 创建文章
  static Future<({bool ok, String? error, int? id})> createArticle({
    required String title,
    required String content,
    String? description,
    String? coverImage,
    required String category,
    List<String>? tags,
    bool isPublic = true,
  }) async {
    final res = await ApiClient.post('/articles', body: {
      'title': title,
      'content': content,
      'description': description,
      'cover_image': coverImage,
      'category': category,
      'tags': tags,
      'isPublic': isPublic,
    });

    if (res == null) return (ok: false, error: '网络错误', id: null);

    final data = res['data'] as Map<String, dynamic>?;
    if (data == null) {
      return (ok: false, error: res['message']?.toString() ?? '发布失败', id: null);
    }

    return (ok: true, error: null, id: data['id'] as int?);
  }

  /// 更新文章
  static Future<({bool ok, String? error})> updateArticle({
    required int id,
    required String title,
    required String content,
    String? description,
    String? coverImage,
    required String category,
    List<String>? tags,
    bool isPublic = true,
  }) async {
    final res = await ApiClient.put('/articles/$id', body: {
      'title': title,
      'content': content,
      'description': description,
      'cover_image': coverImage,
      'category': category,
      'tags': tags,
      'isPublic': isPublic,
    });

    if (res == null) return (ok: false, error: '网络错误');
    return (ok: true, error: null);
  }

  /// 删除文章
  static Future<bool> deleteArticle(int id) async {
    final res = await ApiClient.delete('/articles/$id');
    return res != null;
  }
}
