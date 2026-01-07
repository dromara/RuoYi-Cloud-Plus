import argparse
import requests
import sys
import os
import time

# 设置请求超时时间
TIMEOUT = 10

def get_access_token(base_url, username, password):
    """
    获取鉴权 Token
    """
    login_url = f"{base_url}/nacos/v1/auth/users/login"
    try:
        response = requests.post(login_url, data={
            "username": username,
            "password": password
        }, timeout=TIMEOUT)
        if response.status_code == 200:
            return response.json().get("accessToken")
        else:
             print(f"[!] 登录请求异常: {str(e)}")
             sys.exit(1)
        return None
    except Exception as e:
        print(f"[!] 登录请求异常: {str(e)}")
        sys.exit(1)

def get_config_list(base_url, namespace_id, token):
    """
    分页获取 Nacos 中所有的配置列表
    """
    url = f"{base_url}/nacos/v1/cs/configs"
    page_no = 1
    page_size = 100 # Nacos 默认每页最大通常支持到几百
    all_configs = []

    print("🔍 正在从 Nacos 获取配置列表...")

    while True:
        params = {
            "pageNo": page_no,
            "pageSize": page_size,
            "search": "accurate", # 精确搜索模式
            "dataId": "",
            "group": "",
            "tenant": namespace_id
        }

        headers = {}
        if token:
            headers["accessToken"] = token

        try:
            resp = requests.get(url, params=params, headers=headers, timeout=TIMEOUT)
            if resp.status_code != 200:
                print(f"❌ 获取列表失败 (Page {page_no}): {resp.text}")
                break

            data = resp.json()
            page_items = data.get("pageItems", [])

            if not page_items:
                break

            all_configs.extend(page_items)
            print(f"   已获取第 {page_no} 页，本页 {len(page_items)} 条...")

            # 如果当前页不满 page_size，说明是最后一页
            if len(page_items) < page_size:
                break

            page_no += 1

        except Exception as e:
            print(f"❌ 获取列表异常: {str(e)}")
            break

    print(f"✅ 共发现 {len(all_configs)} 个配置项。")
    return all_configs

def publish_config(base_url, data_id, group, content, namespace_id, config_type, token):
    """
    上传配置
    """
    url = f"{base_url}/nacos/v1/cs/configs"

    payload = {
        "dataId": data_id,
        "group": group,
        "content": content,
        "type": config_type,
        "tenant": namespace_id
    }

    headers = {}
    if token:
        headers["accessToken"] = token

    try:
        resp = requests.post(url, data=payload, headers=headers, timeout=TIMEOUT)
        if resp.status_code == 200 and resp.text == "true":
            return True, "Success"
        else:
            return False, resp.text
    except Exception as e:
        return False, str(e)

def main():
    parser = argparse.ArgumentParser(description="Nacos 批量匹配导入工具 (根据API返回查找本地文件)")

    parser.add_argument("--dir", required=True, default="./config" help="包含配置文件的本地目录路径")
    parser.add_argument("--host", required=True, default="http://127.0.0.1:8848", help="Nacos 地址")

    parser.add_argument("--namespace", default="dev", help="Namespace ID (默认 public)")
    parser.add_argument("-u", "--username", default="nacos", help="用户名")
    parser.add_argument("-p", "--password", default="nacos", help="密码")

    args = parser.parse_args()

    # 1. 检查本地目录
    if not os.path.isdir(args.dir):
        print(f"❌ 错误: 目录不存在 '{args.dir}'")
        sys.exit(1)

    base_url = args.host.rstrip("/")

    # 2. 登录
    token = get_access_token(base_url, args.username, args.password)

    # 3. 获取 Nacos 服务端现有配置列表
    config_list = get_config_list(base_url, args.namespace, token)

    if not config_list:
        print("⚠️  Nacos 中没有配置，无需操作。")
        return

    # 4. 遍历列表并在本地查找文件
    print(f"\n🚀 开始匹配本地文件并上传 (目录: {args.dir})...\n" + "-"*50)

    success_count = 0
    skip_count = 0
    fail_count = 0

    for cfg in config_list:
        data_id = cfg.get("dataId")
        group = cfg.get("group")
        # Nacos API 返回的 type 可能是 null，默认为 yaml 或 text
        config_type = cfg.get("type") or "yaml"

        # 假设文件名直接等于 DataID (例如 dataId="application.yml", 文件名就是 "application.yml")
        file_path = os.path.join(args.dir, data_id)

        # 尝试查找文件
        if os.path.exists(file_path):
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()

                # 执行上传
                is_ok, msg = publish_config(base_url, data_id, group, content, args.namespace, config_type, token)

                if is_ok:
                    print(f"✅ [更新] DataID: {data_id:<30} | Group: {group} | Type: {config_type}")
                    success_count += 1
                else:
                    print(f"❌ [失败] DataID: {data_id:<30} | Msg: {msg}")
                    fail_count += 1
            except Exception as read_err:
                 print(f"❌ [错误] 读取文件 {data_id} 失败: {str(read_err)}")
                 fail_count += 1
        else:
            # 本地没有这个文件，跳过
            print(f"⚪ [跳过] 本地未找到对应文件: {data_id}")
            skip_count += 1

    print("-" * 50)
    print(f"🏁 完成。成功: {success_count} | 跳过: {skip_count} | 失败: {fail_count}")

if __name__ == "__main__":
    main()
