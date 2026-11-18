#!/usr/bin/env python3
"""Validate user-approved JSON price feeds offline. No scraping, network, or credentials.

Usage: python tools/validate_external_feed.py authorized-offers.json
"""
import datetime as dt
import json
import math
import sys
from pathlib import Path
from urllib.parse import urlparse


def ensure_https(url):
    parsed = urlparse(url)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password:
        raise ValueError("Offer URL must be absolute HTTPS without credentials")


def validate(obj, now=None):
    if not isinstance(obj, dict) or obj.get("scope") != "external":
        raise ValueError("scope must equal external")
    code = obj.get("product_code")
    if not isinstance(code, str) or not code.strip():
        raise ValueError("product_code required")
    if obj.get("currency") != "TRY":
        raise ValueError("Only TRY supported")
    name = obj.get("source_name")
    if not isinstance(name, str) or not 1 <= len(name.strip()) <= 100:
        raise ValueError("source_name required (max 100)")
    snapshot = obj.get("snapshot_id")
    if not isinstance(snapshot, str) or not 3 <= len(snapshot.strip()) <= 100:
        raise ValueError("snapshot_id required (3–100 chars)")
    try:
        observed = dt.datetime.fromisoformat(obj["observed_at"].replace("Z", "+00:00"))
    except Exception as exc:
        raise ValueError("observed_at must be ISO 8601 UTC") from exc
    if observed.tzinfo is None:
        raise ValueError("observed_at timezone required")
    now = now or dt.datetime.now(dt.timezone.utc)
    if not (-600 <= (now - observed).total_seconds() <= 48*3600):
        raise ValueError("stale or future-dated feed")
    offers = obj.get("offers")
    if not isinstance(offers, list) or not 1 <= len(offers) <= 25:
        raise ValueError("1–25 offers required")
    valid = []
    for offer in offers:
        if offer.get("product_code") != code:
            raise ValueError("offer product_code mismatch")
        if offer.get("in_stock") is not True:
            continue
        name = offer.get("merchant")
        if not isinstance(name, str) or not 1 <= len(name.strip()) <= 100:
            raise ValueError("merchant missing or too long")
        price, shipping = offer.get("price"), offer.get("shipping")
        if type(price) not in (float, int) or not math.isfinite(price) or not 0 < price <= 1e9:
            raise ValueError("invalid price")
        if type(shipping) not in (float, int) or not math.isfinite(shipping) or not 0 <= shipping <= 1e6:
            raise ValueError("invalid shipping")
        total = price+shipping
        if total > 1e9:
            raise ValueError("invalid total")
        ensure_https(offer.get("url", ""))
        valid.append((total, name))
    if not valid:
        raise ValueError("no in-stock offers")
    return sorted(valid)


def main():
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    best = validate(data)[0]
    print(f"Şema geçerli. En düşük kargo dahil toplam: {best[0]:.2f} TRY - {best[1]}")
    print("NOT: Şema doğrulaması verilerin gerçekten mağazadan geldiğini KANITLAMAZ.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ValueError, KeyError, json.JSONDecodeError) as exc:
        print(f"HATA: {exc}", file=sys.stderr)
        sys.exit(1)
