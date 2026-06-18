/// HTML to Markdown converter utility for Starlore
/// Converts simple HTML tags produced by rich text editors (e.g. WangEditor) to Markdown format.
String convertHtmlToMarkdown(String html) {
  if (html.isEmpty) return '';

  String md = html;

  // 1. Remove doctype, html, head, body tags if present
  md = md.replaceAll(RegExp(r'<!DOCTYPE[^>]*>', caseSensitive: false), '');
  md = md.replaceAll(RegExp(r'</?(html|head|body|meta|link)[^>]*>', caseSensitive: false), '');

  // 2. Handle blockquotes
  md = md.replaceAllMapped(RegExp(r'<blockquote[^>]*>([\s\S]*?)<\/blockquote>', caseSensitive: false), (match) {
    final inner = match.group(1) ?? '';
    final lines = inner.split('\n').map((line) => '> $line').join('\n');
    return '\n$lines\n';
  });

  // 3. Handle code blocks (pre / code)
  md = md.replaceAllMapped(RegExp(r'<pre[^>]*><code[^>]*>([\s\S]*?)<\/code><\/pre>', caseSensitive: false), (match) {
    final code = match.group(1) ?? '';
    final decodedCode = code
        .replaceAll('&lt;', '<')
        .replaceAll('&gt;', '>')
        .replaceAll('&amp;', '&')
        .replaceAll('&quot;', '"')
        .replaceAll('&#39;', "'");
    return '\n```\n$decodedCode\n```\n';
  });
  
  md = md.replaceAllMapped(RegExp(r'<code[^>]*>([\s\S]*?)<\/code>', caseSensitive: false), (match) {
    final code = match.group(1) ?? '';
    final decodedCode = code
        .replaceAll('&lt;', '<')
        .replaceAll('&gt;', '>')
        .replaceAll('&amp;', '&')
        .replaceAll('&quot;', '"')
        .replaceAll('&#39;', "'");
    return ' `$decodedCode` ';
  });

  // 4. Handle Headings: h1 - h6
  for (int i = 1; i <= 6; i++) {
    final prefix = '#' * i;
    md = md.replaceAllMapped(RegExp('<h$i[^>]*>([\\s\\S]*?)<\\/h$i>', caseSensitive: false), (match) {
      final text = match.group(1) ?? '';
      return '\n\n$prefix ${text.trim()}\n\n';
    });
  }

  // 5. Handle lists (ul/ol/li)
  md = md.replaceAllMapped(RegExp(r'<ul[^>]*>([\s\S]*?)<\/ul>', caseSensitive: false), (match) {
    final listItems = match.group(1) ?? '';
    final converted = listItems.replaceAllMapped(RegExp(r'<li[^>]*>([\s\S]*?)<\/li>', caseSensitive: false), (liMatch) {
      final content = liMatch.group(1) ?? '';
      return '* ${content.trim()}\n';
    });
    return '\n\n$converted\n';
  });

  md = md.replaceAllMapped(RegExp(r'<ol[^>]*>([\s\S]*?)<\/ol>', caseSensitive: false), (match) {
    final listItems = match.group(1) ?? '';
    int index = 1;
    final converted = listItems.replaceAllMapped(RegExp(r'<li[^>]*>([\s\S]*?)<\/li>', caseSensitive: false), (liMatch) {
      final content = liMatch.group(1) ?? '';
      final res = '$index. ${content.trim()}\n';
      index++;
      return res;
    });
    return '\n\n$converted\n';
  });

  // 6. Handle paragraphs
  md = md.replaceAll(RegExp(r'<p[^>]*>', caseSensitive: false), '\n');
  md = md.replaceAll(RegExp(r'<\/p>', caseSensitive: false), '\n\n');

  // 7. Handle Line Breaks
  md = md.replaceAll(RegExp(r'<br\s*\/?>', caseSensitive: false), '\n');

  // 8. Handle strong / bold / em / italic
  md = md.replaceAllMapped(RegExp(r'<(strong|b)[^>]*>([\s\S]*?)<\/\1>', caseSensitive: false), (match) {
    final text = match.group(2) ?? '';
    return '**$text**';
  });
  md = md.replaceAllMapped(RegExp(r'<(em|i)[^>]*>([\s\S]*?)<\/\1>', caseSensitive: false), (match) {
    final text = match.group(2) ?? '';
    return '*$text*';
  });

  // 9. Handle links: <a href="url">text</a> => [text](url)
  md = md.replaceAllMapped(RegExp(r'<a\s+[^>]*href="([^"]+)"[^>]*>([\s\S]*?)<\/a>', caseSensitive: false), (match) {
    final url = match.group(1) ?? '';
    final text = match.group(2) ?? '';
    return '[$text]($url)';
  });

  // 10. Handle images: <img src="url" ...> => ![image](url)
  md = md.replaceAllMapped(RegExp(r'<img\s+[^>]*src="([^"]+)"[^>]*>', caseSensitive: false), (match) {
    final url = match.group(1) ?? '';
    return '\n![image]($url)\n';
  });

  // 11. Remove remaining tags
  md = md.replaceAll(RegExp(r'</?[a-zA-Z]+[^>]*>'), '');

  // 12. Unescape common HTML entities
  md = md
      .replaceAll('&amp;', '&')
      .replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>')
      .replaceAll('&quot;', '"')
      .replaceAll('&nbsp;', ' ')
      .replaceAll('&#39;', "'");

  // Clean up double newlines
  md = md.replaceAll(RegExp(r'\n{3,}'), '\n\n');

  return md.trim();
}
