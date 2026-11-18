import datetime as dt
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'tools'))
from validate_external_feed import validate


def sample():
    return {
        'scope': 'external',
        'product_code': '797577544',
        'currency': 'TRY',
        'source_name': 'İzinli Test Sağlayıcısı',
        'snapshot_id': 'nov18-001',
        'observed_at': dt.datetime.now(dt.timezone.utc).isoformat(),
        'offers': [
            {'product_code': '797577544', 'merchant': 'Satıcı A', 'price': 280.0, 'shipping': 35.0, 'in_stock': True, 'url': 'https://example.org/a'},
            {'product_code': '797577544', 'merchant': 'Satıcı B', 'price': 300.0, 'shipping': 0.0, 'in_stock': True, 'url': 'https://example.org/b'},
        ]
    }

class TestFeed(unittest.TestCase):
    def test_shipping_included(self):
        assert validate(sample())[0] == (300.0, 'Satıcı B')
    def test_mismatched_product_rejected(self):
        obj = sample(); obj['offers'][0]['product_code'] = '999'
        with self.assertRaises(ValueError): validate(obj)
    def test_stale_feed_rejected(self):
        obj = sample()
        obj['observed_at'] = (dt.datetime.now(dt.timezone.utc)-dt.timedelta(days=10)).isoformat()
        with self.assertRaises(ValueError): validate(obj)
    def test_http_rejected(self):
        obj = sample(); obj['offers'][0]['url'] = 'http://example.com'
        with self.assertRaises(ValueError): validate(obj)
    def test_out_of_stock_ignored(self):
        obj = sample(); obj['offers'][1]['in_stock'] = False
        assert validate(obj)[0] == (315.0, 'Satıcı A')
    def test_source_not_proof(self):
        obj = sample(); obj['scope'] = 'official-trendyol'
        with self.assertRaises(ValueError): validate(obj)

if __name__ == '__main__': unittest.main()
