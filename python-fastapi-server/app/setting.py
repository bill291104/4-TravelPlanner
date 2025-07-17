import os
from dotenv import load_dotenv

from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings

load_dotenv()
openai_api_key = os.getenv("OPENAI_API_KEY")

embedding_function = OpenAIEmbeddings(
    model="text-embedding-3-large"
)

persist_directory = './vector_db'
if not os.path.exists(persist_directory):
    os.mkdir(persist_directory)

db_collections = {
    "place": Chroma(
        collection_name="place",
        embedding_function=embedding_function,
        persist_directory=persist_directory
    ),
    "restaurant": Chroma(
        collection_name="restaurant",
        embedding_function=embedding_function,
        persist_directory=persist_directory
    ),
    "accom": Chroma(
        collection_name="accom",
        embedding_function=embedding_function,
        persist_directory=persist_directory
    )
}
