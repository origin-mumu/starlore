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
