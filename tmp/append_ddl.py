from pathlib import Path
import re
ddl_path = Path(r"C:\Users\lzhao3730\Desktop\github\bgssai-short\sql\DDL.sql")
upgrade = Path(r"C:\Users\lzhao3730\Desktop\github\bgssai-short\sql\UPGRADE-20260826-full-features.sql").read_text(encoding="utf-8")
text = ddl_path.read_text(encoding="utf-8")
if "ai_model_config" in text:
    print("already present")
else:
    start = upgrade.find("CREATE TABLE IF NOT EXISTS `ai_model_config`")
    end = upgrade.find("-- 密钥占位")
    chunk = upgrade[start:end]
    parts = []
    for m in re.finditer(r"CREATE TABLE IF NOT EXISTS `(\w+)` \((.*?)\n\) ENGINE=InnoDB.*?;", chunk, re.S):
        name = m.group(1)
        full = m.group(0).replace("CREATE TABLE IF NOT EXISTS", "CREATE TABLE")
        parts.append(f"DROP TABLE IF EXISTS `{name}`;\n{full}")
    append = "\n\n-- ---------- full features 2026-08-26 ----------\n" + "\n\n".join(parts) + "\n"
    ddl_path.write_text(text.rstrip() + append, encoding="utf-8")
    print("appended", len(parts), "tables")
