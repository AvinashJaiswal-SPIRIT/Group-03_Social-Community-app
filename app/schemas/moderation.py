from pydantic import BaseModel, Field
class PredictionRequest(BaseModel):
    text: str = Field(min_length=1, max_length=10000)
class ToxicityResponse(BaseModel):
    text: str
    probabilities: dict[str, float]
    labels: list[str]
class SpamResponse(BaseModel):
    text: str
    spam_probability: float
    predicted_label: str
