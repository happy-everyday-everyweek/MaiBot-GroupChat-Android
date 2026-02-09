"""
MaiBot服务 - 简化版
适配Android环境的轻量级聊天服务
"""

import json
import random
import time
from http.server import HTTPServer, BaseHTTPRequestHandler
import threading

class MaiBotHandler(BaseHTTPRequestHandler):
    """处理HTTP请求"""
    
    def do_POST(self):
        """处理POST请求"""
        if self.path == '/api/chat':
            self.handle_chat()
        else:
            self.send_error(404, "Not Found")
    
    def do_GET(self):
        """处理GET请求"""
        if self.path == '/api/health':
            self.handle_health()
        else:
            self.send_error(404, "Not Found")
    
    def handle_chat(self):
        """处理聊天请求"""
        try:
            # 获取请求体长度
            content_length = int(self.headers['Content-Length'])
            # 读取请求体
            post_data = self.rfile.read(content_length)
            data = json.loads(post_data.decode('utf-8'))
            
            message = data.get('message', '')
            
            # 模拟处理延迟
            time.sleep(0.5 + random.random())
            
            # 生成回复
            reply = self.generate_reply(message)
            
            # 发送响应
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            
            response = {
                'status': 'success',
                'reply': reply,
                'timestamp': time.time()
            }
            
            self.wfile.write(json.dumps(response).encode('utf-8'))
            
        except Exception as e:
            self.send_error(500, f"Internal Server Error: {str(e)}")
    
    def handle_health(self):
        """处理健康检查请求"""
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        response = {
            'status': 'healthy',
            'service': 'MaiBot',
            'version': '1.0.0'
        }
        
        self.wfile.write(json.dumps(response).encode('utf-8'))
    
    def generate_reply(self, message):
        """生成回复"""
        # 简单的回复逻辑
        replies = [
            "你好！很高兴和你聊天。",
            "这个问题很有趣，让我想想...",
            "我觉得你说得对！",
            "嗯，有道理",
            "哈哈，太搞笑了",
            "我不太确定，再聊聊别的吧",
            "这个想法不错",
            "是的，我同意你的观点",
            "让我仔细考虑一下",
            "你能详细说说吗？",
            "我明白了，继续说",
            "这确实是个有趣的话题",
            "我从没这样想过",
            "你说得很有道理",
            "我也这么觉得"
        ]
        
        # 根据消息内容选择回复
        if "你好" in message or "嗨" in message:
            return "你好！很高兴见到你！"
        elif "谢谢" in message:
            return "不客气！"
        elif "再见" in message or "拜拜" in message:
            return "再见！期待下次聊天！"
        elif "?" in message or "？" in message:
            return "这是个好问题！让我想想..."
        else:
            return random.choice(replies)
    
    def log_message(self, format, *args):
        """重写日志方法，减少输出"""
        pass

class MaiBotServer:
    """MaiBot服务类"""
    
    def __init__(self, host='127.0.0.1', port=8000):
        self.host = host
        self.port = port
        self.server = None
        self.server_thread = None
        self.is_running = False
    
    def start(self):
        """启动服务"""
        if self.is_running:
            return
        
        try:
            self.server = HTTPServer((self.host, self.port), MaiBotHandler)
            self.server_thread = threading.Thread(target=self.server.serve_forever)
            self.server_thread.daemon = True
            self.server_thread.start()
            self.is_running = True
            print(f"MaiBot服务已启动: http://{self.host}:{self.port}")
        except Exception as e:
            print(f"启动MaiBot服务失败: {e}")
            self.is_running = False
    
    def stop(self):
        """停止服务"""
        if not self.is_running:
            return
        
        try:
            if self.server:
                self.server.shutdown()
                self.server.server_close()
            self.is_running = False
            print("MaiBot服务已停止")
        except Exception as e:
            print(f"停止MaiBot服务失败: {e}")
    
    def is_server_running(self):
        """检查服务是否正在运行"""
        return self.is_running

# 全局服务实例
_server = None

def get_server():
    """获取服务实例"""
    global _server
    if _server is None:
        _server = MaiBotServer()
    return _server

def start_server():
    """启动服务"""
    server = get_server()
    server.start()
    return server

def stop_server():
    """停止服务"""
    server = get_server()
    server.stop()

if __name__ == '__main__':
    # 测试服务
    server = start_server()
    
    try:
        # 保持运行
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        stop_server()
