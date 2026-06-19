import 'package:flutter/material.dart';

/// Starlore 色板 — 6 套主题，对齐 Web 端
class StarlorePalette {
  final String key;
  final String label;
  final Color dot;

  // 画布
  final Color canvas;
  final Color canvasDeep;

  // 表面
  final Color surface;
  final Color surfaceHover;

  // 文字
  final Color ink;
  final Color inkSoft;
  final Color inkMuted;

  // 强调
  final Color accent;
  final Color accentHover;
  final Color accentSoft;

  // 暖辅助
  final Color warm;
  final Color warmSoft;

  // 边框
  final Color border;
  final Color borderFocus;

  // 阴影
  final Color shadowCard;

  // 亮度模式
  final Brightness brightness;

  const StarlorePalette({
    required this.key,
    required this.label,
    required this.dot,
    required this.canvas,
    required this.canvasDeep,
    required this.surface,
    required this.surfaceHover,
    required this.ink,
    required this.inkSoft,
    required this.inkMuted,
    required this.accent,
    required this.accentHover,
    required this.accentSoft,
    required this.warm,
    required this.warmSoft,
    required this.border,
    required this.borderFocus,
    required this.shadowCard,
    required this.brightness,
  });
}

// ─── 默认：暖白 ───
const paletteDefault = StarlorePalette(
  key: 'default',
  label: '暖阳',
  dot: Color(0xFFE85D2A),
  canvas: Color(0xFFFFFCF7),
  canvasDeep: Color(0xFFF5EFE6),
  surface: Color(0xFFFFFEFC),
  surfaceHover: Color(0xFFFFF8F0),
  ink: Color(0xFF1A1410),
  inkSoft: Color(0xFF5C544A),
  inkMuted: Color(0xFF9C9488),
  accent: Color(0xFFE85D2A),
  accentHover: Color(0xFFD04E1F),
  accentSoft: Color(0xFFFFF0EA),
  warm: Color(0xFFD48C70),
  warmSoft: Color(0xFFFFF5F0),
  border: Color(0xFFEDE8E0),
  borderFocus: Color(0xFFE85D2A),
  shadowCard: Color(0x14B08860),
  brightness: Brightness.light,
);

// ─── 极简白 ───
const paletteWhite = StarlorePalette(
  key: 'white',
  label: '素白',
  dot: Color(0xFF333333),
  canvas: Color(0xFFF5F5F5),
  canvasDeep: Color(0xFFEBEBEB),
  surface: Color(0xFFFCFCFC),
  surfaceHover: Color(0xFFF2F2F2),
  ink: Color(0xFF1A1A1A),
  inkSoft: Color(0xFF666666),
  inkMuted: Color(0xFF999999),
  accent: Color(0xFF333333),
  accentHover: Color(0xFF1A1A1A),
  accentSoft: Color(0xFFF0F0F0),
  warm: Color(0xFF888888),
  warmSoft: Color(0xFFF5F5F5),
  border: Color(0xFFE0E0E0),
  borderFocus: Color(0xFF333333),
  shadowCard: Color(0x0F000000),
  brightness: Brightness.light,
);

// ─── 深空 ───
const paletteDark = StarlorePalette(
  key: 'dark',
  label: '深空',
  dot: Color(0xFF7B9AFF),
  canvas: Color(0xFF0F1117),
  canvasDeep: Color(0xFF080A0F),
  surface: Color(0xFF181C26),
  surfaceHover: Color(0xFF1E2330),
  ink: Color(0xFFF0F0F2),
  inkSoft: Color(0xFF9098AC),
  inkMuted: Color(0xFF505870),
  accent: Color(0xFF7B9AFF),
  accentHover: Color(0xFF95ADFF),
  accentSoft: Color(0xFF1A2040),
  warm: Color(0xFFA890CC),
  warmSoft: Color(0xFF1A1528),
  border: Color(0xFF252A38),
  borderFocus: Color(0xFF7B9AFF),
  shadowCard: Color(0x30000020),
  brightness: Brightness.dark,
);

// ─── 薄荷绿 ───
const paletteGreen = StarlorePalette(
  key: 'green',
  label: '薄荷',
  dot: Color(0xFF4A8C5C),
  canvas: Color(0xFFF0F7EE),
  canvasDeep: Color(0xFFE2EEE0),
  surface: Color(0xFFF8FCF7),
  surfaceHover: Color(0xFFEEF6EC),
  ink: Color(0xFF1A3A2D),
  inkSoft: Color(0xFF4A6858),
  inkMuted: Color(0xFF7A9888),
  accent: Color(0xFF4A8C5C),
  accentHover: Color(0xFF3A7A4C),
  accentSoft: Color(0xFFE8F5EC),
  warm: Color(0xFF8CB89A),
  warmSoft: Color(0xFFF0F8F2),
  border: Color(0xFFD4E8D8),
  borderFocus: Color(0xFF4A8C5C),
  shadowCard: Color(0x10306040),
  brightness: Brightness.light,
);

// ─── 浅海蓝 ───
const paletteBlue = StarlorePalette(
  key: 'blue',
  label: '浅海',
  dot: Color(0xFF3B7DD8),
  canvas: Color(0xFFEEF3F8),
  canvasDeep: Color(0xFFE0E8F0),
  surface: Color(0xFFF5F8FC),
  surfaceHover: Color(0xFFEAF0F8),
  ink: Color(0xFF1A2A3E),
  inkSoft: Color(0xFF4A6280),
  inkMuted: Color(0xFF7A92A8),
  accent: Color(0xFF3B7DD8),
  accentHover: Color(0xFF2B6DC8),
  accentSoft: Color(0xFFE4EFFA),
  warm: Color(0xFF7AAAD0),
  warmSoft: Color(0xFFF0F5FA),
  border: Color(0xFFD0DDE8),
  borderFocus: Color(0xFF3B7DD8),
  shadowCard: Color(0x10304060),
  brightness: Brightness.light,
);

// ─── 裸粉 ───
const palettePink = StarlorePalette(
  key: 'pink',
  label: '玫瑰',
  dot: Color(0xFFD4638F),
  canvas: Color(0xFFFDF2F6),
  canvasDeep: Color(0xFFF5E4EC),
  surface: Color(0xFFFFF8FA),
  surfaceHover: Color(0xFFFFF0F5),
  ink: Color(0xFF2D1B24),
  inkSoft: Color(0xFF6A4858),
  inkMuted: Color(0xFF9A7888),
  accent: Color(0xFFD4638F),
  accentHover: Color(0xFFC4537F),
  accentSoft: Color(0xFFFDE8F0),
  warm: Color(0xFFD49AB0),
  warmSoft: Color(0xFFFFF0F5),
  border: Color(0xFFEAD4DC),
  borderFocus: Color(0xFFD4638F),
  shadowCard: Color(0x10603048),
  brightness: Brightness.light,
);

/// 全部调色板列表
const allPalettes = [
  paletteDefault,
  paletteWhite,
  paletteDark,
];

/// 通过 key 查找
StarlorePalette paletteByKey(String key) =>
    allPalettes.firstWhere((p) => p.key == key, orElse: () => paletteDefault);
