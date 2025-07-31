import requests
import json
import csv
import os
import time
from dotenv import load_dotenv # .env 파일에서 환경 변수를 로드하기 위함

# .env 파일에서 환경 변수 로드
load_dotenv()

# Google Places API 키를 환경 변수에서 가져옵니다.
# 실제 API 키는 코드에 직접 노출하지 않고 환경 변수로 관리하는 것이 보안상 안전합니다.

GOOGLE_PLACES_API_KEY = os.getenv("GOOGLE_PLACES_API_KEY")

if not GOOGLE_PLACES_API_KEY:
    print("오류: GOOGLE_PLACES_API_KEY 환경 변수가 설정되지 않았습니다.")
    print(".env 파일에 GOOGLE_PLACES_API_KEY=YOUR_API_KEY 를 추가해주세요.")
    exit()

# --- 1. Google Places API에서 데이터 가져오기 ---
def search_places(query: str, api_key: str, language: str = 'ko', place_type: str = '', next_page_token: str = None) -> dict:
    """
    Google Places Text Search API를 사용하여 장소를 검색합니다.
    :param query: 검색할 텍스트 (예: "서울 맛집")
    :param api_key: Google Places API 키
    :param language: 결과 언어 (기본값: 한국어)
    :return: API 응답 JSON (딕셔너리 형태)
    """
    base_url = "https://maps.googleapis.com/maps/api/place/textsearch/json"
    params = {
        "query": query,
        "language": language,
        "key": api_key
        }
    if place_type:
        params["type"] = place_type # 장소 유형 파라미터 추가
    if next_page_token:
        params["pagetoken"] = next_page_token # 다음 페이지 토큰 추가

    print(f"DEBUG: API 요청 URL: {base_url}?query={query}&language={language}&key=YOUR_API_KEY_HIDDEN")

    try:
        response = requests.get(base_url, params=params)
        response.raise_for_status() # HTTP 오류 발생 시 예외 발생
        # print(json.dumps(response.json(), indent=2, ensure_ascii=False))
        return response.json()
    except requests.exceptions.HTTPError as http_err:
        print(f"HTTP 오류 발생: {http_err}")
        print(f"응답 본문: {response.text}")
        return {}
    except requests.exceptions.ConnectionError as conn_err:
        print(f"연결 오류 발생: {conn_err}")
        return {}
    except requests.exceptions.Timeout as timeout_err:
        print(f"타임아웃 오류 발생: {timeout_err}")
        return {}
    except requests.exceptions.RequestException as req_err:
        print(f"알 수 없는 요청 오류 발생: {req_err}")
        return {}

def get_place_details(place_id: str, api_key: str, language: str = 'ko') -> dict:
    """
    Google Places Details API를 사용하여 특정 장소의 상세 정보를 가져옴.
    특히 'reviews' 필드를 요청
    :param place_id: 장소의 고유 ID
    :param api_key: Google Places API 키
    :param language: 결과 언어 (기본: 한국어)
    :return: Place Details API 응답 JSON (딕셔너리 형태)
    """
    base_url = "https://maps.googleapis.com/maps/api/place/details/json"
    fields = "name,formatted_address,geometry,rating,user_ratings_total,place_id,reviews"

    params = {
        "place_id" : place_id,
        "fields" : fields,
        "language" : language,
        "key" : api_key
    }
    print(f"DEBUG: Place Details 요청 URL: {base_url}?place_id={place_id}&fields={fields}&key=YOUR_API_KEY_HIDDEN")

    try:
        response = requests.get(base_url, params=params)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"Place Details API 호출 오류: {e}")
        return {}

# --- 2. 데이터 추출 및 가공 ---
def extract_place_data(place_json: dict) -> dict:
    """
    단일 장소 JSON 객체에서 필요한 데이터를 추출합니다.
    :param place_json: Google Places API의 단일 장소 JSON 객체
    :return: 추출된 데이터를 담은 딕셔너리
     """

    name = place_json.get('name', 'N/A')
    address = place_json.get('formatted_address', 'N/A')
    latitude = 'N/A'
    longitude = 'N/A'

    if 'geometry' in place_json and 'location' in place_json['geometry']:
        latitude = place_json['geometry']['location'].get('lat', 'N/A')
        longitude = place_json['geometry']['location'].get('lng', 'N/A')

    rating = place_json.get('rating', 'N/A')
    place_id = place_json.get('place_id', 'N/A') # Place Details API 호출 시 필요할 수 있음
    review_texts = [] # 리뷰 텍스트를 저장할 리스트

    reviews = place_json.get('reviews', [])

    # Google Places API는 일반적으로 Places Details 요청 시 1~5개 정도의 리뷰만 반환(비공개 알고리즘에 따라 개수가 다름)
    for i, review in enumerate(reviews):
        review_text = review.get('text', '')
        if review_text: # 빈 리뷰 텍스트는 건너 뜀.
            review_texts.append(review_text)

    # 리뷰 텍스트는 각 리뷰를 구분하기 위한 구분자로 연결하여 저장
    # 이 데이터는 나중에 RDB의 텍스트 컬럼에 저장될 수 있음.
    reviews_str = " ||| ".join(review_text) if review_texts else "N/A"

    return {
        "이름": name,
        "주소": address,
        "위도": latitude,
        "경도": longitude,
        "평점": rating,
        "장소_ID": place_id,
        "리뷰" : reviews_str # 리뷰 텍스트 컬럼
    }


