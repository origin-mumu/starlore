import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

// ═══════════════════════════════════════════
// 色板
// ═══════════════════════════════════════════

class StarlorePalette {
  final String name, label;
  final Color dotColor;
  final Color accent;        // 主强调色
  final Color accentDim;     // 淡化版
  final Color accentGlow;    // 发光版
  final Color surface0;      // 最底背景
  final Color surface1;      // 卡片背景
  final Color surface2;      // 浮起层
  final Color surface3;      // 最高层
  final Color text0;         // 主文字
  final Color text1;         // 次文字
  final Color text2;         // 辅助文字
  final Color text3;         // 最淡文字
  final Color line;          // 线条
  final Color lineStrong;    // 强线条

  const StarlorePalette({
    required this.name, required this.label, required this.dotColor,
    required this.accent, required this.accentDim, required this.accentGlow,
    required this.surface0, required this.surface1, required this.surface2, required this.surface3,
    required this.text0, required this.text1, required this.text2, required this.text3,
    required this.line, required this.lineStrong,
  });
}

/// 深空青 — 冷峻科技
const _void = StarlorePalette(
  name: 'void',
  label: '深空',
  dotColor: Color(0xFF00E5CC),
  accent: Color(0xFF00E5CC),
  accentDim: Color(0xFF00E5CC),
  accentGlow: Color(0xFF00FFE0),
  surface0: Color(0xFF04060C),
  surface1: Color(0xFF0A0E18),
  surface2: Color(0xFF101624),
  surface3: Color(0xFF182030),
  text0: Color(0xFFE8ECF4),
  text1: Color(0xFF8892A8),
  text2: Color(0xFF4A5268),
  text3: Color(0xFF252A38),
  line: Color(0xFF1A2030),
  lineStrong: Color(0xFF2A3448),
);

/// 暗焰 — 深红科技
const _ember = StarlorePalette(
  name: 'ember',
  label: '暗焰',
  dotColor: Color(0xFFFF4D4D),
  accent: Color(0xFFFF4D4D),
  accentDim: Color(0xFF661A1A),
  accentGlow: Color(0xFFFF6666),
  surface0: Color(0xFF080404),
  surface1: Color(0xFF120A0A),
  surface2: Color(0xFF1C1010),
  surface3: Color(0xFF261818),
  text0: Color(0xFFF0E8E8),
  text1: Color(0xFFA88888),
  text2: Color(0xFF684848),
  text3: Color(0xFF382424),
  line: Color(0xFF2A1818),
  lineStrong: Color(0xFF482828),
);

/// 星尘紫 — 深紫科技
const _nebula = StarlorePalette(
  name: 'nebula',
  label: '星尘',
  dotColor: Color(0xFF8B5CF6),
  accent: Color(0xFF8B5CF6),
  accentDim: Color(0xFF3B1F66),
  accentGlow: Color(0xFFA78BFA),
  surface0: Color(0xFF06040C),
  surface1: Color(0xFF0E0A18),
  surface2: Color(0xFF161024),
  surface3: Color(0xFF201830),
  text0: Color(0xFFEAE8F4),
  text1: Color(0xFF9488A8),
  text2: Color(0xFF584868),
  text3: Color(0xFF2C2438),
  line: Color(0xFF221830),
  lineStrong: Color(0xFF382848),
);

/// 极光 — 冰蓝科技
const _frost = StarlorePalette(
  name: 'frost',
  label: '极光',
  dotColor: Color(0xFF38BDF8),
  accent: Color(0xFF38BDF8),
  accentDim: Color(0xFF1A4A66),
  accentGlow: Color(0xFF7DD3FC),
  surface0: Color(0xFF04080C),
  surface1: Color(0xFF0A1018),
  surface2: Color(0xFF101824),
  surface3: Color(0xFF182030),
  text0: Color(0xFFE8F0F8),
  text1: Color(0xFF8898A8),
  text2: Color(0xFF485868),
  text3: Color(0xFF242C38),
  line: Color(0xFF182838),
  lineStrong: Color(0xFF283848),
);

