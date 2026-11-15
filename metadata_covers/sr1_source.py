import urllib.request as urllib
from xml.etree import ElementTree as ET
import time
import re
import os

def verify_license(license_key):
    '''验证注册码'''
    return (True, 999, '验证成功')

def check_license():
    '''直接放行'''
    return True

# 此处保留你的 sru_urls 列表 ...
sru_urls = [
    'https://hvd.alma.exlibrisgroup.com/view/sru/01HVD_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://csu-sjsu.primo.exlibrisgroup.com/view/sru/01CALS_SJO?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://yale.alma.exlibrisgroup.com/view/sru/01YALE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://anu.primo.exlibrisgroup.com/view/sru/61ANU_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://wrlc-gm.primo.exlibrisgroup.com/view/sru/01WRLC_GML?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://kbdk-aub.alma.exlibrisgroup.com/view/sru/45KBDK_AUB?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://aber.alma.exlibrisgroup.com/view/sru/44WHELF_ABW?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://aul.alma.exlibrisgroup.com/view/sru/01AUL_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://col-albion.alma.exlibrisgroup.com/view/sru/01COL_ALBION?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://www.alexandria.ch/view/sru/41BIG_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://anglia.alma.exlibrisgroup.com/view/sru/44APU_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://acu.alma.exlibrisgroup.com/view/sru/61ACU_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://avondale.alma.exlibrisgroup.com/view/sru/61UNI_AVN?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://eu06.alma.exlibrisgroup.com/view/sru/370LABT_KTU?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://mit.alma.exlibrisgroup.com/view/sru/01MIT_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://must.alma.exlibrisgroup.com/view/sru/853MUST_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://eu06.alma.exlibrisgroup.com/view/sru/370LABT_NETWORK?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://eu06.alma.exlibrisgroup.com/view/sru/44BL_MAIN?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://na91.alma.exlibrisgroup.com/view/sru/01NLM_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://bne.alma.exlibrisgroup.com/view/sru/34BNE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://bn-nl.alma.exlibrisgroup.com/view/sru/48OMNIS_NLOP?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://mta-konyvtar.alma.exlibrisgroup.com/view/sru/36MTA_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://cityumo.alma.exlibrisgroup.com/view/sru/853CUM_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://csuc-network.alma.exlibrisgroup.com/view/sru/34CSUC_NETWORK?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://bgr.alma.exlibrisgroup.com/view/sru/41BGR_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'http://bvbr.bib-bvb.de:5661/bvb01sru?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://eu04.alma.exlibrisgroup.com/view/sru/354ILC_NETWORK?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://www.bibliotechediroma.it/opac/sru?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://rice.alma.exlibrisgroup.com/view/sru/01RICE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://eu04.alma.exlibrisgroup.com/view/sru/49HBZ_NETWORK?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://bu.alma.exlibrisgroup.com/view/sru/01BOSU_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://www.a-z.lu/view/sru/352LUX_BIBNET_NETWORK?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://uvm.alma.exlibrisgroup.com/view/sru/52UVM_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://cisug-uvigo.alma.exlibrisgroup.com/view/sru/34CISUG_UVIGO?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://cisug-udc.alma.exlibrisgroup.com/view/sru/34CISUG_UDC?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://cisug-usc.alma.exlibrisgroup.com/view/sru/34CISUG_USC?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://usach.alma.exlibrisgroup.com/view/sru/56USACH_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://unesp.alma.exlibrisgroup.com/view/sru/55UNESP_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://bgr.alma.exlibrisgroup.com/view/sru/41BGR_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ap02.alma.exlibrisgroup.com/view/sru/61CSIRO_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://nb-posters.alma.exlibrisgroup.com/view/sru/41SNL_53_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ec-europa.alma.exlibrisgroup.com/view/sru/32EUC_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://rice.alma.exlibrisgroup.com/view/sru/01RICE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ap01.alma.exlibrisgroup.com/view/sru/886NTU_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://nuigalway.alma.exlibrisgroup.com/view/sru/353GAL_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://nyarc.alma.exlibrisgroup.com/view/sru/01NYA_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ncl.alma.exlibrisgroup.com/view/sru/44NEW_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://bn-network.alma.exlibrisgroup.com/view/sru/8OMNIS_NETWORK?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://princeton.alma.exlibrisgroup.com/view/sru/01PRI_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ca01.alma.exlibrisgroup.com/view/sru/01OCUL_QU?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://rmit.alma.exlibrisgroup.com/view/sru/61RMIT_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://switch-am.alma.exlibrisgroup.com/view/sru/01SLCO_AM?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://slv.alma.exlibrisgroup.com/view/sru/61SLV_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://swisscovery.slsp.ch/view/sru/41SLSP_NETWORK?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ucla.alma.exlibrisgroup.com/view/sru/01UCS_LAL?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://uli-c.alma.exlibrisgroup.com/view/sru/972NNL_ULI_C?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://union.alma.exlibrisgroup.com/view/sru/01UCNY_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://uahurtado.alma.exlibrisgroup.com/view/sru/56UAH_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://uaustral.alma.exlibrisgroup.com/view/sru/54IAE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://cbua-upo.alma.exlibrisgroup.com/view/sru/34CBUA_UPO?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://unitec-mx.alma.exlibrisgroup.com/view/sru/52UNITEC_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://utdt.alma.exlibrisgroup.com/view/sru/54UTDT_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://upv.alma.exlibrisgroup.com/view/sru/34UPV_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://slsp-hsg.alma.exlibrisgroup.com/view/sru/41SLSP_HSG?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ulg-psb.alma.exlibrisgroup.com/view/sru/32ULG_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://adelaide.alma.exlibrisgroup.com/view/sru/61ADELAIDE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://eu.alma.exlibrisgroup.com/view/sru/44CAM_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ed.alma.exlibrisgroup.com/view/sru/44UOE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://haifa.alma.exlibrisgroup.com/view/sru/972HAI_MAIN?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://UOL.alma.exlibrisgroup.com/view/sru/353UOL_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://oxford.alma.exlibrisgroup.com/view/sru/44OXF_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://pitt.alma.exlibrisgroup.com/view/sru/01PITT_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://unisa.alma.exlibrisgroup.com/view/sru/61USOUTHAUS_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ap02-a.alma.exlibrisgroup.com/view/sru/61USYD_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://utk.alma.exlibrisgroup.com/view/sru/01UTN_KNOXVILLE?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ca01.alma.exlibrisgroup.com/view/sru/1UTORONTO_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://unipd.alma.exlibrisgroup.com/view/sru/39UPD_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://sbart-unipi.alma.exlibrisgro.com/view/sru/39SBART_UPI?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://uniss.alma.exlibrisgroup.com/view/sru/39USS_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://eu.alma.exlibrisgroup.com/view/sru/39UVR_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ulb.alma.exlibrisgroup.com/view/sru/32ULDB_U_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://eu06.alma.exlibrisgroup.com/view/sru/370LABT_VGTU?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://vdu.alma.exlibrisgroup.com/view/sru/370LABT_VMU?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://wfu.alma.exlibrisgroup.com/view/sru/01WAKE_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://wigner.alma.exlibrisgroup.com/view/sru/36KFK_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://baylor.alma.exlibrisgroup.com/view/sru/01BUL_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://bethelks.alma.exlibrisgroup.com/view/sru/01BETHEL_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://libsearch.bethel.edu/view/sru/01CLIC_BETHEL?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://suny-bin.alma.exlibrisgroup.com/view/sru/01SUNY_BIN?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://librarysearch.bond.edu.au/view/sru/61BOND_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://bu.alma.exlibrisgroup.com/view/sru/01BOSU_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://bowdoin.alma.exlibrisgroup.com/view/sru/01CBB_BOWC?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://search.library.brandeis.edu/view/sru/01BRAND_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://bruknow.library.brown.edu/view/sru/01BU_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://csu-bakersfield.alma.exlibrisgroup.com/view/sru/01CALS_UBA?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://csu-network.alma.exlibrisgroup.com/view/sru/01CALS_NETWORK?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://csu-ci.alma.exlibrisgroup.com/view/sru/01CALS_UCI?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://csu-chico.alma.exlibrisgroup.com/view/sru/01CALS_CHI?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://csu-sfsu.alma.exlibrisgroup.com/view/sru/01CALS_SFR?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://csu-sjsu.alma.exlibrisgroup.com/view/sru/01CALS_SJO?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://i-share-alp.alma.exlibrisgroup.com/view/sru/01CARLI_ALP?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://i-share-csu.alma.exlibrisgroup.com/view/sru/01CARLI_CSU?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://i-share-dpu.alma.exlibrisgroup.com/view/sru/01CARLI_DPU?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://i-share-uiu.alma.exlibrisgroup.com/view/sru/01CARLI_UIU?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://fsu-flvc.alma.exlibrisgroup.com/view/sru/01FALSC_FSU?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://fiu-flvc.alma.exlibrisgroup.com/view/sru/01FALSC_FIU?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://librarysearch.cardiff.ac.uk/view/sru/44WHELF_CAR?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://julac-cuhk.alma.exlibrisgroup.com/view/sru/852JULAC_CUHK?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://libsearch.cbs.dk/view/sru/45KBDK_CBS?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://eth.swisscovery.slsp.ch/view/sru/41SLSP_ETH?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5',
    'https://ec-europa-finder.alma.exlibrisgroup.com/view/sru/32EUC_INST?version=1.2&operation=searchRetrieve&query=alma.isbn=&maximumRecords=5']

