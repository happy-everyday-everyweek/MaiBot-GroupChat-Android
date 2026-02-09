"""
MaiBot Android 适配启动脚本（生产环境版）
用于在Android环境中启动完整的MaiBot服务，提供HTTP API接口
"""

import os
import sys
import json
import threading
import time
import asyncio
from pathlib import Path
from typing import Optional, Dict, Any

# 设置环境变量
os.environ['MAIBOT_ANDROID'] = '1'
os.environ['HOST'] = '127.0.0.1'
os.environ['PORT'] = '8000'
os.environ['WEBUI_HOST'] = '127.0.0.1'
os.environ['WEBUI_PORT'] = '8001'

# 获取Android应用数据目录
def get_android_data_dir():
    """获取Android应用数据目录"""
    return Path(os.getcwd())

# 设置配置目录
DATA_DIR = get_android_data_dir()
CONFIG_DIR = DATA_DIR / "config"
DB_DIR = DATA_DIR / "data"
LOG_DIR = DATA_DIR / "logs"

# 创建必要的目录
CONFIG_DIR.mkdir(exist_ok=True)
DB_DIR.mkdir(exist_ok=True)
LOG_DIR.mkdir(exist_ok=True)

# 获取MaiBot目录
MAIBOT_DIR = Path(__file__).parent / "MaiBot"

# 设置环境变量
os.environ['MAIBOT_CONFIG_DIR'] = str(CONFIG_DIR)
os.environ['MAIBOT_DB_DIR'] = str(DB_DIR)
os.environ['MAIBOT_LOG_DIR'] = str(LOG_DIR)

# 设置模板目录环境变量（指向MaiBot包内的template目录）
MAIBOT_TEMPLATE_DIR = MAIBOT_DIR / "template"
os.environ['MAIBOT_TEMPLATE_DIR'] = str(MAIBOT_TEMPLATE_DIR)

# 全局变量
_server_instance: Optional['MaiBotAndroidServer'] = None
_message_callback: Optional[callable] = None


