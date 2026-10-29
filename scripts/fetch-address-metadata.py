
#!/usr/bin/env python3
"""抓取地址元数据并生成 region / address_format 的 SQL 快照（ADR-0013 §8.5）。

为什么需要代理：chromium-i18n.appspot.com 直连不通，必须走 HTTP 代理。
为什么生成 SQL 而不是运行期请求：客户内网不可达该域名，运行期必须零外网依赖（§12）。

用法：
    http_proxy=http://host:port https_proxy=http://host:port \
    python3 scripts/fetch-address-metadata.py --countries CN,HK,MO,TW,JP,US --cn-depth 3 \
        --out server/src/main/resources/db/migration/V144__region_address_format_data.sql

注意：生成物是**数据快照**，必须与迁移一起提交；重跑会覆盖同名文件（内容以抓取时间为准）。
"""
import argparse, json, os, sys, time
import urllib.request, urllib.parse
from datetime import datetime, timezone

BASE = "https://chromium-i18n.appspot.com/ssl-address/data"


def fetch(key, retries=3):
    url = BASE + "/" + urllib.parse.quote(key)
    last = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "fullmall-region-snapshot/1.0"})
            with urllib.request.urlopen(req, timeout=25) as response:
                return json.loads(response.read().decode("utf-8"))
        except Exception as error:  # 失败必须暴露：重试后仍失败就退出，不静默跳过
            last = error
            time.sleep(1.5 * (attempt + 1))
    raise SystemExit(f"抓取失败 {key}: {last}")


