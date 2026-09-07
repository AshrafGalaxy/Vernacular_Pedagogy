import os
import markdown

def generate_html():
    md_path = 'PALASH_SETU_COMPREHENSIVE_TECHNICAL_DEFENSE_REPORT.md'
    html_path = 'PALASH_SETU_COMPREHENSIVE_TECHNICAL_DEFENSE_REPORT.html'
    
    with open(md_path, 'r', encoding='utf-8') as f:
        md_content = f.read()

    html_body = markdown.markdown(md_content, extensions=['tables', 'fenced_code', 'toc', 'nl2br'])

    html_template = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PALASH-SETU (ᱯᱟᱞᱟᱥ ᱥᱮᱛᱩ) - Comprehensive Technical Defense Report</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Noto+Sans:wght@400;600;700&family=Noto+Sans+Ol+Chiki:wght@500;700&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
<script>mermaid.initialize({startOnLoad:true, theme:'neutral'});</script>
<style>
  @page {
    size: A4 portrait;
    margin: 16mm 14mm;
  }
  * {
    box-sizing: border-box;
  }
  body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    color: #0f172a;
    background: #f1f5f9;
    line-height: 1.65;
    font-size: 13.5px;
    margin: 0;
    padding: 0;
  }
  .container {
    max-width: 980px;
    margin: 20px auto 40px auto;
    background: #ffffff;
    padding: 56px 64px;
    box-shadow: 0 4px 24px rgba(0,0,0,0.08);
    border-radius: 8px;
  }
  .top-action-bar {
    position: sticky;
    top: 0;
    background: #00236f;
    color: #ffffff;
    padding: 12px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    z-index: 1000;
    box-shadow: 0 2px 10px rgba(0,0,0,0.2);
  }
  .top-action-bar .brand {
    font-weight: 700;
    font-size: 15px;
    letter-spacing: 0.5px;
  }
  .btn-print {
    background: #fe932c;
    color: #00164e;
    font-weight: 700;
    border: none;
    padding: 9px 20px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 13.5px;
    box-shadow: 0 2px 6px rgba(0,0,0,0.2);
    transition: all 0.2s;
  }
  .btn-print:hover {
    background: #ffb77d;
    transform: translateY(-1px);
  }
  h1, h2, h3, h4 {
    font-family: 'Inter', sans-serif;
    color: #00236f;
    font-weight: 800;
    margin-top: 1.8em;
    margin-bottom: 0.6em;
    line-height: 1.3;
  }
  h1 {
    font-size: 26px;
    border-bottom: 3px solid #00236f;
    padding-bottom: 8px;
    margin-top: 0.2em;
  }
  h2 {
    font-size: 19px;
    border-bottom: 1.5px solid #e2e8f0;
    padding-bottom: 6px;
    margin-top: 2em;
  }
  h3 {
    font-size: 15px;
    color: #904d00;
  }
  p, li {
    color: #334155;
    font-size: 13.5px;
  }
  ol chiki, .olchiki {
    font-family: 'Noto Sans Ol Chiki', 'Noto Sans', sans-serif;
  }
  code {
    font-family: 'JetBrains Mono', monospace;
    background: #f1f5f9;
    color: #00236f;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
  }
  pre {
    background: #0f172a;
    color: #f8fafc;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    font-size: 12px;
    line-height: 1.5;
  }
  pre code {
    background: transparent;
    color: #f8fafc;
    padding: 0;
    font-weight: 400;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    margin: 20px 0;
    font-size: 12.5px;
  }
  th, td {
    border: 1px solid #cbd5e1;
    padding: 10px 12px;
    text-align: left;
    vertical-align: middle;
  }
  th {
    background: #00236f;
    color: #ffffff;
    font-weight: 700;
  }
  tr:nth-child(even) {
    background: #f8fafc;
  }
  blockquote {
    border-left: 4px solid #fe932c;
    background: #fff9f2;
    margin: 16px 0;
    padding: 12px 18px;
    border-radius: 0 6px 6px 0;
  }
  hr {
    border: none;
    border-top: 1px solid #e2e8f0;
    margin: 32px 0;
  }
  .mermaid {
    text-align: center;
    margin: 24px 0;
    background: #ffffff;
    padding: 16px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
  }
  @media print {
    .top-action-bar {
      display: none !important;
    }
    body {
      background: #ffffff;
    }
    .container {
      box-shadow: none;
      padding: 0;
      max-width: 100%;
      margin: 0;
      border-radius: 0;
    }
    a {
      color: #00236f;
      text-decoration: none;
    }
    pre, blockquote, table, .mermaid {
      page-break-inside: avoid;
    }
    h1, h2, h3 {
      page-break-after: avoid;
    }
  }
</style>
</head>
<body>
<div class="top-action-bar">
  <div class="brand">PALASH-SETU (ᱯᱟᱞᱟᱥ ᱥᱮᱛᱩ) • Technical Defense & Architecture Dossier</div>
  <button class="btn-print" onclick="window.print()">📥 Print / Save as PDF</button>
</div>
<div class="container">
""" + html_body + """
</div>
<script>
  document.querySelectorAll('pre code.language-mermaid').forEach(function(el) {
    var div = document.createElement('div');
    div.className = 'mermaid';
    div.textContent = el.textContent;
    el.parentNode.replaceWith(div);
  });
</script>
</body>
</html>
"""

    with open(html_path, 'w', encoding='utf-8') as f:
        f.write(html_template)
    print(f"Generated {html_path} successfully!")

if __name__ == '__main__':
    generate_html()
