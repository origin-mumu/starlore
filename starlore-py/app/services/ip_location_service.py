"""IP 地理位置查询服务。"""

import logging

import httpx

logger = logging.getLogger(__name__)


async def lookup_ip(ip: str | None) -> dict | None:
    """查询 IP 地理位置，返回 {country, province, city}。"""
    if not ip:
        return None
    if ip.startswith("127.") or ip.startswith("192.168.") or ip == "::1":
        return None
    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            resp = await client.get(f"http://ip-api.com/json/{ip}")
            if resp.status_code == 200:
                data = resp.json()
                return {
                    "country": data.get("country", ""),
                    "province": data.get("regionName", ""),
                    "city": data.get("city", ""),
                }
    except Exception as e:
        logger.warning("IP lookup failed for %s: %s", ip, e)
    return None
