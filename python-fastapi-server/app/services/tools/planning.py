import os
import re
import requests
import json
from datetime import datetime
from typing import Dict, Any, List
from dotenv import load_dotenv

from langchain.tools import tool

from ..schemas.planning import BaseTravelDetail

@tool
def price_summation(
        targets: List[BaseTravelDetail]
) -> float:
    """주어진 데이터 목록의 예상 비용(exp_cost) 총합을 계산합니다."""
    total_price = 0.0
    for target in targets:
        total_price += target['exp_cost']
    return total_price

@tool
def tmap_route_optimizer_with_details(
        start_point: Dict[str, Any],
        end_point: Dict[str, Any],
        waypoints: List[Dict[str, Any]],
        start_time: str = None
) -> str:
    """
    출발지, 도착지, 여러 경유지를 입력받아 최적의 방문 순서와 예상 시간/거리를 알려주는 도구입니다.

    Args:
        start_point: 출발지 정보 {'name': '장소명', 'lon': 경도, 'lat': 위도}
        end_point: 도착지 정보 {'name': '장소명', 'lon': 경도, 'lat': 위도}
        waypoints: 경유지 목록 [{'name': '장소명', 'lon': 경도, 'lat': 위도, 'stay_time_seconds': 체류시간(초)}]
        start_time: 출발 시간 'YYYYMMDDHHMM' 형식 (미지정 시 현재 시간)

    Returns:
        최적화된 경로 정보 문자열
    """
    print("\n\ntmap_route_optimizer_with_details tool called\n\n")
    return _execute_tmap_api(start_point, end_point, waypoints, start_time)