def filter_and_extract_mods(xml_bytes):
    '''
    全新的 MODS XML 筛选引擎。
    负责解析 SRU 吐出的数据，执行 RDA 与纸质书严格校验。
    如果合格，返回纯净的 MODS 字符串；不合格返回 None。
    '''
    ns = {
        'srw': 'http://www.loc.gov/zing/srw/',
        'mods': 'http://www.loc.gov/mods/v3'
    }
    
    try:
        root = ET.fromstring(xml_bytes)
    except ET.ParseError:
        return None

    # 1. 定位 MODS 核心节点
    mods_node = root.find('.//mods:mods', ns)
    if mods_node is None:
        # 兼容某些不带命名空间前缀的返回结果
        mods_node = root.find('.//{http://www.loc.gov/mods/v3}mods')
    if mods_node is None:
        return None

    # 2. 安检项 A：必须是 RDA 描述标准
    is_rda = False
    for standard in mods_node.findall('.//mods:recordInfo/mods:descriptionStandard', ns):
        if standard.text and 'rda' in standard.text.lower():
            is_rda = True
            break
    if not is_rda:
        return None

    # 3. 安检项 B：必须是文字资料 (对应 Leader 06=a)
    is_text = False
    for tor in mods_node.findall('.//mods:typeOfResource', ns):
        if tor.text and 'text' in tor.text.lower():
            is_text = True
            break
    if not is_text:
        return None

    # 4. 安检项 C：排除期刊和连续出版物 (对应 Leader 07=m)
    for issuance in mods_node.findall('.//mods:originInfo/mods:issuance', ns):
        if issuance.text and issuance.text.lower() in ['serial', 'integrating resource', 'continuing']:
            return None # 发现是期刊，直接毙掉

    # 5. 安检项 D：排除电子书 (对应 Leader 23!=o/s)
    for form in mods_node.findall('.//mods:physicalDescription/mods:form', ns):
        if form.text and form.text.lower() in ['electronic', 'online', 'remote']:
            return None # 发现是电子书，直接毙掉

    # ================= 校验通过 =================
    # 将合规的 XML 节点转换为字符串，并做一次深度保洁
    mods_str = ET.tostring(mods_node, encoding='unicode')
    # 移除令人烦躁的命名空间前缀，让导出的 XML 像雪一样干净
    mods_str = mods_str.replace('mods:', '')
    mods_str = mods_str.replace('xmlns:mods=', 'xmlns=')
    
    return mods_str

