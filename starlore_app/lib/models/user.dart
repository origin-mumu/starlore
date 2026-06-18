/// 用户信息
class UserInfo {
  final int id;
  final String username;
  final String? nickname;
  final String? email;
  final String? avatar;
  final String? bio;
  final String? location;
  final String? website;
  final String? github;
  final String role;

  const UserInfo({
    required this.id,
    required this.username,
    this.nickname,
    this.email,
    this.avatar,
    this.bio,
    this.location,
    this.website,
    this.github,
    this.role = 'user',
  });

  String get displayName => nickname?.isNotEmpty == true ? nickname! : username;

  factory UserInfo.fromJson(Map<String, dynamic> json) {
    return UserInfo(
      id: json['id'] ?? 0,
      username: json['username'] ?? '',
      nickname: json['nickname'],
      email: json['email'],
      avatar: json['avatar'],
      bio: json['bio'],
      location: json['location'],
      website: json['website'],
      github: json['github'],
      role: json['role'] ?? 'user',
    );
  }
}
