from pathlib import Path

import joblib
from fastapi import FastAPI, HTTPException

from app.schemas.moderation import PredictionRequest

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "artifacts"

app = FastAPI(
    title="Smart Community Comment Moderation API",
    version="1.0.0",
    description="Detects toxic comments and spam using one common endpoint.",
)

tox_path = ART / "toxicity_word_char_tfidf.joblib"
spam_path = ART / "sms_spam_tfidf_logistic_regression.joblib"

tox_bundle = joblib.load(tox_path) if tox_path.exists() else None
spam_bundle = joblib.load(spam_path) if spam_path.exists() else None


@app.get("/health")
def health():
    return {
        "status": "ok",
        "toxicity_model_loaded": tox_bundle is not None,
        "spam_model_loaded": spam_bundle is not None,
    }


@app.post("/api/v1/moderation/predict")
def moderate_comment(req: PredictionRequest):
    text = req.text.strip()

    if not text:
        raise HTTPException(
            status_code=422,
            detail="Comment must not be blank.",
        )

    if tox_bundle is None or spam_bundle is None:
        raise HTTPException(
            status_code=503,
            detail="One or both models are missing. Train the models first.",
        )

    tox_features = tox_bundle["features"].transform([text])
    tox_probabilities = tox_bundle["model"].predict_proba(tox_features)[0]

    tox_thresholds = tox_bundle["thresholds"]
    tox_labels = tox_bundle["labels"]

    toxicity_results = {
        name: bool(float(probability) >= float(tox_thresholds[name]))
        for name, probability in zip(tox_labels, tox_probabilities)
    }

    detected_toxicity_labels = [
        name
        for name, detected in toxicity_results.items()
        if detected
    ]

    is_toxic = bool(detected_toxicity_labels)

    spam_features = spam_bundle["features"].transform([text])

    spam_probability = float(
        spam_bundle["model"].predict_proba(spam_features)[0, 1]
    )

    spam_threshold = float(spam_bundle["threshold"])
    is_spam = spam_probability >= spam_threshold

    review = is_toxic or is_spam

    reasons = []

    if is_toxic:
        reasons.append("TOXICITY_DETECTED")

    if is_spam:
        reasons.append("SPAM_DETECTED")

    if not reasons:
        reasons.append("NO_HARMFUL_LABELS_DETECTED")

    return {
        "comment": text,
        "toxicity": {
            "detected": is_toxic,
            "labels": toxicity_results,
            "detected_labels": detected_toxicity_labels,
        },
        "spam": {
            "detected": is_spam,
            "probability": round(spam_probability, 6),
        },
        "review": review,
        "status": "REVIEW" if review else "PASSED",
        "reason": reasons,
    }
