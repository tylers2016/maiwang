import os
import json
import random
import csv
import datetime
import urllib.request
import ssl
import html as html_lib
from PIL import Image, ImageOps

# Define directories
BASE_DIR = r"c:\Sword-polish\脉望\daily_review"
CONFIG_FILE = os.path.join(BASE_DIR, "config.json")
TEMPLATE_FILE = os.path.join(BASE_DIR, "template.html")
OUTPUT_FILE = os.path.join(BASE_DIR, "output_card.html")
CSV_FILE = os.path.join(BASE_DIR, "daily-horoscope.csv")
IMAGES_DIR = os.path.join(BASE_DIR, "downloaded_images")
NORMALIZED_IMAGES_FILE = os.path.join(IMAGES_DIR, ".normalized_images.json")

# Constants
HIGHLIGHT_MAX_LEN = 150
IMAGE_EXTENSIONS = ('.png', '.jpg', '.jpeg', '.webp')

def render_category_badges(category):
    chars = [ch for ch in category.strip() if not ch.isspace()]
    if not chars:
        chars = ["摘", "录"]
    return "".join(f"<span>{html_lib.escape(ch)}</span>" for ch in chars)

def trim_near_white_border(img, threshold=240, padding=8):
    rgb = img.convert("RGB")
    mask = ImageOps.grayscale(rgb).point(lambda p: 255 if p < threshold else 0)
    bbox = mask.getbbox()
    if not bbox:
        return img

    left, top, right, bottom = bbox
    left = max(left - padding, 0)
    top = max(top - padding, 0)
    right = min(right + padding, img.width)
    bottom = min(bottom + padding, img.height)
    return img.crop((left, top, right, bottom))

def save_image_in_place(img, path):
    ext = os.path.splitext(path)[1].lower()
    if ext in ('.jpg', '.jpeg'):
        img.convert("RGB").save(path, "JPEG", quality=95, optimize=True)
    elif ext == '.webp':
        img.convert("RGB").save(path, "WEBP", quality=95, method=6)
    else:
        img.save(path)

def image_fingerprint(path):
    stat = os.stat(path)
    return {
        "size": stat.st_size,
        "mtime_ns": stat.st_mtime_ns,
    }

