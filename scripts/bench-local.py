#!/usr/bin/env python3
"""本地性能基线：并发打读接口，输出 P50/P95/吞吐。仅本机参考，不代表生产。"""
import json, statistics, sys, time, urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
CONCURRENCY = int(sys.argv[2]) if len(sys.argv) > 2 else 20
REQUESTS = int(sys.argv[3]) if len(sys.argv) > 3 else 200

PATHS = [
    ("商品列表", "/api/v1/products?size=20"),
    ("商品详情", "/api/v1/products/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
    ("促销试算", "/api/v1/promotions/preview"),
]

def one(path):
    start = time.perf_counter()
    req = urllib.request.Request(BASE + path)
    with urllib.request.urlopen(req, timeout=10) as r:
        r.read()
        code = r.status
    return code, (time.perf_counter() - start) * 1000

def run(name, path):
    if name == "促销试算":
        body = {"items": [{"skuId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "quantity": 1}]}
        def call(_):
            start = time.perf_counter()
            req = urllib.request.Request(BASE + path, data=json.dumps(body).encode(),
                                         headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=10) as r:
                r.read(); return r.status, (time.perf_counter() - start) * 1000
        fn = call
    else:
        fn = lambda _: one(path)
    started = time.perf_counter()
    with ThreadPoolExecutor(max_workers=CONCURRENCY) as pool:
        results = list(pool.map(fn, range(REQUESTS)))
    elapsed = time.perf_counter() - started
    latencies = sorted(ms for _, ms in results)
    errors = sum(1 for code, _ in results if code >= 400)
    p50 = statistics.median(latencies)
    p95 = latencies[int(len(latencies) * 0.95) - 1]
    print(f"{name}: n={REQUESTS} 并发={CONCURRENCY} errors={errors} "
          f"P50={p50:.1f}ms P95={p95:.1f}ms 吞吐={REQUESTS / elapsed:.1f} req/s")

for name, path in PATHS:
    run(name, path)
