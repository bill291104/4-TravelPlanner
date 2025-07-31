import csv
import os
import time
from dotenv import load_dotenv
import mysql.connector
from mysql.connector import Error

# .env 파일에서 환경 변수 로드
load_dotenv()

# MySQL 데이터베이스 접속 정보
DB_HOST = os.getenv("MYSQL_HOST")
DB_DATABASE = os.getenv("MYSQL_DATABASE")
DB_USER = os.getenv("MYSQL_USER")
DB_PASSWORD = os.getenv("MYSQL_PASSWORD")

# CSV 파일들이 저장된 디렉토리 경로
CSV_DIRECTORY_PATH = "csv_data"

def create_connection():
    """MySQL 데이터베이스 연결을 생성합니다."""
    connection = None
    try:
        connection = mysql.connector.connect(
            host=DB_HOST,
            database=DB_DATABASE, # 지정된 데이터베이스에 직접 연결
            user=DB_USER,
            password=DB_PASSWORD
        )
        if connection.is_connected():
            print(f"MySQL 데이터베이스 '{DB_DATABASE}'에 성공적으로 연결되었습니다.")
        return connection
    except Error as e:
        print(f"MySQL 데이터베이스 연결 오류: {e}")
        return None

def create_tables(connection):
    """
    place 및 place_review 테이블을 생성합니다.
    테이블이 이미 존재하면 생성하지 않습니다.
    이 함수는 초기 테이블 구조를 정의하며, 컬럼 추가 및 제약 조건은 add_column/add_unique_constraint에서 처리합니다.
    """
    cursor = connection.cursor()
    # place 테이블 생성 쿼리 (description 컬럼 포함, google_place_id 및 복합 UNIQUE는 별도 함수에서 추가)
    place_table_creation_query = """
                                 CREATE TABLE IF NOT EXISTS place (
                                                                      place_id INT AUTO_INCREMENT PRIMARY KEY,
                                                                      place_name VARCHAR(255) NOT NULL,
                                     description TEXT, -- 장소에 대한 설명 컬럼
                                     lat DECIMAL(10, 8),
                                     lon DECIMAL(11, 8),
                                     address VARCHAR(512),
                                     google_place_id VARCHAR(255), -- UNIQUE 제약 조건은 add_unique_constraint에서 추가
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     embeded_at TIMESTAMP,
                                     travel_style_id INT DEFAULT 0 -- 필요에 따라 추가
                                     ); \
                                 """
    # place_review 테이블 생성 쿼리 (place_id를 외래 키로 참조, rating 컬럼 유지)
    review_table_creation_query = """
                                  CREATE TABLE IF NOT EXISTS place_review (
                                                                              review_id INT AUTO_INCREMENT PRIMARY KEY,
                                                                              place_id INT NOT NULL, -- place 테이블의 place_id를 참조
                                                                              rating DECIMAL(2,1), -- 평점은 DECIMAL(2,1)로 변경
                                      comment TEXT,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      embeded_at TIMESTAMP,
                                      FOREIGN KEY (place_id) REFERENCES place(place_id) ON DELETE CASCADE
                                      ); \
                                  """
    try:
        cursor.execute(place_table_creation_query)
        print("테이블 'place'가 성공적으로 생성되었거나 이미 존재합니다.")
        cursor.execute(review_table_creation_query)
        print("테이블 'place_review'가 성공적으로 생성되었거나 이미 존재합니다.")
        connection.commit()
    except Error as e:
        print(f"테이블 생성 오류: {e}")
    finally:
        cursor.close()

