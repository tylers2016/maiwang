import os
import json
import socket
import binascii
import re
import urllib.request as urllib
from xml.etree import ElementTree as ET
import ssl
import requests
from bs4 import BeautifulSoup
import time

# --- Config ---
# 1. 将原来的固定图片路径改为文件夹路径
PLACEHOLDER_FOLDER = r"c:\Sword-polish\脉望\metadata_covers\edition_placeholder"
# 2. 留一个原本的路径作为绝对保底（万一文件夹不存在或里面没图）
DEFAULT_PLACEHOLDER = r"c:\Sword-polish\脉望\metadata_covers\edition_placeholder.png"


def get_random_placeholder():
    """动态获取 edition_placeholder 文件夹中的任意一张 PNG"""
    try:
        if os.path.exists(PLACEHOLDER_FOLDER):
            png_files = [f for f in os.listdir(PLACEHOLDER_FOLDER) if f.lower().endswith('.png')]
            if png_files:
                return os.path.join(PLACEHOLDER_FOLDER, random.choice(png_files))
    except Exception as e:
        print(f"[Placeholder] 读取随机图片失败: {e}")
    return DEFAULT_PLACEHOLDER

# --- Cover Fetcher ---
def fetch_cover(isbn):
    url = f"https://isbnsearch.org/isbn/{isbn}"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.content, 'html.parser')
        
        # fallback to standard CSS selectors if lxml is missing
        img_tag = soup.select_one('body > div:nth-of-type(1) > div:nth-of-type(1) > div:nth-of-type(1) > img')
        if not img_tag:
            img_tag = soup.select_one('.image img') # fallback selector
            
        if img_tag and 'src' in img_tag.attrs:
            img_url = img_tag['src']
            return img_url
    except Exception as e:
        print(f"[Cover] Error fetching cover for {isbn}: {e}")
    return get_random_placeholder()

# --- Source 1: GlobalMARC Z39.50 ---
def fetch_global_marc(isbn):
    HOST = '54.78.43.234'
    PORT = 210
    INIT_HEX = "b43c830205e0840402e1a220850310000086031000009f6e0750795a333935309f6f1550795a3339353020312e302f5a4f4f4d2076312e349f7003312e30"
    SEARCH_BASE = "b64d8d01008e01018f01009001019103727331b20b9f6908444953434f564552b52da12b06072a8648ce130301a020bf661dbf2c0a30089f7801019f7901079f2d0d39373830313331313033363237"
    PRESENT_HEX = "b81c9f1f037273319e01019d0106b3038001469f68082a8648ce13056d0a"
    
    if len(isbn) != 13 or not isbn.isdigit():
        return None
        
    target_hex = binascii.hexlify(isbn.encode()).decode()
    search_packet = SEARCH_BASE.replace("39373830313331313033363237", target_hex)
    
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(15)
    try:
        s.connect((HOST, PORT))
        s.send(binascii.unhexlify(INIT_HEX))
        s.recv(1024)
        
        s.send(binascii.unhexlify(search_packet))
        s.recv(1024)
        
        s.send(binascii.unhexlify(PRESENT_HEX))
        
        raw_stream = b""
        while True:
            try:
                chunk = s.recv(8192)
                if not chunk: break
                raw_stream += chunk
                if b'</mods>' in chunk or b'\x1d' in chunk: break
            except socket.timeout:
                break
                
        xml_start = raw_stream.find(b'<mods')
        if xml_start != -1:
            xml_data = raw_stream[xml_start:]
            xml_end = xml_data.rfind(b'</mods>')
            if xml_end != -1:
                xml_data = xml_data[:xml_end + 7]
            xml_str = xml_data.decode('utf-8', errors='ignore')
            
            title = re.search(r'<title>(.*?)</title>', xml_str, re.DOTALL)
            author = re.search(r'<namePart>(.*?)</namePart>', xml_str)
            
            if title:
                clean_title = re.sub(r'<[^>]+>', '', title.group(1)).strip()
                clean_author = author.group(1).strip() if author else None
                return {
                    "source_type": "global_marc",
                    "title": clean_title,
                    "author": clean_author,
                    "source_url": f"Z39.50://{HOST}:{PORT}?isbn={isbn}",
                    "category": "books"
                }
    except Exception as e:
        print(f"[GlobalMARC] Error: {e}")
    finally:
        s.close()
    return None

# --- Source 2: SRU MODS ---
SRU_URLS = [
    'https://hvd.alma.exlibrisgroup.com/view/sru/01HVD_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://csu-sjsu.primo.exlibrisgroup.com/view/sru/01CALS_SJO?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://yale.alma.exlibrisgroup.com/view/sru/01YALE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://mit.alma.exlibrisgroup.com/view/sru/01MIT_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5'
] # Trimmed list for brevity in this example. Feel free to extend to full list if necessary.

