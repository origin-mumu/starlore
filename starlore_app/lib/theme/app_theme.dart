import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'palette.dart';
export 'palette.dart';

// ═══════════════════════════════════════════
// Theme Notifier — 状态管理
// ═══════════════════════════════════════════

class ThemeNotifier extends ValueNotifier<StarlorePalette> {
  ThemeNotifier() : super(paletteDefault);

  static const _storageKey = 'starlore_theme';

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final key = prefs.getString(_storageKey) ?? 'default';
    value = paletteByKey(key);
  }

  Future<void> setTheme(StarlorePalette palette) async {
    value = palette;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_storageKey, palette.key);
  }

  Future<void> setThemeByKey(String key) async {
    await setTheme(paletteByKey(key));
  }
}

// ═══════════════════════════════════════════
// InheritedWidget — 色板透传
// ═══════════════════════════════════════════

class _PaletteScope extends InheritedWidget {
  final StarlorePalette palette;
  const _PaletteScope({required this.palette, required super.child});

  @override
  bool updateShouldNotify(_PaletteScope old) => palette.key != old.palette.key;
}

class StarloreThemeProvider extends StatelessWidget {
  final ThemeNotifier notifier;
  final Widget child;

  const StarloreThemeProvider({
    super.key,
    required this.notifier,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<StarlorePalette>(
      valueListenable: notifier,
      builder: (_, palette, child) =>
          _PaletteScope(palette: palette, child: child!),
      child: child,
    );
  }
}

/// 快捷获取当前色板
StarlorePalette paletteOf(BuildContext context) =>
    context.dependOnInheritedWidgetOfExactType<_PaletteScope>()!.palette;

// ═══════════════════════════════════════════
// ThemeData 构建
// ═══════════════════════════════════════════

ThemeData buildThemeData(StarlorePalette p) {
  final isDark = p.brightness == Brightness.dark;

  return ThemeData(
    useMaterial3: true,
    brightness: p.brightness,
    colorScheme: ColorScheme(
      brightness: p.brightness,
      primary: p.accent,
      onPrimary: isDark ? p.canvas : Colors.white,
      secondary: p.warm,
      onSecondary: isDark ? p.canvas : Colors.white,
      surface: p.surface,
      onSurface: p.ink,
      error: const Color(0xFFD94040),
      onError: Colors.white,
    ),
    scaffoldBackgroundColor: p.canvas,
    appBarTheme: AppBarTheme(
      backgroundColor: Colors.transparent,
      foregroundColor: p.ink,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
    ),
    splashFactory: InkSparkle.splashFactory,
    dividerColor: p.border,
    cardColor: p.surface,
  );
}
