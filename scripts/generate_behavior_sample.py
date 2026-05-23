#!/usr/bin/env python3
"""生成客户行为 10 万行 Excel 样例（离线使用，无需启动后端）"""

import random
from datetime import datetime, timedelta
from pathlib import Path

try:
    from openpyxl import Workbook
except ImportError:
    print("请先安装: pip install openpyxl")
    raise

BEHAVIOR_TYPES = ["电话沟通", "拜访", "邮件", "演示"]
DEFAULT_COUNT = 100_000
OUTPUT = Path(__file__).resolve().parent.parent / "data" / "behavior_sample_100000.xlsx"


def generate(count: int = DEFAULT_COUNT, output: Path = OUTPUT) -> Path:
    output.parent.mkdir(parents=True, exist_ok=True)
    wb = Workbook(write_only=True)
    ws = wb.create_sheet("客户行为")
    ws.append(["客户ID", "行为类型", "描述", "行为时间"])
    now = datetime.now()
    for i in range(1, count + 1):
        t = random.choice(BEHAVIOR_TYPES)
        bt = now - timedelta(days=random.randint(0, 364), hours=random.randint(0, 23))
        ws.append([
            random.randint(1, 500),
            t,
            f"{t}记录-{i}",
            bt.strftime("%Y-%m-%d %H:%M:%S"),
        ])
        if i % 10000 == 0:
            print(f"已写入 {i}/{count} 行…")
    wb.save(output)
    print(f"完成: {output} ({count} 行)")
    return output


if __name__ == "__main__":
    generate()