def esc(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"


COUNTRY_NAMES_ZH = {
    "CN": "中国", "HK": "中国香港", "MO": "中国澳门", "TW": "中国台湾",
    "JP": "日本", "US": "美国", "SG": "新加坡", "MY": "马来西亚", "TH": "泰国",
}


def jsonb_multi(names):
    return esc(json.dumps(names, ensure_ascii=False)) + "::jsonb"


def jsonb(locale, name):
    return esc(json.dumps({locale: name}, ensure_ascii=False)) + "::jsonb"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--countries", required=True)
    parser.add_argument("--cn-depth", type=int, default=1)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    countries = [c.strip().upper() for c in args.countries.split(",") if c.strip()]
    fetched_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    regions = []   # (code, parent, level, depth, country, names_jsonb, source)
    formats = []   # (country, required, fmt, zip, zipex, state_type, has_admin_list, admin_optional, upper)

    for cc in countries:
        data = fetch(cc)
        locale = data.get("lang") or "en"
        name = data.get("name") or cc
        # 国家节点：level=COUNTRY，depth=1
        # ADS 的国家名只有英文（CN--zh 的 name 仍是 CHINA），中文名在这里补，
        # 否则中文界面会显示 CHINA。新开通市场要在这里补一条，缺失时回退英文名。
        country_names = {"zh-CN": COUNTRY_NAMES_ZH.get(cc, name)}
        regions.append((cc, None, "COUNTRY", 1, cc, jsonb_multi(country_names), "ads"))
        require = data.get("require") or None
        fmt = data.get("fmt") or ""
        zip_rule = data.get("zip") or None
        zipex = data.get("zipex") or None
        state_type = data.get("state_name_type") or "province"
        sub_keys = [k for k in (data.get("sub_keys") or "").split("~") if k]
        sub_names = [n for n in (data.get("sub_names") or "").split("~")]
        has_admin = len(sub_keys) > 0
        # admin_optional：require 里没有 S 就是该级不要求填
        admin_optional = "S" not in (require or "")
        formats.append((cc, require, fmt, zip_rule, zipex, state_type, has_admin, admin_optional, data.get("upper")))

        # 一级行政区（level=ADMIN）。code 用 <CC>-<序号>：ADS 的 key 是名称，含中文/空格/变音符号，
        # 不能直接做 code（ADR-0013 §8：匹配只用 code，不用名称）。
        for index, key in enumerate(sub_keys):
            code = f"{cc}-{index + 1:02d}"
            label = sub_names[index] if index < len(sub_names) and sub_names[index] else key
            regions.append((code, cc, "ADMIN", 2, cc, jsonb(locale, label), "ads"))

        # 中国要更深一级：省 → 市 → 区县（ADS 里 CN 是严格三级；再往下一级是街道，不做）
        if cc == "CN" and args.cn_depth >= 3:
            import concurrent.futures as futures
            province_items = [(f"{cc}-{i + 1:02d}", key) for i, key in enumerate(sub_keys)]
            key_of = dict(province_items)

            def fetch_children(item):
                code, key = item
                try:
                    child = fetch(f"{cc}/{key}")
                except SystemExit:
                    return code, []
                keys = [k for k in (child.get("sub_keys") or "").split("~") if k]
                names = [n for n in (child.get("sub_names") or "").split("~")]
                out = []
                for j, ckey in enumerate(keys):
                    label = names[j] if j < len(names) and names[j] else ckey
                    out.append((f"{code}-{j + 1:02d}", ckey, label))
                return code, out

            done = 0
            with futures.ThreadPoolExecutor(max_workers=8) as pool:
                for code, children in pool.map(fetch_children, province_items):
                    done += 1
                    if done % 10 == 0:
                        print(f"  已抓 {done}/{len(province_items)} 个省", file=sys.stderr)
                    for ccode, ckey, label in children:
                        regions.append((ccode, code, "LOCALITY", 3, cc, jsonb(locale, label), "ads"))
                    for ccode, ckey, label in children:
                        try:
                            grand = fetch(f"{cc}/{key_of[code]}/{ckey}")
                        except SystemExit:
                            continue
                        gkeys = [k for k in (grand.get("sub_keys") or "").split("~") if k]
                        gnames = [n for n in (grand.get("sub_names") or "").split("~")]
                        for k, gkey in enumerate(gkeys):
                            glabel = gnames[k] if k < len(gnames) and gnames[k] else gkey
                            regions.append((f"{ccode}-{k + 1:02d}", ccode, "DEPENDENT", 4, cc,
                                            jsonb(locale, glabel), "ads"))

    lines = [
        f"-- 由 scripts/fetch-address-metadata.py 生成，抓取时间 {fetched_at}",
        "-- 数据来源：Google Address Data Service（libaddressinput 元数据；代码 Apache-2.0，区划为政府公开事实）",
        "-- 重新生成见脚本 usage；不要手改本文件",
        "",
        "INSERT INTO region (code, parent_code, level, depth, country_code, names, source_id, source_version)",
        "VALUES",
    ]
    rrows = []
    for code, parent, level, depth, country, names, source in regions:
        rrows.append("    (" + ", ".join([esc(code), esc(parent), esc(level), str(depth), esc(country),
                                        names, esc(source), esc(fetched_at[:10])]) + ")")
    lines.append(",\n".join(rrows) + "\nON CONFLICT (code) DO NOTHING;")
    lines.append("")
    lines.append("INSERT INTO address_format (country_code, required, fmt, zip, zipex, state_name_type,")
    lines.append("                           has_admin_list, admin_optional, upper, source_id, source_version)")
    lines.append("VALUES")
    frows = []
    for cc, require, fmt, zip_rule, zipex, state_type, has_admin, admin_optional, upper in formats:
        frows.append("    (" + ", ".join([esc(cc), esc(require), esc(fmt), esc(zip_rule), esc(zipex),
                                       esc(state_type), str(has_admin).lower(), str(admin_optional).lower(),
                                       esc(upper), "'ads'", esc(fetched_at[:10])]) + ")")
    lines.append(",\n".join(frows) + "\nON CONFLICT (country_code) DO NOTHING;")
    lines.append("")

    tmp = args.out + ".tmp"
    with open(tmp, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines))
    os.replace(tmp, args.out)
    print(f"写出 {args.out}: {len(formats)} 个国家格式，{len(regions)} 个区划节点")


if __name__ == "__main__":
    main()
