/// 文章摘要（列表用）
class Article {
  final int id;
  final String title;
  final String description;
  final String? coverImage;
  final String category;
  final String authorName;
  final DateTime createdAt;
  final int viewCount;
  final List<String> tags;

  const Article({
    required this.id,
    required this.title,
    required this.description,
    this.coverImage,
    required this.category,
    required this.authorName,
    required this.createdAt,
    this.viewCount = 0,
    this.tags = const [],
  });

  factory Article.fromJson(Map<String, dynamic> json) {
    return Article(
      id: json['id'] ?? 0,
      title: json['title'] ?? '',
      description: json['description'] ?? '',
      coverImage: json['cover_image'] ?? json['coverImage'],
      category: json['category'] ?? '未分类',
      authorName: json['authorName'] ?? json['author_name'] ?? '',
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'])
          : DateTime.now(),
      viewCount: json['view_count'] ?? json['viewCount'] ?? 0,
      tags: (json['tags'] as List?)?.map((e) => e.toString()).toList() ?? [],
    );
  }
}

/// 文章详情（含 content）
class ArticleDetail extends Article {
  final String content;

  const ArticleDetail({
    required super.id,
    required super.title,
    required super.description,
    super.coverImage,
    required super.category,
    required super.authorName,
    required super.createdAt,
    super.viewCount,
    super.tags,
    required this.content,
  });

  factory ArticleDetail.fromJson(Map<String, dynamic> json) {
    return ArticleDetail(
      id: json['id'] ?? 0,
      title: json['title'] ?? '',
      description: json['description'] ?? '',
      content: json['content'] ?? '',
      coverImage: json['cover_image'] ?? json['coverImage'],
      category: json['category'] ?? '未分类',
      authorName: json['authorName'] ?? json['author_name'] ?? '',
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'])
          : DateTime.now(),
      viewCount: json['view_count'] ?? json['viewCount'] ?? 0,
      tags: (json['tags'] as List?)?.map((e) => e.toString()).toList() ?? [],
    );
  }
}

/// 分类
class Category {
  final int id;
  final String name;
  final String? description;
  final String? color;
  final int articleCount;

  const Category({
    required this.id,
    required this.name,
    this.description,
    this.color,
    this.articleCount = 0,
  });

  factory Category.fromJson(Map<String, dynamic> json) {
    return Category(
      id: json['id'] ?? 0,
      name: json['name'] ?? '',
      description: json['description'],
      color: json['color'],
      articleCount: json['article_count'] ?? json['articleCount'] ?? 0,
    );
  }
}

/// AI 聊天消息
class ChatMessage {
  final String role;
  final String content;
  final DateTime createdAt;

  ChatMessage({
    required this.role,
    required this.content,
    DateTime? createdAt,
  }) : createdAt = createdAt ?? DateTime.now();
}

/// 用户信息
class UserInfo {
  final int id;
  final String username;
  final String? nickname;
  final String? email;
  final String? avatar;
  final String? bio;
  final String role;
  final int aiDailyLimit;
  final int aiTodayCount;

  const UserInfo({
    required this.id,
    required this.username,
    this.nickname,
    this.email,
    this.avatar,
    this.bio,
    this.role = 'user',
    this.aiDailyLimit = 10,
    this.aiTodayCount = 0,
  });

  factory UserInfo.fromJson(Map<String, dynamic> json) {
    return UserInfo(
      id: json['id'] ?? 0,
      username: json['username'] ?? '',
      nickname: json['nickname'],
      email: json['email'],
      avatar: json['avatar'],
      bio: json['bio'],
      role: json['role'] ?? 'user',
      aiDailyLimit: json['aiDailyLimit'] ?? 10,
      aiTodayCount: json['aiTodayCount'] ?? 0,
    );
  }
}
