import socket
import binascii
import re

HOST = '54.78.43.234'
PORT = 210

# 嚴格使用原版 App 的通訊封包
INIT_HEX = "b43c830205e0840402e1a220850310000086031000009f6e0750795a333935309f6f1550795a3339353020312e302f5a4f4f4d2076312e349f7003312e30"
SEARCH_BASE = "b64d8d01008e01018f01009001019103727331b20b9f6908444953434f564552b52da12b06072a8648ce130301a020bf661dbf2c0a30089f7801019f7901079f2d0d39373830313331313033363237"
PRESENT_HEX = "b81c9f1f037273319e01019d0106b3038001469f68082a8648ce13056d0a"

def bypass_xml_fetch():
    isbn = input("請輸入 13 位 ISBN: ").strip()
    if len(isbn) != 13 or not isbn.isdigit():
        print("[-] 錯誤：請輸入 13 位數字。")
        return

    # 等長替換 ISBN
    target_hex = binascii.hexlify(isbn.encode()).decode()
    search_packet = SEARCH_BASE.replace("39373830313331313033363237", target_hex)

    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(15)

    try:
        s.connect((HOST, PORT))

        # 1. Init
        s.send(binascii.unhexlify(INIT_HEX))
        s.recv(1024)

        # 2. Search
        s.send(binascii.unhexlify(search_packet))
        s.recv(1024)

        # 3. Present
        s.send(binascii.unhexlify(PRESENT_HEX))
        
        raw_stream = b""
        while True:
            try:
                chunk = s.recv(8192)
                if not chunk: break
                raw_stream += chunk
                # 判斷 XML 結束符號
                if b'</mods>' in chunk or b'\x1d' in chunk: break
            except socket.timeout:
                break

        # 4. 暴力提取 XML (跳過外層 ASN.1/Z39.50 包裝)
        xml_start = raw_stream.find(b'<mods')
        if xml_start != -1:
            xml_data = raw_stream[xml_start:]
            xml_end = xml_data.rfind(b'</mods>')
            if xml_end != -1:
                xml_data = xml_data[:xml_end + 7]

            # 轉為字串並過濾無效字元
            xml_str = xml_data.decode('utf-8', errors='ignore')

            # 正則提取用戶可讀資訊
            title = re.search(r'<title>(.*?)</title>', xml_str, re.DOTALL)
            author = re.search(r'<namePart>(.*?)</namePart>', xml_str)
            publisher = re.search(r'<publisher>(.*?)</publisher>', xml_str)
            date = re.search(r'<dateIssued.*?>(.*?)</dateIssued>', xml_str)

            print("\n" + "="*40)
            if title:
                # 處理 <nonSort> 等子標籤
                clean_title = re.sub(r'<[^>]+>', '', title.group(1)).strip()
                print(f"【書名】: {clean_title}")
            print(f"【作者】: {author.group(1).strip() if author else 'N/A'}")
            print(f"【出版】: {publisher.group(1).strip() if publisher else 'N/A'} ({date.group(1).strip() if date and date.group(1).strip() else 'N/A'})")
            print("="*40)

            # 存檔
            file_name = f"{isbn}_raw.xml"
            with open(file_name, "wb") as f:
                f.write(xml_data)
            print(f"\n[+] 剝離成功！伺服器原始資料已存至: {file_name}")
            print("[*] 註：若需生成 .mrc，可使用 Python 的 pymarc 或 xslt 腳本將此 XML 本地轉換。")

        else:
            print("[-] 伺服器回傳了數據，但未發現 XML 標籤，可能查無此書。")

    except Exception as e:
        print(f"[!] 通訊錯誤: {e}")
    finally:
        s.close()

if __name__ == "__main__":
    bypass_xml_fetch()