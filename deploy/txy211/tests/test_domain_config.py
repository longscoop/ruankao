from pathlib import Path
import unittest


DEPLOY_DIR = Path(__file__).resolve().parents[1]


class DomainConfigTest(unittest.TestCase):
    def test_nginx_serves_the_bound_domain_over_https(self):
        config = (DEPLOY_DIR / "nginx.conf").read_text()
        self.assertIn("server_name www.e68q.cn;", config)
        self.assertIn("listen 443 ssl;", config)
        self.assertIn("/etc/letsencrypt/live/www.e68q.cn/fullchain.pem", config)
        self.assertIn("return 301 https://www.e68q.cn$request_uri;", config)
        self.assertIn("proxy_pass http://127.0.0.1:8080;", config)
        self.assertIn("alias /opt/ruankao/data/storage/;", config)

    def test_server_receives_wechat_login_settings(self):
        compose = (DEPLOY_DIR / "docker-compose.yml").read_text()
        self.assertIn("WECHAT_APP_ID: ${WECHAT_APP_ID:?", compose)
        self.assertIn("WECHAT_APP_SECRET: ${WECHAT_APP_SECRET:?", compose)

    def test_public_storage_url_uses_https(self):
        env_example = (DEPLOY_DIR / ".env.example").read_text()
        self.assertIn("STORAGE_PUBLIC_BASE_URL=https://www.e68q.cn/storage", env_example)


if __name__ == "__main__":
    unittest.main()