def load_normalized_manifest():
    if not os.path.exists(NORMALIZED_IMAGES_FILE):
        return {}
    try:
        with open(NORMALIZED_IMAGES_FILE, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception:
        return {}

def save_normalized_manifest(manifest):
    with open(NORMALIZED_IMAGES_FILE, 'w', encoding='utf-8') as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

def normalize_downloaded_images():
    if not os.path.exists(IMAGES_DIR):
        return []

    manifest = load_normalized_manifest()
    normalized = []
    manifest_changed = False
    for name in os.listdir(IMAGES_DIR):
        if not name.lower().endswith(IMAGE_EXTENSIONS):
            continue

        path = os.path.join(IMAGES_DIR, name)
        try:
            current_fingerprint = image_fingerprint(path)
            if manifest.get(name) == current_fingerprint:
                continue

            with Image.open(path) as img:
                trimmed = trim_near_white_border(img)
                if trimmed.size != img.size:
                    save_image_in_place(trimmed, path)
                    normalized.append(name)
                    current_fingerprint = image_fingerprint(path)

            manifest[name] = current_fingerprint
            manifest_changed = True
        except Exception as e:
            print(f"Error normalizing {name}: {e}")

    stale_names = [name for name in manifest if not os.path.exists(os.path.join(IMAGES_DIR, name))]
    for name in stale_names:
        manifest.pop(name, None)
        manifest_changed = True

    if manifest_changed:
        save_normalized_manifest(manifest)
    if normalized:
        print(f"Normalized {len(normalized)} downloaded image(s).")
    return normalized

def get_config():
    if not os.path.exists(CONFIG_FILE):
        raise FileNotFoundError(f"Config file not found at {CONFIG_FILE}. Please create it and add 'readwise_token'.")
    with open(CONFIG_FILE, 'r', encoding='utf-8') as f:
        return json.load(f)

def fetch_readwise_highlight(token):
    url = "https://readwise.io/api/v2/review/"
    req = urllib.request.Request(url, headers={"Authorization": f"Token {token}"})
    try:
        context = ssl._create_unverified_context()
        with urllib.request.urlopen(req, context=context) as response:
            data = json.loads(response.read().decode('utf-8'))
            if data and "highlights" in data and len(data["highlights"]) > 0:
                # Pick the first highlight from the daily review
                highlight = data["highlights"][0]
                # In the v2/review/ API, category is often null, but source_type contains "book", "article" etc.
                category = highlight.get("category") or highlight.get("source_type") or "quotes"
                text = highlight.get("text", "")
                
                cat_map = {
                    "books": "书籍",
                    "book": "书籍",
                    "articles": "文章",
                    "article": "文章",
                    "tweets": "网帖",
                    "tweet": "网帖",
                    "podcasts": "播客",
                    "podcast": "播客",
                    "quotes": "摘录"
                }
                category_zh = cat_map.get(category.lower(), "摘录")
                return category_zh, text
            else:
                return "书籍", "今日没有发现回顾数据..."
    except Exception as e:
        print(f"Error fetching Readwise highlight: {e}")
        return "书籍", "无法连接Readwise API获取数据..."

def fetch_lunar_date(date_str):
    url = f"https://api.sou-yun.cn/api/RecommendedReading?type=poem&needHtml=true&index=-1&date={date_str}"
    try:
        context = ssl._create_unverified_context()
        with urllib.request.urlopen(url, context=context) as response:
            data = json.loads(response.read().decode('utf-8'))
            date_field = data.get("Date", "")
            # Example date_field: "丙午 四月初三　5月19日"
            # We want "丙午年四月初三"
            parts = date_field.split()
            if len(parts) >= 2:
                year = parts[0]
                month_day = parts[1]
                return f"{year}年{month_day}"
            return "未知农历"
    except Exception as e:
        print(f"Error fetching lunar date: {e}")
        return "未知农历"

def get_gregorian_date():
    now = datetime.datetime.now()
    
    months = ["一月", "二月", "三月", "四月", "五月", "六月", 
              "七月", "八月", "九月", "十月", "十一月", "十二月"]
    weekdays = ["星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"]
    
    month_str = months[now.month - 1]
    weekday_str = weekdays[now.weekday()]
    day_str = str(now.day)
    
    return month_str, weekday_str, day_str, now.strftime("%Y-%m-%d")

def get_random_fortune():
    if not os.path.exists(CSV_FILE):
        return "宜测试", "CSV文件不存在，请检查路径"
    
    rows = []
    with open(CSV_FILE, 'r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row.get('title') and row.get('content'):
                rows.append(row)
    
    if not rows:
        return "宜补充", "CSV中没有有效数据"
        
    choice = random.choice(rows)
    title = choice['title'].strip()
    content = choice['content'].strip()
    
    # Content must be <= 16 chars as per user request
    if len(content) > 16:
        content = content[:15] + "…"
        
    return title, content

def get_random_image():
    if not os.path.exists(IMAGES_DIR):
        return "", 0, 0
    normalize_downloaded_images()
    images = [f for f in os.listdir(IMAGES_DIR) if f.lower().endswith(IMAGE_EXTENSIONS)]
    if not images:
        return "", 0, 0
    chosen = random.choice(images)
    abs_path = os.path.abspath(os.path.join(IMAGES_DIR, chosen))

    try:
        with Image.open(abs_path) as img:
            width, height = img.size
    except Exception as e:
        print(f"Error reading image size: {e}")
        width, height = 0, 0

    # Convert absolute path to a file URI for HTML
    uri = "file:///" + abs_path.replace("\\", "/")
    return uri, width, height

def generate():
    print("Loading config...")
    config = get_config()
    token = config.get("readwise_token", "")
    
    print("Fetching Readwise Highlight...")
    category, text = fetch_readwise_highlight(token)
    
    # Truncate text if needed
    if len(text) > HIGHLIGHT_MAX_LEN:
        text = text[:HIGHLIGHT_MAX_LEN] + "..."
    
    print("Parsing system date...")
    g_month, g_weekday, g_day, sys_date_str = get_gregorian_date()
    
    print("Fetching Lunar Date...")
    lunar_date = fetch_lunar_date(sys_date_str)
    
    print("Selecting Fortune...")
    f_title, f_content = get_random_fortune()
    
    print("Selecting Image...")
    img_uri, img_width, img_height = get_random_image()
    
    print("Reading HTML template...")
    with open(TEMPLATE_FILE, 'r', encoding='utf-8') as f:
        html = f.read()
    
    print("Injecting data...")
    category_html = render_category_badges(category)
    html = html.replace("__IMAGE_PATH__", img_uri)
    html = html.replace("__IMAGE_WIDTH__", str(img_width))
    html = html.replace("__IMAGE_HEIGHT__", str(img_height))
    html = html.replace("__GREGORIAN_MONTH__", g_month)
    html = html.replace("__GREGORIAN_WEEKDAY__", g_weekday)
    html = html.replace("__LUNAR_DATE__", lunar_date)
    html = html.replace("__GREGORIAN_DAY__", g_day)
    html = html.replace("__FORTUNE_TITLE__", f_title)
    html = html.replace("__FORTUNE_CONTENT__", f_content)
    html = html.replace("__HIGHLIGHT_CATEGORY__", category_html)
    html = html.replace("__HIGHLIGHT_TEXT__", text)
    
    print(f"Writing output to {OUTPUT_FILE}...")
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write(html)
        
    print("Done! You can open output_card.html in your browser.")

if __name__ == "__main__":
    generate()
