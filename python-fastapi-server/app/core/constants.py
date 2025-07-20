# app/core/constants.py
from enum import Enum

class Domains(str, Enum):
    PLACE = "place"
    RESTAURANT = "restaurant"
    ACCOM = "accom"