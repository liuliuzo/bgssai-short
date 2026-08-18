from pathlib import Path
import re

root = Path(r"C:\Users\lzhao3730\Desktop\github\bgssai-short")
up = (root / "sql" / "UPGRADE-20260818-short-production.sql").read_text(encoding="utf-8")
body = up.split("USE `bgssai_short`;", 1)[1]


def repl(m):
    name = m.group(1)
    return f"DROP TABLE IF EXISTS `{name}`;\nCREATE TABLE `{name}`"


body = re.sub(r"CREATE TABLE IF NOT EXISTS `([^`]+)`", repl, body)
ddl_path = root / "sql" / "DDL.sql"
text = ddl_path.read_text(encoding="utf-8")
if "short_work" not in text:
    ddl_path.write_text(
        text.rstrip() + "\n\n-- ----------------------------\n-- 短剧制作闭环 MVP\n-- ----------------------------\n" + body.lstrip(),
        encoding="utf-8",
    )
    print("appended")
else:
    print("already present")
