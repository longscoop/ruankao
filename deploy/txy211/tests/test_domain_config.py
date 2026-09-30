import re
import unittest
from pathlib import Path


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

    def test_http_never_serves_the_api_or_admin(self):
        config = (DEPLOY_DIR / "nginx.conf").read_text()
        self.assertIn("listen 80 default_server;", config)
        self.assertEqual(config.count("proxy_pass http://127.0.0.1:8080;"), 1)
        self.assertEqual(config.count("root /var/www/ruankao-admin;"), 1)

    def test_server_receives_wechat_login_settings(self):
        compose = (DEPLOY_DIR / "docker-compose.yml").read_text()
        self.assertIn("WECHAT_APP_ID: ${WECHAT_APP_ID:?", compose)
        self.assertIn("WECHAT_APP_SECRET: ${WECHAT_APP_SECRET:?", compose)

    def test_public_storage_url_uses_https(self):
        env_example = (DEPLOY_DIR / ".env.example").read_text()
        self.assertIn("STORAGE_PUBLIC_BASE_URL=https://www.e68q.cn/storage", env_example)

    def test_pdf_upload_limit_is_consistent_across_proxy_and_server(self):
        nginx = (DEPLOY_DIR / "nginx.conf").read_text()
        api_location = re.search(r"location /api/ \{([^}]*)\}", nginx, re.DOTALL)
        self.assertIsNotNone(api_location)
        self.assertIn("client_max_body_size 25m;", api_location.group(1))

        server = (DEPLOY_DIR.parent.parent / "server/src/main/resources/application.yml").read_text()
        self.assertIn("max-file-size: 20MB", server)
        self.assertIn("max-request-size: 25MB", server)


if __name__ == "__main__":
    unittest.main()
