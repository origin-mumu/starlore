import 'package:flutter/material.dart';

/// Starelore 极简风格主题
class AppTheme {
  AppTheme._();

  // ==================== 配色方案 ====================
  static const Color bgBase = Color(0xFFF5F5F5);           // 浅灰背景
  static const Color bgCard = Color(0xFFFFFFFF);            // 白色卡片
  static const Color accentSage = Color(0xFF7A8B76);        // 鼠尾草绿
  static const Color accentTerracotta = Color(0xFFD48C70);  // 陶土橘
  static const Color accentSand = Color(0xFFC9B89C);        // 沙金
  static const Color accentLavender = Color(0xFF9B8AA0);    // 薰衣草紫
  static const Color textDark = Color(0xFF1A1A1A);          // 近黑文字
  static const Color textMedium = Color(0xFF666666);        // 中灰文字
  static const Color textLight = Color(0xFF999999);         // 浅灰文字
  static const Color white = Color(0xFFFFFFFF);

  // ==================== 圆角 ====================
  static const double radiusMD = 14.0;
  static const double radiusSM = 12.0;
  static const double radiusFull = 100.0;

  // ==================== 阴影 ====================
  static List<BoxShadow> get shadowSoft => [
    BoxShadow(
      color: Colors.black.withValues(alpha: 0.06),
      blurRadius: 12,
      offset: const Offset(0, 2),
    ),
  ];

  // ==================== 渐变 ====================
  static const LinearGradient gradientSage = LinearGradient(
    colors: [accentSage, Color(0xFF8FA38B)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient gradientTerracotta = LinearGradient(
    colors: [accentTerracotta, accentSand],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient gradientLavender = LinearGradient(
    colors: [accentLavender, Color(0xFFB5A3BA)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  // ==================== 文字样式 ====================
  static TextStyle headingLarge = const TextStyle(
    fontSize: 28,
    fontWeight: FontWeight.w700,
    color: textDark,
    letterSpacing: 0,
  );

  static TextStyle headingMedium = const TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.w700,
    color: textDark,
  );

  static TextStyle bodyLarge = const TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w600,
    color: textDark,
  );

  static TextStyle bodyMedium = const TextStyle(
    fontSize: 15,
    fontWeight: FontWeight.w400,
    color: textDark,
  );

  static TextStyle bodySmall = const TextStyle(
    fontSize: 13,
    fontWeight: FontWeight.w400,
    color: textLight,
  );

  static TextStyle caption = const TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: textLight,
  );

  // ==================== ThemeData ====================
  static ThemeData get theme => ThemeData(
    useMaterial3: true,
    scaffoldBackgroundColor: bgBase,
    colorScheme: ColorScheme.light(
      primary: accentSage,
      secondary: accentTerracotta,
      tertiary: accentLavender,
      surface: bgCard,
      onPrimary: white,
      onSecondary: white,
      onSurface: textDark,
    ),
    fontFamily: 'System',
    textTheme: const TextTheme(
      headlineLarge: TextStyle(fontSize: 28, fontWeight: FontWeight.w700, color: textDark),
      headlineMedium: TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: textDark),
      bodyLarge: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: textDark),
      bodyMedium: TextStyle(fontSize: 15, fontWeight: FontWeight.w400, color: textDark),
      bodySmall: TextStyle(fontSize: 13, fontWeight: FontWeight.w400, color: textLight),
      labelSmall: TextStyle(fontSize: 12, fontWeight: FontWeight.w400, color: textLight),
    ),
  );
}