def add_column(connection, table_name: str, column_name: str, data_type: str, default_value: str = None):
    """
    지정된 테이블에 새로운 컬럼을 추가합니다.
    컬럼이 이미 존재하면 건너뜁니다.
    :param connection: MySQL 연결 객체
    :param table_name: 컬럼을 추가할 테이블의 이름
    :param column_name: 추가할 컬럼의 이름
    :param data_type: 추가할 컬럼의 데이터 타입 (예: VARCHAR(255), INT, TEXT)
    :param default_value: 컬럼의 기본값 (선택 사항)
    """
    cursor = connection.cursor()
    try:
        # 컬럼 존재 여부 확인
        cursor.execute(f"""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = '{DB_DATABASE}' AND TABLE_NAME = '{table_name}' AND COLUMN_NAME = '{column_name}';
        """)
        if cursor.fetchone()[0] == 0:
            default_clause = f" DEFAULT '{default_value}'" if default_value is not None else ""
            alter_table_query = f"""
            ALTER TABLE {table_name}
            ADD COLUMN {column_name} {data_type}{default_clause};
            """
            cursor.execute(alter_table_query)
            connection.commit()
            print(f"테이블 '{table_name}'에 컬럼 '{column_name}'이(가) 성공적으로 추가되었습니다.")
        else:
            print(f"컬럼 '{column_name}'이(가) 테이블 '{table_name}'에 이미 존재합니다. 건너뜁니다.")
    except Error as e:
        # MySQL Error Code 1060은 "Duplicate column name"을 의미합니다.
        if e.errno == 1060:
            print(f"컬럼 '{column_name}'이(가) 테이블 '{table_name}'에 이미 존재합니다. 건너뜜니다.")
        else:
            print(f"컬럼 추가 오류 (테이블: {table_name}, 컬럼: {column_name}): {e}")
    finally:
        cursor.close()

def drop_column(connection, table_name: str, column_name: str):
    """
    지정된 테이블에서 컬럼을 삭제합니다.
    컬럼이 존재하지 않으면 건너뜁니다.
    """
    cursor = connection.cursor()
    try:
        # 컬럼 존재 여부 확인
        cursor.execute(f"""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = '{DB_DATABASE}' AND TABLE_NAME = '{table_name}' AND COLUMN_NAME = '{column_name}';
        """)
        if cursor.fetchone()[0] > 0:
            alter_table_query = f"""
            ALTER TABLE {table_name} DROP COLUMN {column_name};
            """
            cursor.execute(alter_table_query)
            connection.commit()
            print(f"테이블 '{table_name}'에서 컬럼 '{column_name}'이(가) 성공적으로 삭제되었습니다.")
        else:
            print(f"컬럼 '{column_name}'이(가) 테이블 '{table_name}'에 존재하지 않습니다. 건너뜁니다.")
    except Error as e:
        print(f"컬럼 삭제 오류 (테이블: {table_name}, 컬럼: {column_name}): {e}")
    finally:
        cursor.close()

def add_unique_constraint(connection, table_name: str, constraint_name: str, columns: list[str]):
    """
    지정된 테이블에 UNIQUE 제약 조건을 추가합니다.
    제약 조건이 이미 존재하면 건너뜁니다.
    """
    cursor = connection.cursor()
    try:
        # 제약 조건 존재 여부 확인
        cursor.execute(f"""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = '{DB_DATABASE}' AND TABLE_NAME = '{table_name}' AND CONSTRAINT_TYPE = 'UNIQUE' AND CONSTRAINT_NAME = '{constraint_name}';
        """)
        if cursor.fetchone()[0] == 0:
            # address(255)와 같은 표현은 INDEX 생성 시에만 유효하며, CONSTRAINT 추가 시에는 컬럼 이름만 사용합니다.
            columns_for_constraint = [col.split('(')[0] if '(' in col else col for col in columns]
            columns_str_for_constraint = ", ".join(columns_for_constraint)

            alter_table_query = f"""
            ALTER TABLE {table_name} ADD CONSTRAINT {constraint_name} UNIQUE ({columns_str_for_constraint});
            """
            cursor.execute(alter_table_query)
            connection.commit()
            print(f"테이블 '{table_name}'에 UNIQUE 제약 조건 '{constraint_name}'이(가) 성공적으로 추가되었습니다.")
        else:
            print(f"UNIQUE 제약 조건 '{constraint_name}'이(가) 테이블 '{table_name}'에 이미 존재합니다. 건너뜁니다.")
    except Error as e:
        # MySQL Error Code 1061은 "Duplicate key name"을 의미합니다.
        if e.errno == 1061:
            print(f"UNIQUE 제약 조건 '{constraint_name}'이(가) 테이블 '{table_name}'에 이미 존재합니다. 건너뜜니다.")
        else:
            print(f"UNIQUE 제약 조건 추가 오류 (테이블: {table_name}, 제약 조건: {constraint_name}): {e}")
    finally:
        cursor.close()