# --- 3. CSV 파일로 저장 ---

def save_to_csv(data: list[dict], filename: str = 'places_data.csv'):
    """
    추출된 장소 데이터를 CSV 파일로 저장합니다.
    :param data: 추출된 장소 데이터 딕셔너리 리스트
    :param filename: 저장할 CSV 파일 이름
    """

    if not data:
        print("저장할 데이터가 없습니다.")
        return

    # CSV 헤더 (첫 번째 딕셔너리의 키를 사용)
    fieldnames = data[0].keys()

    try:
        with open(filename, 'w', newline='', encoding='utf-8') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader() # 헤더 쓰기
            writer.writerows(data) # 모든 데이터 행 쓰기
        print(f"데이터가 '{filename}' 파일에 성공적으로 저장되었습니다.")
    except IOError as e:
        print(f"CSV 파일 저장 중 오류 발생: {e}")

# --- 메인 실행 로직 ---
if __name__ == "__main__":
    search_query = "부산에 분위기 좋은 카페" # 검색할 쿼리
    place_type_filter = "cafe" # 카페 유형으로 제한 (바다뷰 카페에 더 적합)

    print(f"'{search_query}'에 대한 장소 정보를 검색합니다...")

    # 1단계: Text Search로 기본 정보 가져오기
    places_response = search_places(search_query, GOOGLE_PLACES_API_KEY, place_type=place_type_filter)

    all_extracted_data = []
    current_page_token = None
    page_count = 0
    max_pages = 3

while page_count < max_pages:
    # 1단계: Text Search로 기본 정보 및 place_id 가져오기 (페이지 토큰 사용)
    places_response = search_places(search_query, GOOGLE_PLACES_API_KEY,
                                    place_type=place_type_filter,
                                    next_page_token=current_page_token)

    if not places_response or places_response.get('status') != 'OK':
        print(f"API 응답 실패 또는 상태 오류: {places_response.get('status', 'N/A')}")
        print(f"에러 메시지: {places_response.get('error_message', '없음')}")
        break # 오류 발생 시 루프 종료

    initial_results = places_response.get('results', [])
    if not initial_results:
        print(f"페이지 {page_count + 1}에 검색 결과가 없습니다. 더 이상 가져올 결과가 없거나 오류가 발생했습니다.")
        break # 현재 페이지에 결과가 없으면 루프 종료

    print(f"페이지 {page_count + 1}: 총 {len(initial_results)}개의 초기 검색 결과를 찾았습니다. 상세 정보를 가져옵니다...")
    for i, place in enumerate(initial_results):
        place_id = place.get('place_id')
        if place_id:
            print(f"  [{i+1}/{len(initial_results)}] '{place.get('name', 'N/A')}'의 상세 정보 가져오는 중...")
            # 2단계: 각 place_id에 대해 Place Details API 호출
            details_response = get_place_details(place_id, GOOGLE_PLACES_API_KEY)

            if details_response and details_response.get('status') == 'OK':
                detailed_place_data = details_response.get('result', {})
                if detailed_place_data:
                    # 3단계: 상세 정보에서 데이터 추출
                    extracted_item = extract_place_data(detailed_place_data)
                    all_extracted_data.append(extracted_item)
                else:
                    print(f"    경고: '{place.get('name', 'N/A')}' (ID: {place_id})의 상세 결과가 비어있습니다.")
            else:
                print(f"    경고: '{place.get('name', 'N/A')}' (ID: {place_id})의 상세 정보 API 호출 실패. 상태: {details_response.get('status', 'N/A')}")
        else:
            print(f"  경고: 장소 ID가 없는 결과가 있습니다: {place.get('name', 'N/A')}")

        # API 호출 간 짧은 지연을 두어 API 할당량 초과 방지 및 안정성 확보
        time.sleep(0.1)

        # 다음 페이지 토큰 업데이트
    current_page_token = places_response.get('next_page_token')
    page_count += 1

    if current_page_token and page_count < max_pages:
        print(f"\n다음 페이지 토큰을 발견했습니다. {page_count}초 대기 후 다음 페이지를 가져옵니다...")
        time.sleep(2) # Google API 정책상 다음 페이지 요청 시 최소 2초 딜레이 필요
    else:
        print("\n더 이상 가져올 페이지가 없거나 최대 페이지 수에 도달했습니다.")
        break # 다음 페이지 토큰이 없거나 최대 페이지 수에 도달하면 루프 종료

    # 모든 추출된 데이터를 CSV로 저장
if all_extracted_data:
    save_to_csv(all_extracted_data, f"{search_query.replace(' ', '_')}_with_reviews_data.csv")
else:
    print("추출된 상세 장소 데이터가 없습니다.")
