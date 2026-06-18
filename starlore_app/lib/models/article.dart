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
  final bool isPublic;

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
    this.isPublic = true,
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
          ? DateTime.tryParse(json['createdAt'].toString()) ?? DateTime.now()
          : DateTime.now(),
      viewCount: json['view_count'] ?? json['viewCount'] ?? 0,
      tags: (json['tags'] as List?)?.map((e) => e.toString()).toList() ?? [],
      isPublic: json['is_public'] ?? json['isPublic'] ?? true,
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
    super.isPublic,
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
          ? DateTime.tryParse(json['createdAt'].toString()) ?? DateTime.now()
          : DateTime.now(),
      viewCount: json['view_count'] ?? json['viewCount'] ?? 0,
      tags: (json['tags'] as List?)?.map((e) => e.toString()).toList() ?? [],
      isPublic: json['is_public'] ?? json['isPublic'] ?? true,
    );
  }
}
