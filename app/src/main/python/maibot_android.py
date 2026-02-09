"""
MaiBot Android 适配启动脚本
用于在Android环境中启动完整的MaiBot服务
"""

import os
import sys
import json
import threading
import time
from pathlib import Path

# 设置环境变量
os.environ['MAIBOT_ANDROID'] = '1'
os.environ['HOST'] = '127.0.0.1'
os.environ['PORT'] = '8000'
os.environ['WEBUI_HOST'] = '127.0.0.1'
os.environ['WEBUI_PORT'] = '8001'

# 获取Android应用数据目录
def get_android_data_dir():
    """获取Android应用数据目录"""
    # 在Chaquopy中，当前工作目录就是应用数据目录
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

# 设置环境变量
os.environ['MAIBOT_CONFIG_DIR'] = str(CONFIG_DIR)
os.environ['MAIBOT_DB_DIR'] = str(DB_DIR)
os.environ['MAIBOT_LOG_DIR'] = str(LOG_DIR)

class MaiBotAndroidServer:
    """MaiBot Android服务器"""
    
    def __init__(self):
        self.server_thread = None
        self.is_running = False
        self.main_system = None
        
    def initialize_config(self, api_provider, api_key, instance_count=3):
        """初始化配置文件"""
        try:
            import toml
            
            # 读取模板配置
            template_dir = Path(__file__).parent / "template"
            
            # 创建bot_config.toml
            bot_config_path = CONFIG_DIR / "bot_config.toml"
            if not bot_config_path.exists():
                # 从模板复制并修改
                with open(template_dir / "bot_config_template.toml", 'r', encoding='utf-8') as f:
                    bot_config = toml.load(f)
                
                # 修改配置
                bot_config['bot']['nickname'] = "麦麦"
                bot_config['bot']['platform'] = "android"
                bot_config['chat']['talk_value'] = 1.0
                
                # 保存配置
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
                
                # 保存配置
                with open(model_config_path, 'w', encoding='utf-8') as f:
                    toml.dump(model_config, f)
                
                return True
                
        except Exception as e:
            print(f"初始化配置失败: {e}")
            return False
    
    def _get_base_url(self, provider):
        """获取API提供商的base URL"""
        urls = {
            "DeepSeek": "https://api.deepseek.com/v1",
            "OpenAI": "https://api.openai.com/v1",
            "SiliconFlow": "https://api.siliconflow.cn/v1",
            "BaiLian": "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "Google": "https://generativelanguage.googleapis.com/v1beta"
        }
        return urls.get(provider, "https://api.deepseek.com/v1")
    
    def _get_model_identifier(self, provider):
        """获取模型标识符"""
        models = {
            "DeepSeek": "deepseek-chat",
            "OpenAI": "gpt-3.5-turbo",
            "SiliconFlow": "deepseek-ai/DeepSeek-V3",
            "BaiLian": "qwen-turbo",
            "Google": "gemini-pro"
        }
        return models.get(provider, "deepseek-chat")
    
    def start(self):
        """启动MaiBot服务"""
        if self.is_running:
            return True
        
        try:
            # 添加src到Python路径
            src_path = Path(__file__).parent / "src"
            if str(src_path) not in sys.path:
                sys.path.insert(0, str(src_path))
            
            # 导入MaiBot主程序
            from src.main import MainSystem
            import asyncio
            
            # 创建主系统
            self.main_system = MainSystem()
            
            # 在后台线程中运行
            def run_server():
                try:
                    asyncio.run(self._run_server())
                except Exception as e:
                    print(f"服务器运行错误: {e}")
            
            self.server_thread = threading.Thread(target=run_server, daemon=True)
            self.server_thread.start()
            self.is_running = True
            
            print("MaiBot服务启动成功")
            return True
            
        except Exception as e:
            print(f"启动MaiBot服务失败: {e}")
            return False
    
    async def _run_server(self):
        """运行服务器"""
        try:
            await self.main_system.initialize()
            await self.main_system.schedule_tasks()
        except Exception as e:
            print(f"服务器运行错误: {e}")
    
    def stop(self):
        """停止MaiBot服务"""
        if not self.is_running:
            return
        
        try:
            if self.main_system:
                # 停止服务
                import asyncio
                asyncio.run(self.main_system.app.stop())
            
            self.is_running = False
            print("MaiBot服务已停止")
            
        except Exception as e:
            print(f"停止服务失败: {e}")
    
    def is_running(self):
        """检查服务是否正在运行"""
        return self.is_running

# 全局服务器实例
_server = None

def get_server():
    """获取服务器实例"""
    global _server
    if _server is None:
        _server = MaiBotAndroidServer()
    return _server

def initialize_config(api_provider, api_key, instance_count=3):
    """初始化配置"""
    server = get_server()
    return server.initialize_config(api_provider, api_key, instance_count)

def start_server():
    """启动服务器"""
    server = get_server()
    return server.start()

def stop_server():
    """停止服务器"""
    server = get_server()
    server.stop()

def is_server_running():
    """检查服务器是否运行"""
    server = get_server()
    return server.is_running

if __name__ == '__main__':
    # 测试
    print("MaiBot Android 启动脚本")
    print(f"数据目录: {DATA_DIR}")
    print(f"配置目录: {CONFIG_DIR}")
    print(f"数据库目录: {DB_DIR}")
