from pathlib import Path
import json, re, unicodedata
import numpy as np
import pandas as pd
import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.pipeline import FeatureUnion
from sklearn.linear_model import LogisticRegression
from sklearn.multiclass import OneVsRestClassifier
from sklearn.metrics import classification_report, f1_score, accuracy_score, confusion_matrix

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "processed"
OUT = ROOT / "artifacts"
OUT.mkdir(exist_ok=True)
LABELS = ["toxic","severe_toxic","obscene","threat","insult","identity_hate"]

def threshold_by_f1(y_true, probabilities, candidates):
    scores = [f1_score(y_true, probabilities >= t, zero_division=0) for t in candidates]
    return float(candidates[int(np.argmax(scores))])

tr = pd.read_csv(DATA/"toxicity_train.csv")
va = pd.read_csv(DATA/"toxicity_validation.csv")
te = pd.read_csv(DATA/"toxicity_test.csv")
tox_features = FeatureUnion([
    ("word", TfidfVectorizer(ngram_range=(1,2), min_df=2, max_features=100000, sublinear_tf=True)),
    ("char", TfidfVectorizer(analyzer="char_wb", ngram_range=(3,5), min_df=3, max_features=120000, sublinear_tf=True, dtype=np.float32))
])
Xtr = tox_features.fit_transform(tr["text"].fillna(""))
Xva = tox_features.transform(va["text"].fillna(""))
Xte = tox_features.transform(te["text"].fillna(""))
Ytr, Yva, Yte = (df[LABELS].fillna(0).astype(int).to_numpy() for df in (tr,va,te))
tox_model = OneVsRestClassifier(
    LogisticRegression(max_iter=1000),
    n_jobs=1
)
tox_model.fit(Xtr,Ytr)
Pva = tox_model.predict_proba(Xva)
thresholds = {}
for i,label in enumerate(LABELS):
    thresholds[label] = threshold_by_f1(Yva[:,i], Pva[:,i], np.arange(.10,.91,.02))
Pte = tox_model.predict_proba(Xte)
Ypred = np.column_stack([Pte[:,i] >= thresholds[label] for i,label in enumerate(LABELS)]).astype(int)
tox_report = classification_report(Yte,Ypred,target_names=LABELS,output_dict=True,zero_division=0)
tox_result = {
    "macro_f1": float(f1_score(Yte,Ypred,average="macro",zero_division=0)),
    "micro_f1": float(f1_score(Yte,Ypred,average="micro",zero_division=0)),
    "classification_report": tox_report,
    "confusion_matrices": {label:confusion_matrix(Yte[:,i],Ypred[:,i]).tolist() for i,label in enumerate(LABELS)},
    "thresholds": thresholds,
    "test_rows": int(len(te)),
    "threshold_tuning": "per-label F1 maximization on validation split only"
}
joblib.dump({"features":tox_features,"model":tox_model,"labels":LABELS,"thresholds":thresholds},OUT/"toxicity_word_char_tfidf.joblib",compress=3)
(OUT/"toxicity_thresholds.json").write_text(json.dumps(thresholds,indent=2))
(OUT/"toxicity_test_report.json").write_text(json.dumps(tox_result,indent=2))

# Spam: its labels remain separate from the Jigsaw labels.
tr = pd.read_csv(DATA/"spam_train.csv")
va = pd.read_csv(DATA/"spam_validation.csv")
te = pd.read_csv(DATA/"spam_test.csv")
spam_features = FeatureUnion([
    ("word",TfidfVectorizer(ngram_range=(1,2),min_df=1,max_features=30000,sublinear_tf=True)),
    ("char",TfidfVectorizer(analyzer="char_wb",ngram_range=(3,5),min_df=1,max_features=30000,sublinear_tf=True,dtype=np.float32))
])
Xs_tr=spam_features.fit_transform(tr["text"].fillna(""))
Xs_va=spam_features.transform(va["text"].fillna(""))
Xs_te=spam_features.transform(te["text"].fillna(""))
ys_tr=(tr["spam_label"]=="spam").astype(int).to_numpy()
ys_va=(va["spam_label"]=="spam").astype(int).to_numpy()
ys_te=(te["spam_label"]=="spam").astype(int).to_numpy()
spam_model=LogisticRegression(C=4.0,class_weight="balanced",max_iter=500,solver="liblinear")
spam_model.fit(Xs_tr,ys_tr)
pva=spam_model.predict_proba(Xs_va)[:,1]
spam_threshold=threshold_by_f1(ys_va,pva,np.arange(.10,.91,.01))
pte=spam_model.predict_proba(Xs_te)[:,1]
ypred=(pte>=spam_threshold).astype(int)
spam_result={
 "accuracy":float(accuracy_score(ys_te,ypred)),
 "spam_f1":float(f1_score(ys_te,ypred,zero_division=0)),
 "classification_report":classification_report(ys_te,ypred,target_names=["ham","spam"],output_dict=True,zero_division=0),
 "confusion_matrix":confusion_matrix(ys_te,ypred).tolist(),
 "threshold":spam_threshold,
 "test_rows":int(len(te)),
 "threshold_tuning":"spam F1 maximization on validation split only"
}
joblib.dump({"features":spam_features,"model":spam_model,"threshold":spam_threshold,"labels":["ham","spam"]},OUT/"sms_spam_tfidf_logistic_regression.joblib",compress=3)
(OUT/"sms_spam_threshold.json").write_text(json.dumps({"spam_threshold":spam_threshold,"tuning_metric":"validation spam F1"},indent=2))
(OUT/"sms_spam_test_report.json").write_text(json.dumps(spam_result,indent=2))
print("Training finished. Reports and thresholds are saved in artifacts/.")
print("Toxicity macro F1:", round(tox_result["macro_f1"],4), "| micro F1:",round(tox_result["micro_f1"],4))
print("Spam accuracy:",round(spam_result["accuracy"],4),"| spam F1:",round(spam_result["spam_f1"],4),"| threshold:",spam_threshold)
