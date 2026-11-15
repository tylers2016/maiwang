import socket
import binascii
import re

def hfjsj_isbn_search_debug(isbn_raw):
    host = '106.13.235.115'
    port = 210
    header = binascii.unhexlify("00ff17db0000")
    
    # 1. 格式化 ISBN：遵循抓包看到的空格特征
    isbn_clean = isbn_raw.replace('-', '').strip()
    isbn_query = f"{isbn_clean[:6]} {isbn_clean[6:]}" if len(isbn_clean) == 13 else isbn_clean

    # 2. 构造指令：INIT1@10@ 没问题，维持现状
    query = f"REQU_BEGININIT1@10@ ma_isbn='{isbn_query}'BAG_END"
    payload = header + query.encode('gbk')

    print(f"🚀 正在发送指令: {query}")

    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(15)
            s.connect((host, port))
            s.sendall(payload)
            
            response = b""
            while True:
                chunk = s.recv(8192)
                if not chunk: break
                response += chunk
                if b"BAG_END" in response: break 
            
            if response:
                # 使用 GBK 解码原始数据
                content = response.decode('gbk', errors='ignore')
                
                # 提取 REC_BEGIN 和 REC_END 之间的所有记录
                records = re.findall(r'REC_BEGIN(.*?)REC_END', content, re.S)
                
                if not records:
                    print(f"--- 原始回包内容 ---\n{content}")
                    print("\n❌ 未能解析出记录，请检查上方原始回显。")
                    return

                print(f"✅ 成功命中 {len(records)} 条记录：\n")
                for idx, rec in enumerate(records, 1):
                    print(f"=== [记录 {idx}] 完整原始数据 ===")
                    # 直接打印整段记录内容，让你看清目次区后的文字
                    print(rec.strip()) 
                    
                    # 尝试做一个更宽容的题名提取（查找 a 之后的文字）
                    # 在抓包图中题名通常紧跟在 a 后面
                    title_match = re.search(r'a([\u4e00-\u9fa5]+)', rec)
                    if title_match:
                        print(f"\n🏷️ 提取到的可能题名: {title_match.group(1)}")
                    print("-" * 40)
            else:
                print("⚠️ 服务器返回了空数据包。")

    except Exception as e:
        print(f"💥 运行报错: {e}")

if __name__ == "__main__":
    # 使用你抓包验证成功的那个 ISBN
    hfjsj_isbn_search_debug("9787544299015")