/// 暖金 — 深空金
const _aureum = StarlorePalette(
  name: 'aureum',
  label: '暖金',
  dotColor: Color(0xFFD4A853),
  accent: Color(0xFFD4A853),
  accentDim: Color(0xFF5A4420),
  accentGlow: Color(0xFFE8C070),
  surface0: Color(0xFF080604),
  surface1: Color(0xFF12100A),
  surface2: Color(0xFF1C1A10),
  surface3: Color(0xFF262418),
  text0: Color(0xFFF0ECE0),
  text1: Color(0xFFA89878),
  text2: Color(0xFF686048),
  text3: Color(0xFF383020),
  line: Color(0xFF282018),
  lineStrong: Color(0xFF483828),
);

const allPalettes = [_void, _ember, _nebula, _frost, _aureum];

// ═══════════════════════════════════════════
// 状态管理
// ═══════════════════════════════════════════

class ThemeNotifier extends ValueNotifier<StarlorePalette> {
  ThemeNotifier() : super(_void);

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final name = prefs.getString('starlore_theme') ?? 'void';
    value = allPalettes.firstWhere((p) => p.name == name, orElse: () => _void);
  }

  Future<void> setTheme(StarlorePalette palette) async {
    value = palette;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('starlore_theme', palette.name);
  }
}

StarlorePalette paletteOf(BuildContext context) =>
    context.dependOnInheritedWidgetOfExactType<_ThemeScope>()!.palette;

class _ThemeScope extends InheritedWidget {
  final StarlorePalette palette;
  const _ThemeScope({required this.palette, required super.child});
  @override
  bool updateShouldNotify(_ThemeScope old) => palette != old.palette;
}

class StarloreTheme extends StatelessWidget {
  final ThemeNotifier notifier;
  final Widget child;
  const StarloreTheme({super.key, required this.notifier, required this.child});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<StarlorePalette>(
      valueListenable: notifier,
      builder: (_, p, child) => _ThemeScope(palette: p, child: child!),
      child: child,
    );
  }
}

// ═══════════════════════════════════════════
// 设计 Token
// ═══════════════════════════════════════════

class T {
  // 间距
  static const xs = 4.0, sm = 8.0, md = 12.0, lg = 16.0, xl = 20.0, xxl = 24.0, xxxl = 32.0;
  // 圆角
  static const r4 = 4.0, r8 = 8.0, r12 = 12.0, r16 = 16.0, r20 = 20.0, r24 = 24.0, rFull = 100.0;
}

/// 文字样式 — 等宽数字、紧凑行高
class F {
  static TextStyle display(Color c) => TextStyle(fontSize: 32, fontWeight: FontWeight.w200, height: 1.1, letterSpacing: 6, color: c);
  static TextStyle h1(Color c) => TextStyle(fontSize: 24, fontWeight: FontWeight.w300, height: 1.2, letterSpacing: 3, color: c);
  static TextStyle h2(Color c) => TextStyle(fontSize: 18, fontWeight: FontWeight.w400, height: 1.3, letterSpacing: 1, color: c);
  static TextStyle body(Color c) => TextStyle(fontSize: 14, fontWeight: FontWeight.w400, height: 1.6, color: c);
  static TextStyle bodySmall(Color c) => TextStyle(fontSize: 13, fontWeight: FontWeight.w400, height: 1.5, color: c);
  static TextStyle caption(Color c) => TextStyle(fontSize: 11, fontWeight: FontWeight.w400, height: 1.4, letterSpacing: 0.5, color: c);
  static TextStyle label(Color c) => TextStyle(fontSize: 12, fontWeight: FontWeight.w500, height: 1.3, letterSpacing: 1, color: c);
  static TextStyle mono(Color c) => TextStyle(fontSize: 12, fontWeight: FontWeight.w400, height: 1.4, fontFamily: 'monospace', letterSpacing: 1.5, color: c);
  static TextStyle monoLg(Color c) => TextStyle(fontSize: 16, fontWeight: FontWeight.w300, height: 1.3, fontFamily: 'monospace', letterSpacing: 2, color: c);
}

