# Java 서버의 키워드 추출 객체

keyword_extraction_request={
    
    #유저의 입력 
    "user_input": "부산 힐링 캠핑장 여행지 추천해줘",
    
    #프롬프트에 전달할 명령 
    # "" 를 붙여야 문자열 리터럴로 인식함. 
    "prompt": """
    다음 사용자 입력에서 여행계획 및 여행 트랜드 관련된 핵심 키워드를 추출해줘.
    
    사용자 입력:{user_input}
    

    추출 기준:
        1. 지역/도시명 (예: 부산, 제주도, 서울)
        2. 여행 활동 (예: 해수욕, 등산, 쇼핑)
        3. 숙박 유형 (예: 호텔, 펜션, 게스트하우스)
        4. 음식/맛집 (예: 해산물, 카페, 맛집)
        5. 관광지 유형 (예: 해변, 산, 박물관, 사찰)
        6. 분위기/특성 (예: 로맨틱, 가족여행, 힐링)
        7. 의류 (예: 계절별, 기능별, 용도별 )
        
    결과는 쉼표로 구분된 키워드 목록으로 반환해주세요
    예시 : 부산, 해변, 카페, 맛집, 힐링
    
    
    
    """
    
}

#### => keyword_extraction_request={"user_input" ,"prompt"} 를 생성. 

#키워드 추출 함수를 정의
import asyncio
import os
from typing import List
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain.memory import ConversationBufferMemory

os.environ["OPENAI_API_KEY"] = "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"


#키워드 추출해주는 함수 
async def extract_keywords(user_input:str, prompt:str)->List[str]:
    #async = 비동기처리
        # 
    
#입력 매개변수 확인용
    print("사용자의 입력",user_input)
    print("사용할 프롬프트 명: ",prompt)
    
# DB에 저장된 문자열 형태의 프롬프트 
    template = ChatPromptTemplate.from_template(prompt)

#사용자 입력을 템플릿에 넣어서 llm에 전달할 메세지 만들기
    messages = template.format_messages(user_input=user_input)
    # Q. 이 메세지의 역할, 이 위치가 아니어도 상관없나? 
    # Q. 자바에서 받은 user_input(오)과  무엇의 user_input(왼)을 매칭한 것인지? #
    
#llm 설정, temperature는 낮을수록(0~) 창의성 낮아지고 일관된 답
    llm=ChatOpenAI(
        model="gpt-4o-mini",
        temperature=0.1
    )
        # => GPT가 프롬프트에 기입한 추출 기준에 따라, user_input의 사용자입력에서
        # 텍스트를 분석 및 키워드를 추출함.


###


#llm에게 명령을 전달하고 답변을 받음 
    response = await llm.ainvoke(messages)
        #await은 async 비동기 함수를 호출함

#답변의 내용 확인용 print
    print("AI응답",response.content)
        # Q. content는 출처가 어디?
    
#list 형태로 분할해서 반환
    return response.content.split(",")


user_input = keyword_extraction_request["user_input"]
prompt = keyword_extraction_request["prompt"]
#    ↓              ↓                    ↓
# 변수명      딕셔너리 변수            키(key)
# ↑ Python 딕셔너리 접근 문법
        # Q. 딕셔너리 접근이란?
            #키에 저장된 값을 가져오는 것
            # 사물함[열쇠번호] 
        #["user_input"] 키 값을 user_input에 저장 
        

# extracted_keywords = await extract_keywords(user_input, prompt)
extracted_keywords = asyncio.run(extract_keywords(user_input, prompt))
            #await -> asyncio.run( ~)로 변경/   
                #(+import asyncio 추가)
                #컴파일 에러가 파악이 안돼서
                
print(extracted_keywords)
    # extract_keywords = 키워드 추출 함수.


####=> 키워드 추출되었고, extract_keywords 에 저장되어있음. 

#임베딩, 백터 DB
from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings
from langchain_core.documents import Document

#DB 스키마 생성
if not os.path.exists("./chroma_trend_test"):
    os.makedirs("./chroma_trend_test")

#DB 설정 
vector_store = Chroma(
    collection_name="trend_collection",
    embedding_function = OpenAIEmbeddings(
        model="text-embedding-3-large"
        ),
    persist_directory="./chroma_trend_test"
)

# 더미 데이터 임베딩 
description = "부산 현지인만 아는 진짜 맛집으로 안내해드려요! 기장 신선한 해산물부터 골목 속 숨은 보석까지. "
metadata={"pk":"dumy_pk_001"}
document = Document(
    page_content = description,
    metadata=metadata
)

documents = []
documents.append(document)
vector_store.add_documents([document])  

####chroma 는 Document로 주고받음. 




#유사도 검색에 사용할 쿼리 생성
query = " ".join(extracted_keywords)
        # extract_keywords = 키워드 추출 함수.
            # Q. join의 기능
            # Q. " "의 문법 기능 

#유사도 검색 
search_result = vector_store.similarity_search(query)

#결과에서 PK추출 
pks=[]
for doc in search_result:
    pk=doc.metadata.get("pk")
    pks.append(pk)