def insert_data_from_csv(connection, csv_file_path):
    """
    CSV 파일에서 데이터를 읽어 place 및 place_review 테이블에 삽입합니다.
    """
    cursor = connection.cursor()

    # place 테이블 삽입 쿼리 (description 컬럼 포함, hashtags 컬럼 제거)
    # google_place_id 또는 (place_name, address) 조합이 중복될 경우 업데이트
    place_insert_query = """
                         INSERT INTO place (place_name, description, lat, lon, address, google_place_id)
                         VALUES (%s, %s, %s, %s, %s, %s)
                             ON DUPLICATE KEY UPDATE
                                                  place_name = VALUES(place_name),
                                                  description = VALUES(description),
                                                  lat = VALUES(lat),
                                                  lon = VALUES(lon),
                                                  address = VALUES(address),
                                                  google_place_id = VALUES(google_place_id); \
                         """

    # place_review 테이블 삽입 쿼리
    review_insert_query = """
                          INSERT INTO place_review (place_id, rating, comment)
                          VALUES (%s, %s, %s); \
                          """

    try:
        with open(csv_file_path, 'r', newline='', encoding='utf-8') as file:
            reader = csv.DictReader(file)

            total_places_inserted = 0
            total_places_updated = 0
            total_reviews_inserted = 0

            for row in reader:
                google_place_id = row.get('장소_ID')
                place_name = row.get('이름')
                address = row.get('주소')
                # 'N/A' 값 처리: float으로 변환할 수 없으므로 None으로 설정
                latitude = float(row.get('위도')) if row.get('위도') not in ['N/A', '', None] else None
                longitude = float(row.get('경도')) if row.get('경도') not in ['N/A', '', None] else None
                rating = float(row.get('평점')) if row.get('평점') not in ['N/A', '', None] else None
                reviews_str = row.get('리뷰') # CSV에서 리뷰 텍스트 가져옴
                place_description = row.get('설명') # CSV에서 '설명' 컬럼 가져옴

                # 1. place 테이블에 데이터 삽입 또는 업데이트 (description 컬럼 포함)
                # 컬럼 순서: place_name, description, lat, lon, address, google_place_id
                place_data = (place_name, place_description, latitude, longitude, address, google_place_id)
                try:
                    cursor.execute(place_insert_query, place_data)

                    if cursor.rowcount == 1: # INSERT (새로운 행이 삽입됨)
                        total_places_inserted += 1
                        place_db_id = cursor.lastrowid # 새로 삽입된 행의 ID
                    else: # UPDATE (기존 행이 업데이트되거나 변경 없이 중복됨)
                        # 업데이트된 경우 또는 변경 없는 중복인 경우, 기존 place_id를 찾아야 합니다.
                        # google_place_id 또는 (place_name, address)로 조회합니다.
                        cursor.execute("""
                                       SELECT place_id FROM place
                                       WHERE google_place_id = %s
                                          OR (place_name = %s AND address = %s)
                                       """, (google_place_id, place_name, address))
                        result = cursor.fetchone()
                        if result:
                            place_db_id = result[0]

                            print(f"[중복됨] '{place_name}' (google_place_id={google_place_id}) → 기존 place_id={place_db_id}")

                            # cursor.rowcount가 2이면 실제 업데이트가 발생한 경우
                            if cursor.rowcount == 2:
                                total_places_updated += 1
                        else:
                            print(f"  경고: 장소 '{place_name}' (ID: {google_place_id})에 대한 place_id를 찾을 수 없습니다. 리뷰 삽입 건너뜁니다.")
                            continue # place_id를 찾을 수 없으면 리뷰 삽입 건너뛰기

                    # 2. place_review 테이블에 데이터 삽입 (리뷰가 있는 경우)
                    if reviews_str and reviews_str != 'N/A':
                        # 리뷰 텍스트를 ' | ' 구분자로 분리합니다. (CSV 스크립트의 구분자에 맞춤)
                        individual_reviews = reviews_str.split(' ||| ')
                        for review_comment in individual_reviews:
                            if review_comment.strip(): # 빈 리뷰는 건너뜁니다.
                                review_data = (place_db_id, rating, review_comment.strip()) # rating은 place_review에 들어감
                                try:
                                    cursor.execute(review_insert_query, review_data)
                                    total_reviews_inserted += 1
                                except Error as review_err:
                                    # 외래 키 제약 조건 오류 등 리뷰 삽입 실패 시
                                    print(f"    [오류] 리뷰 삽입 오류 (장소 ID: {google_place_id}, 리뷰: '{review_comment[:30]}...'): {review_err}")
                                    # 개별 리뷰 삽입 실패는 전체 트랜잭션을 롤백하지 않고 다음 리뷰로 진행
                except Error as place_err:
                    print(f"  [오류] 장소 삽입/업데이트 오류 (장소 ID: {google_place_id}, 이름: '{place_name}'): {place_err}")
                    connection.rollback() # 장소 삽입 오류 시 해당 트랜잭션 롤백 (데이터 일관성 유지)

        connection.commit() # 모든 작업 완료 후 최종 커밋
        print(f"\nCSV 파일 '{csv_file_path}'에서 데이터 로드 완료.")
        print(f"  총 삽입된 장소 레코드: {total_places_inserted}개")
        print(f"  총 업데이트된 장소 레코드: {total_places_updated}개")
        print(f"  총 삽입된 리뷰 레코드: {total_reviews_inserted}개")

    except FileNotFoundError:
        print(f"  오류: '{csv_file_path}' 파일을 찾을 수 없습니다. 파일 경로를 확인해주세요.")
    except Exception as e:
        print(f"데이터 삽입 중 예상치 못한 오류 발생: {e}")
        connection.rollback() # 예상치 못한 오류 발생 시 전체 롤백
    finally:
        cursor.close()

