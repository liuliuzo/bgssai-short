import os
import re

def resolve_md(content):
    def repl(m):
        head = m.group(1).strip()
        feature = m.group(2).strip()
        # Keep head mostly, as it is newer/more detailed.
        # But we need to incorporate "所有应用项目都可以下载 Bgssai Bot 的安装包" into point 5 if missing.
        # For point 5:
        head_lines = head.split('\n')
        for i, line in enumerate(head_lines):
            if line.startswith('5. **Bgssai Bot**') or line.startswith('5. **bgssai-bot**'):
                # merge
                if '所有应用项目都可以下载 Bgssai Bot' not in line and '所有应用项目都可以下载 bgssai-bot' not in line:
                    line = line.replace('所有应用均可下载 Bot 安装包', '所有应用项目都可以下载 Bgssai Bot 的安装包')
                    head_lines[i] = line
        return '\n'.join(head_lines) + '\n'

    # Regex for conflict markers
    # <<<<<<< HEAD\n(.*?)\n=======\n(.*?)\n>>>>>>> [^\n]+
    return re.sub(r'<<<<<<< HEAD\n(.*?)\n=======\n(.*?)\n>>>>>>> [^\n]+', repl, content, flags=re.DOTALL)

def resolve_props(content):
    def repl(m):
        head = m.group(1).strip()
        feature = m.group(2).strip()
        
        lines1 = head.split('\n')
        lines2 = feature.split('\n')
        
        props1 = {}
        for l in lines1:
            if '=' in l and not l.startswith('#'):
                k, v = l.split('=', 1)
                props1[k.strip()] = v.strip()
                
        # Find new properties in feature
        new_lines = []
        for l in lines2:
            if '=' in l and not l.startswith('#'):
                k, v = l.split('=', 1)
                if k.strip() not in props1:
                    new_lines.append(l)
            elif l.startswith('# --- Bot'):
                new_lines.append(l)

        res = head + '\n'
        if new_lines:
            res += '\n' + '\n'.join(new_lines) + '\n'
        return res
        
    return re.sub(r'<<<<<<< HEAD\n(.*?)\n=======\n(.*?)\n>>>>>>> [^\n]+', repl, content, flags=re.DOTALL)

def process_file(path, is_md):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if is_md:
        resolved = resolve_md(content)
    else:
        resolved = resolve_props(content)
        
    with open(path, 'w', encoding='utf-8') as f:
        f.write(resolved)

def main():
    base = r'C:\Users\lzhao3730\Desktop\github\bgssai-short'
    md_files = ['AGENTS.md', 'CLAUDE.md', 'PRODUCT_VISION.md', 'README.md']
    prop_files = [
        r'bgssai-short-user\bgssai-short-user\src\main\resources\application-dev.properties',
        r'bgssai-short-user\bgssai-short-user\src\main\resources\application-local.properties',
        r'bgssai-short-user\bgssai-short-user\src\main\resources\application-prod.properties',
        r'bgssai-short-user\bgssai-short-user\src\main\resources\application-test.properties'
    ]
    
    for f in md_files:
        p = os.path.join(base, f)
        process_file(p, True)
        
    for f in prop_files:
        p = os.path.join(base, f)
        process_file(p, False)

if __name__ == '__main__':
    main()