#결과 pk 리스트 확인
print("벡터 검색 결과 PK들:",pks) 


#### => 벡터 임베딩 중 유사도 높은 PK를 가져왔음. 
#### 어떤 단어/문서가 유사한 지만 알려줌

#### RDB 사용 시점-------------------------------------------





####---------------------------------------------------------









### 정보 -> 계획수립 요청 -> 계획 응답


# 정보 더미 데이터 

# 1. 부산 오션스파 힐링
travel_info1 = {
    "travel_trend_name": "씨메르 스파 & 리조트",
    "travel_trend_type": "스파리조트",
    "rating": 5,
    "price": 580000,
    "min_personnel": 2,
    "max_personnel": 4,
    "address": "부산광역시 해운대구 해운대해변로 296",
    "latitude": 35.158611,
    "longitude": 129.160278,
    "description": " 바다를 바라보며 힐링하는 특별한 시간! 부산 최고급 오션스파에서 몸과 마음을 재충전해보세요. 인피니티풀과 웰니스 시설 완비.",
    "review_score": 9.4,
    "check_in_time": "15:00:00",
    "check_out_time": "11:00:00",
    "booking_url": "https://booking.example.com/accommodations/spa-resort"
}

# 2. 부산 로컬 맛집 탐방
travel_info2 = {
    "travel_trend_name": "기장 어촌마을 펜션",
    "travel_trend_type": "펜션",
    "rating": 4,
    "price": 120000,
    "min_personnel": 2,
    "max_personnel": 6,
    "address": "부산광역시 기장군 기장읍 해안로 123",
    "latitude": 35.244722,
    "longitude": 129.223333,
    "description": "부산 현지인만 아는 진짜 맛집으로 안내해드려요! 기장 신선한 해산물부터 골목 속 숨은 보석까지. 어촌마을 체험 가능.",
    "review_score": 9.1,
    "check_in_time": "16:00:00",
    "check_out_time": "10:00:00",
    "booking_url": "https://booking.example.com/accommodations/gijang-pension"
}

# 3. 감천문화마을 감성투어
travel_info3 = {
    "travel_trend_name": "감천마을 게스트하우스",
    "travel_trend_type": "게스트하우스",
    "rating": 3,
    "price": 85000,
    "min_personnel": 1,
    "max_personnel": 4,
    "address": "부산광역시 사하구 감천문화로 203",
    "latitude": 35.097222,
    "longitude": 129.011111,
    "description": "부산의 마추픽추에서 동화 같은 시간을! 알록달록 감천문화마을에서 인생샷과 감동을 동시에 만나보세요. 포토존 도보 1분.",
    "review_score": 8.7,
    "check_in_time": "14:00:00",
    "check_out_time": "11:00:00",
    "booking_url": "https://booking.example.com/accommodations/gamcheon-guesthouse"
}

# 4. 광안리 SUP 체험
travel_info4 = {
    "travel_trend_name": "광안리 오션 호텔",
    "travel_trend_type": "호텔",
    "rating": 4,
    "price": 280000,
    "min_personnel": 2,
    "max_personnel": 3,
    "address": "부산광역시 수영구 광안해변로 219",
    "latitude": 35.153056,
    "longitude": 129.118611,
    "description": "광안대교를 바라보며 바다 위를 걸어보세요! SUP로 즐기는 부산 바다의 짜릿한 모험이 기다려요. 수상스포츠 장비 대여 가능.",
    "review_score": 7.9,
    "check_in_time": "15:00:00",
    "check_out_time": "12:00:00",
    "booking_url": "https://booking.example.com/accommodations/gwangalli-ocean"
}

# 5. BTS 부산 성지순례
travel_info5 = {
    "travel_trend_name": "메그네이트 부티크 호텔",
    "travel_trend_type": "부티크호텔",
    "rating": 4,
    "price": 195000,
    "min_personnel": 1,
    "max_personnel": 2,
    "address": "부산광역시 중구 중앙대로 123",
    "latitude": 35.103611,
    "longitude": 129.032778,
    "description": "BTS와 함께하는 부산 여행! 지민이네 카페부터 RM이 다녀간 명소까지, 아미라면 놓칠 수 없는 성지순례. K-POP 테마룸 운영.",
    "review_score": 8.3,
    "check_in_time": "15:00:00",
    "check_out_time": "11:00:00",
    "booking_url": "https://booking.example.com/accommodations/bts-boutique"
}

# 6. 부산 동래온천 힐링
travel_info6 = {
    "travel_trend_name": "동래온천 힐스파 호텔",
    "travel_trend_type": "온천호텔",
    "rating": 4,
    "price": 350000,
    "min_personnel": 2,
    "max_personnel": 4,
    "address": "부산광역시 동래구 온천천로 137",
    "latitude": 35.205556,
    "longitude": 129.078889,
    "description": "부산 전통 온천의 진수를 만나보세요! 동래온천에서 자연 온천수의 따뜻한 품에 안겨 힐링하세요. 24시간 온천 이용 가능.",
    "review_score": 8.9,
    "check_in_time": "15:00:00",
    "check_out_time": "12:00:00",
    "booking_url": "https://booking.example.com/accommodations/dongnae-spa"
}