if __name__ == "__main__":
    # 환경 변수 유효성 검사
    if not all([DB_HOST, DB_DATABASE, DB_USER, DB_PASSWORD]):
        print("오류: MySQL 접속 환경 변수(MYSQL_HOST, MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD)가 모두 설정되지 않았습니다.")
        print(".env 파일을 확인해주세요.")
        exit()

    connection = create_connection()
    if connection:
        # 1. 테이블 생성 (이미 존재하면 건너뛰고, 스키마 변경이 있다면 적용 시도)
        create_tables(connection)

        # # 2. 기존 테이블 스키마 보정: 잘못된 컬럼 삭제 및 누락된 컬럼/제약 조건 추가
        # print("\n--- 기존 테이블 스키마 보정 시작 ---")
        # # place 테이블에서 rating 컬럼이 있다면 삭제 (이전 요청에 따라)
        # drop_column(connection, "place", "rating")
        # # place 테이블에서 hashtags 컬럼이 있다면 삭제 (이번 요청에 따라)
        # drop_column(connection, "place", "hashtags")
        #
        # # place 테이블에 google_place_id 컬럼이 없다면 추가
        # add_column(connection, "place", "google_place_id", "VARCHAR(255)")
        # # place 테이블에 google_place_id UNIQUE 제약 조건 추가 (이미 존재할 수 있음)
        # add_unique_constraint(connection, "place", "uq_google_place_id", ["google_place_id"])
        # # place 테이블에 (place_name, address) 복합 UNIQUE 제약 조건 추가 (이미 존재할 수 있음)
        # # MySQL의 UNIQUE 제약 조건은 VARCHAR 컬럼에 대해 길이를 지정할 수 있습니다.
        # add_unique_constraint(connection, "place", "uq_place_name_address", ["place_name", "address(255)"])
        #
        # # place 테이블에 description 컬럼이 없다면 추가 (TEXT 타입)
        # # 기존에 description 컬럼이 VARCHAR 등으로 존재했다면 삭제 후 TEXT로 재추가하여 타입 일관성 확보
        # add_column(connection, "place", "description", "TEXT") # description 컬럼 추가
        # print("--- 기존 테이블 스키마 보정 완료 ---")

        # 3. CSV 파일에서 데이터를 읽어와 테이블에 삽입합니다.
        if not os.path.isdir(CSV_DIRECTORY_PATH):
            print(f"오류: 지정된 디렉토리 '{CSV_DIRECTORY_PATH}'를 찾을 수 없습니다. 디렉토리를 생성하거나 경로를 확인해주세요.")
        else:
            csv_files_found = False
            for filename in os.listdir(CSV_DIRECTORY_PATH):
                if filename.endswith(".csv"):
                    csv_file_full_path = os.path.join(CSV_DIRECTORY_PATH, filename)
                    print(f"\n--- '{filename}' 파일 처리 시작 ---")
                    insert_data_from_csv(connection, csv_file_full_path)
                    csv_files_found = True

            if not csv_files_found:
                print(f"경고: '{CSV_DIRECTORY_PATH}' 디렉토리에서 CSV 파일을 찾을 수 없습니다.")

        connection.close()

        print("MySQL 연결이 닫혔습니다.")