def _execute_tmap_api(start_point: Dict[str, Any], end_point: Dict[str, Any], waypoints: List[Dict[str, Any]], start_time: str = None) -> str:
    """TMAP API 실행 공통 함수"""

    # .env 파일 로드
    load_dotenv()
    # .env 파일에서 API 키 읽어오기
    api_key = os.getenv("TMAP_APP_KEY")
    if not api_key:
        return "❌ TMAP_APP_KEY를 찾을 수 없습니다. .env 파일에 API 키가 올바르게 설정되었는지 확인해주세요."

    api_url = "https://apis.openapi.sk.com/tmap/routes/routeOptimization10?version=1"

    headers = {
        "appKey": api_key,
        "Content-Type": "application/json",
        "Accept": "application/json"
    }

    # waypoints를 API가 요구하는 viaPoints 형식으로 변환
    via_points = []
    for i, wp in enumerate(waypoints):
        via_points.append({
            "viaPointId": str(i + 1),
            "viaPointName": wp['name'],
            "viaX": str(wp['lon']),
            "viaY": str(wp['lat']),
            "viaTime": str(wp.get('stay_time_seconds', 60*60))  # 머무는 시간(초), 기본 1시간
        })

    # 출발 시간이 주어지지 않으면 현재 시간으로 설정
    if not start_time:
        start_time = datetime.now().strftime('%Y%m%d%H%M')

    # API 요청 payload 구성
    payload = {
        "startName": start_point['name'],
        "startX": str(start_point['lon']),
        "startY": str(start_point['lat']),
        "endName": end_point['name'],
        "endX": str(end_point['lon']),
        "endY": str(end_point['lat']),
        "startTime": start_time,
        "viaPoints": via_points,
        "reqCoordType": "WGS84GEO",
        "resCoordType": "WGS84GEO",
        "searchOption": 0,  # 교통최적+추천
        "carType": 1,  # 승용차
        "coordinateFlag": "1"  # 경로좌표 요청안함 (응답 크기 줄이기)
    }

    try:
        response = requests.post(api_url, headers=headers, json=payload, timeout=30)

        # 응답 상태 확인
        if response.status_code != 200:
            error_msg = f"❌ API 요청 실패 (상태코드: {response.status_code})"
            try:
                error_detail = response.json()
                # ✅ 특정 오류 코드에 대한 분기 처리 추가
                if 'error' in error_detail and 'code' in error_detail['error']:
                    error_code = error_detail['error'].get('code')
                    point_name = ""
                    if error_code == '1100' and 'message' in error_detail['error']:
                        # 오류 메시지에서 어떤 지점이 문제인지 추론
                        if '출발지' in error_detail['error']['message']:
                            point_name = start_point['name']
                        elif '경유지' in error_detail['error']['message']:
                            # 실제 문제 경유지를 특정하긴 어려우므로 대표적으로 안내
                            point_name = "경유지 중 하나"
                        elif '목적지' in error_detail['error']['message']:
                            point_name = end_point['name']

                        # 에이전트가 이해하기 쉬운 피드백으로 변환
                        return (f"❌ 경로 탐색 실패: '{point_name}' 지점이 자동차로 접근할 수 없는 곳(예: 산 정상, 해변, 탐방로)일 수 있습니다. "
                                f"주차장, 탐방로 입구 등 구체적인 장소 이름으로 다시 시도해주세요.")

                error_msg += f"\n상세 오류: {error_detail}"

            except:
                error_msg += f"\n응답 내용: {response.text[:500]}"
            return error_msg

        data = response.json()

        # 응답 구조 확인 및 파싱
        if 'properties' not in data:
            return f"❌ 예상하지 못한 응답 구조입니다.\n응답: {json.dumps(data, ensure_ascii=False, indent=2)[:1000]}"

        properties = data['properties']

        # 총 거리와 시간 정보 추출
        total_distance_m = int(properties.get('totalDistance', 0))
        total_time_sec = int(properties.get('totalTime', 0))

        total_distance_km = total_distance_m / 1000
        total_hours, remainder = divmod(total_time_sec, 3600)
        total_minutes, _ = divmod(remainder, 60)

        # 최적화된 경로 순서 추출 및 구간별 시간/거리 계산
        route_details = []
        prev_distance = 0
        prev_time = None

        # features에서 Point 타입의 지점들을 순서대로 처리
        if 'features' in data:
            for i, feature in enumerate(data['features']):
                if feature['geometry']['type'] == 'Point':
                    props = feature['properties']
                    point_type = props.get('pointType', '')
                    point_name = props.get('viaPointName', '')
                    arrive_time = props.get('arriveTime', '')
                    complete_time = props.get('completeTime', '')
                    cumulative_distance = int(props.get('distance', 0))
                    delivery_time = int(props.get('deliveryTime', 0))

                    # 구간별 거리 계산
                    segment_distance = cumulative_distance - prev_distance
                    segment_distance_km = segment_distance / 1000

                    # 구간별 시간 계산 (도착시간 기준)
                    if prev_time and arrive_time:
                        try:
                            prev_dt = datetime.strptime(prev_time, '%Y%m%d%H%M%S')
                            curr_dt = datetime.strptime(arrive_time, '%Y%m%d%H%M%S')
                            segment_time_sec = (curr_dt - prev_dt).total_seconds()
                            segment_minutes = int(segment_time_sec // 60)
                        except:
                            segment_minutes = 0
                    else:
                        segment_minutes = 0

                    # 출발지
                    if point_type == 'S':
                        route_details.append(f"📍 **{start_point['name']}** (출발)")
                        prev_time = complete_time
                    # 도착지
                    elif point_type == 'E':
                        if segment_minutes > 0 or segment_distance_km > 0:
                            route_details.append(f"  🚗 이동: 약 **{segment_minutes}분**, **{segment_distance_km:.1f}km**")
                        route_details.append(f"🏁 **{end_point['name']}** (도착)")
                    # 경유지
                    else:
                        # 이동 정보 추가
                        if segment_minutes > 0 or segment_distance_km > 0:
                            route_details.append(f"  🚗 이동: 약 **{segment_minutes}분**, **{segment_distance_km:.1f}km**")

                        # 이름에서 '[숫자] ' 패턴 제거
                        cleaned_name = re.sub(r'^\[\d+\]\s*', '', point_name)

                        # 체류 시간 정보
                        stay_minutes = delivery_time // 60
                        route_details.append(f"📌 **{cleaned_name}** (체류: {stay_minutes}분)")

                        prev_time = complete_time

                    prev_distance = cumulative_distance

        # 결과 포맷팅
        summary = (
            f"🚗 **TMAP 최적 경로 플랜**\n\n"
            f"🕒 **총 예상 소요 시간**: {int(total_hours)}시간 {int(total_minutes)}분 *(이동시간만)*\n"
            f"🛣️ **총 예상 거리**: {total_distance_km:.1f}km\n\n"
            f"✨ **최적화된 방문 순서**:\n"
            f"{chr(10).join(route_details)}\n\n"
            f"💡 *총 소요 시간은 순수 이동 시간이며, 각 장소에서의 체류 시간은 별도 표시됩니다.*"
        )

        return summary

    except requests.exceptions.Timeout:
        return "❌ API 요청 시간 초과. 잠시 후 다시 시도해주세요."
    except requests.exceptions.ConnectionError:
        return "❌ 네트워크 연결 오류. 인터넷 연결을 확인해주세요."
    except requests.exceptions.HTTPError as http_err:
        error_response = http_err.response.text if http_err.response else "응답 없음"
        return f"❌ HTTP 에러 발생: {http_err}\nTMAP 응답: {error_response[:500]}"
    except Exception as e:
        return f"❌ 경로 최적화 중 예상치 못한 오류 발생: {str(e)}"