// ═══════════════════════════════════════════
// 组件
// ═══════════════════════════════════════════

/// 细线分割线
class ThinLine extends StatelessWidget {
  final double height;
  const ThinLine({super.key, this.height = 1});
  @override
  Widget build(BuildContext context) => Container(height: height, color: paletteOf(context).line);
}

/// 发光细线
class GlowLine extends StatelessWidget {
  const GlowLine({super.key});
  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    return Container(
      height: 1,
      decoration: BoxDecoration(
        gradient: LinearGradient(colors: [p.accent.withValues(alpha: 0.4), p.accent.withValues(alpha: 0)]),
      ),
    );
  }
}

/// 深空背景层
class SpaceBackground extends StatelessWidget {
  const SpaceBackground({super.key});
  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    return Positioned.fill(
      child: Container(
        decoration: BoxDecoration(
          gradient: RadialGradient(
            center: const Alignment(0.3, -0.6),
            radius: 1.2,
            colors: [p.accent.withValues(alpha: 0.04), p.surface0],
          ),
        ),
      ),
    );
  }
}

/// 发光点
class GlowDot extends StatelessWidget {
  final Color color;
  final double size;
  const GlowDot({super.key, required this.color, this.size = 4});
  @override
  Widget build(BuildContext context) {
    return Container(
      width: size, height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: color,
        boxShadow: [BoxShadow(color: color.withValues(alpha: 0.6), blurRadius: size * 2, spreadRadius: size * 0.5)],
      ),
    );
  }
}

/// 圆角毛玻璃卡片
class SCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding, margin;
  final bool glow;
  final double radius;
  const SCard({super.key, required this.child, this.padding, this.margin, this.glow = false, this.radius = T.r16});

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    return Container(
      margin: margin,
      padding: padding ?? const EdgeInsets.all(T.xl),
      decoration: BoxDecoration(
        color: p.surface1.withValues(alpha: 0.8),
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(color: glow ? p.accent.withValues(alpha: 0.2) : p.line, width: 0.5),
        boxShadow: glow ? [BoxShadow(color: p.accent.withValues(alpha: 0.06), blurRadius: 24, offset: const Offset(0, 8))] : null,
      ),
      child: child,
    );
  }
}

/// 毛玻璃容器（带模糊）
class FrostedCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding, margin;
  final bool glow;
  final double radius;
  const FrostedCard({super.key, required this.child, this.padding, this.margin, this.glow = false, this.radius = T.r16});

  @override
  Widget build(BuildContext context) {
    final p = paletteOf(context);
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
        child: Container(
          margin: margin,
          padding: padding ?? const EdgeInsets.all(T.xl),
          decoration: BoxDecoration(
            color: p.surface1.withValues(alpha: 0.5),
            borderRadius: BorderRadius.circular(radius),
            border: Border.all(color: glow ? p.accent.withValues(alpha: 0.2) : p.line.withValues(alpha: 0.6), width: 0.5),
            boxShadow: glow ? [BoxShadow(color: p.accent.withValues(alpha: 0.08), blurRadius: 32, offset: const Offset(0, 8))] : null,
          ),
          child: child,
        ),
      ),
    );
  }
}

/// 状态指示器
class StatusBadge extends StatelessWidget {
  final String text;
  final Color color;
  const StatusBadge({super.key, required this.text, required this.color});
  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        GlowDot(color: color, size: 4),
        const SizedBox(width: 6),
        Text(text, style: F.mono(color)),
      ],
    );
  }
}

ThemeData buildAppTheme(StarlorePalette p) {
  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    colorScheme: ColorScheme.dark(
      primary: p.accent,
      surface: p.surface0,
      onSurface: p.text0,
      error: const Color(0xFFFF4D4D),
    ),
    scaffoldBackgroundColor: p.surface0,
    appBarTheme: AppBarTheme(
      backgroundColor: Colors.transparent,
      foregroundColor: p.text0,
      elevation: 0,
      scrolledUnderElevation: 0,
    ),
  );
}