class MaiBotAndroidServer:
    """MaiBot Android服务器 - 生产环境版本"""
    
    def __init__(self):
        self.server_thread: Optional[threading.Thread] = None
        self.is_running = False
        self.main_system = None
        self.fastapi_app = None
        self.configured = False
        
    def initialize_config(self, api_provider: str, api_key: str, instance_count: int = 3) -> bool:
        """初始化配置文件"""
        try:
            import toml
            
            template_dir = MAIBOT_DIR / "template"
            
            # 创建bot_config.toml
            bot_config_path = CONFIG_DIR / "bot_config.toml"
            if not bot_config_path.exists():
                with open(template_dir / "bot_config_template.toml", 'r', encoding='utf-8') as f:
                    bot_config = toml.load(f)
                
                # 修改配置
                bot_config['bot']['nickname'] = "麦麦"
                bot_config['bot']['platform'] = "android"
                bot_config['bot']['qq_account'] = "android_bot"
                bot_config['chat']['talk_value'] = 1.0
                bot_config['webui']['mode'] = "production"
                bot_config['log']['file_log_level'] = "INFO"
                bot_config['log']['console_log_level'] = "INFO"
                
                with open(bot_config_path, 'w', encoding='utf-8') as f:
                    toml.dump(bot_config, f)
            
            # 创建model_config.toml
            model_config_path = CONFIG_DIR / "model_config.toml"
            if not model_config_path.exists():
                with open(template_dir / "model_config_template.toml", 'r', encoding='utf-8') as f:
                    model_config = toml.load(f)
                
                # 配置API提供商
                provider_config = {
                    "name": api_provider,
                    "base_url": self._get_base_url(api_provider),
                    "api_key": api_key,
                    "client_type": "openai",
                    "max_retry": 2,
                    "timeout": 120,
                    "retry_interval": 10
                }
                
                model_config['api_providers'] = [provider_config]
                
                # 配置模型
                model_config['models'] = [{
                    "model_identifier": self._get_model_identifier(api_provider),
                    "name": "default-model",
                    "api_provider": api_provider,
                    "price_in": 0.0,
                    "price_out": 0.0
                }]
                
                # 配置任务
                for task in ['utils', 'tool_use', 'replyer', 'planner', 'vlm', 'voice', 'embedding']:
                    if task in model_config.get('model_task_config', {}):
                        model_config['model_task_config'][task]['model_list'] = ["default-model"]
                
                with open(model_config_path, 'w', encoding='utf-8') as f:
                    toml.dump(model_config, f)
            
            self.configured = True
            return True
                
        except Exception as e:
            print(f"初始化配置失败: {e}")
            import traceback
            traceback.print_exc()
            return False
    
    def _get_base_url(self, provider: str) -> str:
        """获取API提供商的base URL"""
        urls = {
            "DeepSeek": "https://api.deepseek.com/v1",
            "OpenAI": "https://api.openai.com/v1",
            "SiliconFlow": "https://api.siliconflow.cn/v1",
            "BaiLian": "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "Google": "https://generativelanguage.googleapis.com/v1beta"
        }
        return urls.get(provider, "https://api.deepseek.com/v1")
    
    def _get_model_identifier(self, provider: str) -> str:
        """获取模型标识符"""
        models = {
            "DeepSeek": "deepseek-chat",
            "OpenAI": "gpt-3.5-turbo",
            "SiliconFlow": "deepseek-ai/DeepSeek-V3",
            "BaiLian": "qwen-turbo",
            "Google": "gemini-pro"
        }
        return models.get(provider, "deepseek-chat")
    
    def start(self) -> bool:
        """启动MaiBot服务和FastAPI服务器"""
        if self.is_running:
            return True
        
        if not self.configured:
            print("错误：未初始化配置，请先调用initialize_config()")
            return False
        
        try:
            # 添加MaiBot src到Python路径
            src_path = MAIBOT_DIR / "src"
            if str(src_path) not in sys.path:
                sys.path.insert(0, str(src_path))
            
            if str(MAIBOT_DIR) not in sys.path:
                sys.path.insert(0, str(MAIBOT_DIR))
            
            # 启动FastAPI服务器
            self.server_thread = threading.Thread(target=self._run_fastapi_server, daemon=True)
            self.server_thread.start()
            self.is_running = True
            
            print("MaiBot服务启动成功")
            return True
            
        except Exception as e:
            print(f"启动MaiBot服务失败: {e}")
            import traceback
            traceback.print_exc()
            return False
    
    def _run_fastapi_server(self):
        """运行FastAPI服务器"""
        try:
            import uvicorn
            from fastapi import FastAPI, HTTPException
            from fastapi.middleware.cors import CORSMiddleware
            from pydantic import BaseModel
            
            # 创建FastAPI应用
            self.fastapi_app = FastAPI(title="MaiBot Android API", version="1.0.0")
            
            # 添加CORS中间件
            self.fastapi_app.add_middleware(
                CORSMiddleware,
                allow_origins=["*"],
                allow_credentials=True,
                allow_methods=["*"],
                allow_headers=["*"],
            )
            
            # 请求模型
            class ChatRequest(BaseModel):
                message: str
                user_id: Optional[str] = "android_user"
                conversation_id: Optional[str] = "default"
            
            class ChatResponse(BaseModel):
                reply: str
                success: bool
                error: Optional[str] = None
            
            class HealthResponse(BaseModel):
                status: str
                version: str
                timestamp: float
            
            # 健康检查端点
            @self.fastapi_app.get("/api/health", response_model=HealthResponse)
            async def health_check():
                return HealthResponse(
                    status="healthy",
                    version="1.0.0",
                    timestamp=time.time()
                )
            
            # 聊天端点
            @self.fastapi_app.post("/api/chat", response_model=ChatResponse)
            async def chat(request: ChatRequest):
                try:
                    # 调用MaiBot核心处理消息
                    reply = await self._process_message(
                        request.message, 
                        request.user_id,
                        request.conversation_id
                    )
                    return ChatResponse(reply=reply, success=True)
                except Exception as e:
                    print(f"处理消息失败: {e}")
                    import traceback
                    traceback.print_exc()
                    return ChatResponse(
                        reply="",
                        success=False,
                        error=str(e)
                    )
            
            # 启动uvicorn服务器
            uvicorn.run(
                self.fastapi_app,
                host="127.0.0.1",
                port=8000,
                log_level="info",
                access_log=False
            )
            
        except Exception as e:
            print(f"FastAPI服务器运行错误: {e}")
            import traceback
            traceback.print_exc()
    
    async def _process_message(self, message: str, user_id: str, conversation_id: str) -> str:
        """处理用户消息并返回回复"""
        try:
            # 导入MaiBot核心
            from src.chat.message_receive.message import Message as MaiMessage
            from src.chat.message_receive.chat_stream import ChatStream
            from src.chat.brain_chat.brain_chat import BrainChat
            
            # 创建聊天流
            chat_stream = ChatStream(
                platform="android",
                user_id=user_id,
                group_id=conversation_id
            )
            
            # 创建消息对象
            msg = MaiMessage(
                message=message,
                chat_stream=chat_stream,
                sender_info={"user_id": user_id, "nickname": "用户"}
            )
            
            # 处理消息并获取回复
            brain_chat = BrainChat()
            reply = await brain_chat.process_message(msg)
            
            return reply if reply else "抱歉，我现在无法回复。"
            
        except Exception as e:
            print(f"处理消息异常: {e}")
            import traceback
            traceback.print_exc()
            return f"处理消息时出现错误: {str(e)}"
    
    def stop(self):
        """停止MaiBot服务"""
        if not self.is_running:
            return
        
        try:
            self.is_running = False
            print("MaiBot服务已停止")
            
        except Exception as e:
            print(f"停止服务失败: {e}")
            import traceback
            traceback.print_exc()
    
    def is_server_running(self) -> bool:
        """检查服务是否正在运行"""
        return self.is_running


def get_server() -> MaiBotAndroidServer:
    """获取服务器实例（单例模式）"""
    global _server_instance
    if _server_instance is None:
        _server_instance = MaiBotAndroidServer()
    return _server_instance


def initialize_config(api_provider: str, api_key: str, instance_count: int = 3) -> bool:
    """初始化配置"""
    server = get_server()
    return server.initialize_config(api_provider, api_key, instance_count)


def start_server() -> bool:
    """启动服务器"""
    server = get_server()
    return server.start()


def stop_server():
    """停止服务器"""
    server = get_server()
    server.stop()


def is_server_running() -> bool:
    """检查服务器是否运行"""
    server = get_server()
    return server.is_server_running()


def set_message_callback(callback):
    """设置消息回调函数（供Java层调用）"""
    global _message_callback
    _message_callback = callback


if __name__ == '__main__':
    print("MaiBot Android 启动脚本（生产环境版）")
    print(f"数据目录: {DATA_DIR}")
    print(f"配置目录: {CONFIG_DIR}")
    print(f"数据库目录: {DB_DIR}")
    print(f"日志目录: {LOG_DIR}")
    print(f"MaiBot目录: {MAIBOT_DIR}")
