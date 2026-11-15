import requests
from bs4 import BeautifulSoup
import os

def fetch_book_cover_refined(isbn):
    url = f"https://isbnsearch.org/isbn/{isbn}"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        
        # 使用 lxml 解析器，它对 XPath 和复杂层级支持更好
        soup = BeautifulSoup(response.content, 'lxml')

        # 方案 A：按照你提供的层级进行选择 (body > div > div > div > img)
        # select_one 使用 CSS 选择器语法，:nth-of-type(1) 对应 XPath 的 [1]
        img_tag = soup.select_one('body > div:nth-of-type(1) > div:nth-of-type(1) > div:nth-of-type(1) > img')

        if img_tag and 'src' in img_tag.attrs:
            img_url = img_tag['src']
            alt_text = img_tag.get('alt', 'No Title')
            print(f"找到封面: {alt_text}")
            print(f"图片链接: {img_url}")

            # 下载并保存
            img_res = requests.get(img_url, headers=headers)
            if not os.path.exists('covers'):
                os.makedirs('covers')
            
            with open(f"covers/{isbn}.jpg", 'wb') as f:
                f.write(img_res.content)
            print(f"保存成功！")
        else:
            print("未能根据指定路径定位到图片，请检查页面结构是否发生变动。")

    except Exception as e:
        print(f"抓取失败: {e}")

if __name__ == "__main__":
    fetch_book_cover_refined("9787101133974")