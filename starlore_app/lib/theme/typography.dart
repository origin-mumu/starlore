import 'package:flutter/material.dart';

/// 文字样式工厂 — 基于设计系统的排版阶梯
class Typo {
  Typo._();

  static const _fontFamily = 'PingFang SC';

  // ─── Display ───
  static TextStyle display(Color color) => TextStyle(
        fontFamily: _fontFamily,
        fontSize: 28,
        fontWeight: FontWeight.w800,
        height: 1.15,
        letterSpacing: -1.2,
        color: color,
      );

  // ─── H1 ───
  static TextStyle h1(Color color) => TextStyle(
        fontFamily: _fontFamily,
        fontSize: 22,
        fontWeight: FontWeight.w700,
        height: 1.25,
        letterSpacing: -0.6,
        color: color,
      );

  // ─── H2 ───
  static TextStyle h2(Color color) => TextStyle(
        fontFamily: _fontFamily,
        fontSize: 18,
        fontWeight: FontWeight.w600,
        height: 1.35,
        letterSpacing: -0.3,
        color: color,
      );

  // ─── H3 ───
  static TextStyle h3(Color color) => TextStyle(
        fontFamily: _fontFamily,
        fontSize: 16,
        fontWeight: FontWeight.w600,
        height: 1.4,
        color: color,
      );

  // ─── Body ───
  static TextStyle body(Color color) => TextStyle(
        fontFamily: _fontFamily,
        fontSize: 15,
        fontWeight: FontWeight.w400,
        height: 1.7,
        color: color,
      );

  // ─── Body Small ───
  static TextStyle bodySmall(Color color) => TextStyle(
        fontFamily: _fontFamily,
        fontSize: 13,
        fontWeight: FontWeight.w400,
        height: 1.6,
        color: color,
      );

  // ─── Caption ───
  static TextStyle caption(Color color) => TextStyle(
        fontFamily: _fontFamily,
        fontSize: 12,
        fontWeight: FontWeight.w400,
        height: 1.4,
        color: color,
      );

  // ─── Label (按钮 / 标签) ───
  static TextStyle label(Color color) => TextStyle(
        fontFamily: _fontFamily,
        fontSize: 13,
        fontWeight: FontWeight.w500,
        height: 1.3,
        letterSpacing: 0.2,
        color: color,
      );

  // ─── 数字 (mono) ───
  static TextStyle mono(Color color) => TextStyle(
        fontFamily: 'SF Mono',
        fontFamilyFallback: const ['Menlo', 'Consolas', 'monospace'],
        fontSize: 12,
        fontWeight: FontWeight.w400,
        height: 1.4,
        letterSpacing: 0.5,
        color: color,
      );
}