def fetch_sru(isbn):
    ssl_ctx = ssl.create_default_context()
    ssl_ctx.check_hostname = False
    ssl_ctx.verify_mode = ssl.CERT_NONE
    
    for base_url in SRU_URLS:
        query_str = f'isbn={isbn}'
        url = re.sub(r'query=[^&]*', f'query={query_str}', base_url)
        if 'recordSchema' not in url: 
            url += '&recordSchema=mods'
        else:
            url = re.sub(r'recordSchema=[^&]*', 'recordSchema=mods', url)
            
        try:
            req = urllib.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.urlopen(req, timeout=10, context=ssl_ctx) as resp:
                content = resp.read()
                try:
                    root = ET.fromstring(content)
                except ET.ParseError:
                    continue
                    
                ns = {'srw': 'http://www.loc.gov/zing/srw/', 'mods': 'http://www.loc.gov/mods/v3'}
                mods_node = root.find('.//mods:mods', ns)
                if mods_node is None:
                    mods_node = root.find('.//{http://www.loc.gov/mods/v3}mods')
                if mods_node is None: continue
                
                # Check RDA
                is_rda = any('rda' in (s.text or '').lower() for s in mods_node.findall('.//mods:recordInfo/mods:descriptionStandard', ns))
                if not is_rda: continue
                
                # Check text
                is_text = any('text' in (t.text or '').lower() for t in mods_node.findall('.//mods:typeOfResource', ns))
                if not is_text: continue
                
                # Exclude serial
                if any((i.text or '').lower() in ['serial', 'integrating resource', 'continuing'] for i in mods_node.findall('.//mods:originInfo/mods:issuance', ns)):
                    continue
                    
                # Exclude electronic
                if any((f.text or '').lower() in ['electronic', 'online', 'remote'] for f in mods_node.findall('.//mods:physicalDescription/mods:form', ns)):
                    continue
                    
                # Passed checks, extract metadata
                title_elem = mods_node.find('.//mods:titleInfo/mods:title', ns)
                title = title_elem.text.strip() if title_elem is not None and title_elem.text else "Unknown Title"
                
                author_elem = mods_node.find('.//mods:name/mods:namePart', ns)
                author = author_elem.text.strip() if author_elem is not None and author_elem.text else None
                
                return {
                    "source_type": "sru_mods",
                    "title": title,
                    "author": author,
                    "source_url": url,
                    "category": "books"
                }
        except Exception:
            continue
    return None

# --- Source 3: Custom Socket ---
def fetch_custom_socket(isbn_raw):
    host = '106.13.235.115'
    port = 210
    header = binascii.unhexlify("00ff17db0000")
    
    isbn_clean = isbn_raw.replace('-', '').strip()
    isbn_query = f"{isbn_clean[:6]} {isbn_clean[6:]}" if len(isbn_clean) == 13 else isbn_clean
    query = f"REQU_BEGININIT1@10@ ma_isbn='{isbn_query}'BAG_END"
    payload = header + query.encode('gbk')

    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(10)
            s.connect((host, port))
            s.sendall(payload)
            
            response = b""
            while True:
                chunk = s.recv(8192)
                if not chunk: break
                response += chunk
                if b"BAG_END" in response: break 
            
            if response:
                content = response.decode('gbk', errors='ignore')
                records = re.findall(r'REC_BEGIN(.*?)REC_END', content, re.S)
                
                if records:
                    rec = records[0]
                    title_match = re.search(r'a([\u4e00-\u9fa5]+)', rec)
                    title = title_match.group(1).strip() if title_match else "Unknown Title"
                    
                    return {
                        "source_type": "custom_socket",
                        "title": title,
                        "author": None,
                        "source_url": f"socket://{host}:{port}?query={isbn_query.replace(' ', '%20')}",
                        "category": "books"
                    }
    except Exception as e:
        print(f"[Custom Socket] Error: {e}")
    return None

# --- Main Orchestrator ---
def fetch_unified_metadata(isbn, sources=['global_marc', 'sru_mods', 'custom_socket']):
    print(f"Fetching data for ISBN: {isbn}")
    
    # 1. Fetch cover
    print("-> Fetching cover...")
    image_url = fetch_cover(isbn)
    
    # 2. Fetch metadata
    metadata_records = []
    
    if 'global_marc' in sources:
        print("-> Fetching from GlobalMARC...")
        res = fetch_global_marc(isbn)
        if res: metadata_records.append(res)
        
    if 'sru_mods' in sources:
        print("-> Fetching from SRU MODS...")
        res = fetch_sru(isbn)
        if res: metadata_records.append(res)
        
    if 'custom_socket' in sources:
        print("-> Fetching from Custom Socket...")
        res = fetch_custom_socket(isbn)
        if res: metadata_records.append(res)
        
    # 3. Assemble final Readwise-ready object
    unified_result = {
        "isbn": isbn,
        "image_url": image_url,
        "metadata_records": metadata_records
    }
    
    return unified_result

if __name__ == "__main__":
    # Test execution
    test_isbn = "9787101133974" # Default valid ISBN from previous examples
    result = fetch_unified_metadata(test_isbn)
    print("\n--- Final Unified Result ---")
    print(json.dumps(result, indent=2, ensure_ascii=False))