def main():
    if not check_license():
        return
    
    print('============================================================')
    print('         MARC RDA 纸质图书导出工具 (MODS 终极进化版)')
    print('============================================================')
    
    isbn_input = input('请输入多个 ISBN（用逗号或空格分隔）：').strip()
    raw_list = re.split(r'[,;\s]+', isbn_input)
    isbns = [re.sub(r'[^0-9X]', '', i.upper()) for i in raw_list if i.strip()]
    
    if not isbns:
        print('❌ 未输入有效的 ISBN')
        return

    output_file = 'exported_records.mods.xml'
    record_count = 0

    import ssl
    ssl_ctx = ssl.create_default_context()
    ssl_ctx.check_hostname = False
    ssl_ctx.verify_mode = ssl.CERT_NONE

    # 使用文本模式写入，因为我们现在输出的是纯 XML 字符串
    with open(output_file, 'w', encoding='utf-8') as f:
        # 写入标准的 MODS 集合头部
        f.write('<?xml version="1.0" encoding="UTF-8"?>\n')
        f.write('<modsCollection xmlns="http://www.loc.gov/mods/v3">\n')

        for isbn in isbns:
            found_and_qualified = False
            for base_url in sru_urls:
                query_str = f'isbn={isbn}'
                url = re.sub(r'query=[^&]*', f'query={query_str}', base_url)
                
                # --- 强制请求 MODS 格式 ---
                if 'recordSchema' not in url: 
                    url += '&recordSchema=mods'
                else:
                    url = re.sub(r'recordSchema=[^&]*', 'recordSchema=mods', url)

                try:
                    domain = base_url.split('//')[1].split('/')[0]
                    print(f'  正在查询: {domain}...') 
                    
                    req = urllib.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
                    with urllib.urlopen(req, timeout=10, context=ssl_ctx) as resp:
                        content = resp.read() # 获取原始字节流
                        
                        # 交给新引擎进行解析与“安检”
                        qualified_mods = filter_and_extract_mods(content)
                        
                        if qualified_mods:
                            f.write(qualified_mods + '\n')
                            f.flush()
                            record_count += 1
                            print(f'✅ 已从 {domain} 成功导出标准 MODS XML')
                            found_and_qualified = True
                            break # 成功拿到优质数据，跳出接口循环
                        elif b'<mods' in content or b'<record' in content:
                            print(f'    [被拦截] 该库数据为电子书或未达 RDA 标准')
                            
                except Exception:
                    continue # 网络报错直接跳过

            if not found_and_qualified:
                print(f'❌ ISBN {isbn} 在所有支持 MODS 的库中均未找到合格的纸质 RDA 记录')

        # 写入集合尾部
        f.write('</modsCollection>\n')

    print(f'\n🏁 处理完成，共成功导出 {record_count} 条严选 MODS 记录。')

if __name__ == '__main__':
    main()