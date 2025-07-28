import requests
import json

def test_java_api():
    """Python에서 Java Spring Boot API 테스트"""
    
    # Java 서버 URL
    base_url = "http://localhost:8080/api/travel"
    
    # 테스트 데이터
    test_pks = [1, 2, 3, 4, 5]
    
    print("Python에서 Java 서비스 테스트 시작")
    print("=" * 50)
    
    # 1. Places API 테스트
    print("\n1. Places API 테스트")
    try:
        response = requests.post(
            f"{base_url}/places",
            headers={"Content-Type": "application/json"},
            json={"pks": test_pks},
            timeout=10
        )
        print(f"상태코드: {response.status_code}")
        print(f"응답 데이터: {response.json()}")
    except Exception as e:
        print(f"Places API 오류: {e}")
    
    # 2. Restaurants API 테스트
    print("\n2. Restaurants API 테스트")
    try:
        response = requests.post(
            f"{base_url}/restaurants",
            headers={"Content-Type": "application/json"},
            json={"pks": test_pks},
            timeout=10
        )
        print(f"상태코드: {response.status_code}")
        print(f"응답 데이터: {response.json()}")
    except Exception as e:
        print(f" Restaurants API 오류: {e}")
    
    # 3. Accommodations API 테스트
    print("\n3. Accommodations API 테스트")
    try:
        response = requests.post(
            f"{base_url}/accommodations",
            headers={"Content-Type": "application/json"},
            json={"pks": test_pks},
            timeout=10
        )
        print(f"상태코드: {response.status_code}")
        print(f"응답 데이터: {response.json()}")
    except Exception as e:
        print(f" Accommodations API 오류: {e}")
    
    print("\n" + "=" * 50)
    print("테스트 완료!")

if __name__ == "__main__":
    test_java_api()