import 'package:flutter/widgets.dart';

/// Design Tokens — 间距、圆角、动效常量
class Tok {
  Tok._();

  // ─── 间距阶梯 ───
  static const double space1 = 4;
  static const double space2 = 8;
  static const double space3 = 12;
  static const double space4 = 16;
  static const double space5 = 24;
  static const double space6 = 32;
  static const double space7 = 48;
  static const double space8 = 64;

  // ─── 圆角 ───
  static const double radiusSm = 6;
  static const double radiusMd = 12;
  static const double radiusLg = 20;
  static const double radiusXl = 28;
  static const double radiusFull = 9999;

  // ─── 动效时长 ───
  static const Duration fast = Duration(milliseconds: 150);
  static const Duration normal = Duration(milliseconds: 250);
  static const Duration slow = Duration(milliseconds: 400);
  static const Duration entrance = Duration(milliseconds: 500);

  // ─── 曲线 ───
  static const Curve easeOutQuart = Cubic(0.25, 1, 0.5, 1);

  // ─── 模糊 ───
  static const double blurSm = 8;
  static const double blurMd = 16;
  static const double blurLg = 30;

  // ─── 导航栏 ───
  static const double navBarHeight = 64;
  static const double navBarRadius = 28;
  static const double navBarBottomPadding = 20;

  // ─── 内容 ───
  static const double contentMaxWidth = 420;
  static const double horizontalPadding = 20;
}
