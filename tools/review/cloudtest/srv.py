import http.server, json, sys
MODE = {"gemini": "fallback"}
class H(http.server.BaseHTTPRequestHandler):
    def log_message(self, *a): pass
    def do_POST(self):
        n = int(self.headers.get("Content-Length", 0)); self.rfile.read(n)
        p = self.path
        if p.startswith("/mode/"):
            MODE["gemini"] = p.split("/")[2]; self.send_response(200); self.end_headers(); self.wfile.write(b"{}"); return
        if p.startswith("/gem/models/"):
            model = p.split("/")[3].split(":")[0]
            if MODE["gemini"] == "down": return self.err(503, "overloaded")
            if model == "gemini-2.5-flash": return self.err(404, "models/gemini-2.5-flash is not found")
            return self.ok({"candidates": [{"content": {"parts": [{"text": "OK from " + model}]}}]})
        if p.startswith("/oai/chat/completions"):
            if self.headers.get("Authorization") != "Bearer sk-test-0123456789abcdefghijk": return self.err(401, "bad key")
            return self.ok({"choices": [{"message": {"content": "OK from chatgpt"}}]})
        self.err(404, "no")
    def ok(self, o): b = json.dumps(o).encode(); self.send_response(200); self.send_header("Content-Type", "application/json"); self.end_headers(); self.wfile.write(b)
    def err(self, c, m): b = json.dumps({"error": {"message": m}}).encode(); self.send_response(c); self.end_headers(); self.wfile.write(b)
http.server.HTTPServer(("127.0.0.1", 18765), H).serve_forever()
