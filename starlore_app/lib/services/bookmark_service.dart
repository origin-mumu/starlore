import '../models/article.dart';
import 'api_client.dart';

/// 收藏服务
class BookmarkService {
  /// 检查是否已收藏
  static Future<bool> isBookmarked(int articleId) async {
    final res = await ApiClient.get('/bookmarks/check/$articleId');
    if (res == null) return false;
    return res['bookmarked'] == true;
  }

  /// 添加收藏
  static Future<bool> add(int articleId) async {
    final res = await ApiClient.post('/bookmarks', body: {
      'articleId': articleId,
    });
    return res != null;
  }

  /// 取消收藏
  static Future<bool> remove(int articleId) async {
    final res = await ApiClient.delete('/bookmarks/$articleId');
    return res != null;
  }

  /// 获取收藏列表
  static Future<List<Article>> getAll() async {
    final res = await ApiClient.get('/bookmarks');
    if (res == null) return [];

    final list = res['data'];
    if (list is List) {
      return list
          .map((e) => Article.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  /// 切换收藏状态
  static Future<bool> toggle(int articleId) async {
    final bookmarked = await isBookmarked(articleId);
    if (bookmarked) {
      return await remove(articleId);
    } else {
      return await add(articleId);
    }
  }
}