# 7. 부산 밤여행 투어
travel_info7 = {
    "travel_trend_name": "누리마루 뷰 호텔",
    "travel_trend_type": "호텔",
    "rating": 4,
    "price": 240000,
    "min_personnel": 2,
    "max_personnel": 2,
    "address": "부산광역시 해운대구 동백로 116",
    "latitude": 35.161944,
    "longitude": 129.149167,
    "description": "부산의 밤이 더 아름다워요! 광안대교 야경부터 밤바다까지, 낭만 가득한 부산 밤여행을 떠나보세요. 야경 전망 특화 객실.",
    "review_score": 7.7,
    "check_in_time": "15:00:00",
    "check_out_time": "11:00:00",
    "booking_url": "https://booking.example.com/accommodations/nurimaru-view"
}

# 8. 부산 가족 테마파크
travel_info8 = {
    "travel_trend_name": "롯데호텔 부산",
    "travel_trend_type": "호텔",
    "rating": 5,
    "price": 420000,
    "min_personnel": 2,
    "max_personnel": 6,
    "address": "부산광역시 기장군 기장읍 동부산관광로 15",
    "latitude": 35.188889,
    "longitude": 129.266667,
    "description": "온 가족이 함께하는 신나는 하루! 롯데월드 어드벤처 부산에서 아이들의 웃음소리가 가득한 추억을 만들어요. 테마파크 연계 패키지.",
    "review_score": 8.6,
    "check_in_time": "15:00:00",
    "check_out_time": "12:00:00",
    "booking_url": "https://booking.example.com/accommodations/lotte-family"
}

# 9. 부산 해안 차박
travel_info9 = {
    "travel_trend_name": "송정해변 캠핑장",
    "travel_trend_type": "캠핑장",
    "rating": 3,
    "price": 35000,
    "min_personnel": 2,
    "max_personnel": 4,
    "address": "부산광역시 해운대구 송정해변로 62",
    "latitude": 35.179167,
    "longitude": 129.199722,
    "description": "바다가 보이는 곳에서의 자유로운 밤! 부산 해안 차박으로 별빛과 파도소리가 함께하는 낭만을 만끽하세요. 차박 시설 완비.",
    "review_score": 7.2,
    "check_in_time": "16:00:00",
    "check_out_time": "10:00:00",
    "booking_url": "https://booking.example.com/accommodations/songjeong-camping"
}

# 10. 부산 해산물 미식투어
travel_info0 = {
    "travel_trend_name": "자갈치 전통호텔",
    "travel_trend_type": "전통호텔",
    "rating": 3,
    "price": 165000,
    "min_personnel": 2,
    "max_personnel": 4,
    "address": "부산광역시 중구 자갈치해안로 52",
    "latitude": 35.096667,
    "longitude": 129.030556,
    "description": "부산 바다의 신선함을 그대로! 자갈치시장부터 미쉐린 레스토랑까지 해산물 미식여행을 떠나보세요. 시장 투어 서비스 제공.",
    "review_score": 8.8,
    "check_in_time": "14:00:00",
    "check_out_time": "11:00:00",
    "booking_url": "https://booking.example.com/accommodations/jagalchi-traditional"
}


# 여행 계획 수립 요청 더미 데이터

generate_plan_request={
    "user_input": "2박 3일 여행갈건데, 가족들과 함께 갈거야 ",
    "travel_trend_context":[travel_info1,travel_info2],
    "user_request":"최신 트랜드를 보여줘, 다른 숙소나 식당은 상관없다.",
    #LLM의 답변 형식 프롬프트#
    "prompt":"""  
        당신은 전문적인 여행 플래너입니다. 사용자의 요청과 제공된 여행 정보를 바탕으로 구체적이고 실용적인 여행 계획을 작성해주세요. 
        
        사용자 요청:{user_request}
        
        관련 트랜드 정보: {travel_trend_context}
        
        여행 계획 작성 가이드라인:
        1. 사용자의 요청사항을 정확히 반영해주세요
        2. 제공된 여행 정보를 적극 활용해주세요
        3. 구체적인 장소명, 주소, 특징을 포함해주세요
        4. 시간대별 일정을 제안해주세요
        5. 교통편, 소요시간, 예상 비용 등 실용적인 정보를 포함해주세요
        6. 한국어로 친근하고 자연스럽게 작성해주세요

    여행 계획 :
    
    """
    
}




# 프롬프트 템플릿 객체 생성
travel_plan_template = ChatPromptTemplate.from_template(generate_plan_request["prompt"])

# 템플릿에 내용을 넣어 메세지 구성
messages=travel_plan_template.format_messages(
    user_input = generate_plan_request["user_input"],
    user_request = generate_plan_request["user_request"],
    travel_trend_context = generate_plan_request["travel_trend_context"]    
)

# llm설정, 
llm =ChatOpenAI(
    model="gpt-4o-mini",
    temperature=0.5
)

#여행 계획 세우기 llm에게 지시 
response =llm.invoke(messages)

print(response.content)
