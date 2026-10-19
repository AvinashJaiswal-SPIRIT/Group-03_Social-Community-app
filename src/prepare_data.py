from pathlib import Path
import pandas as pd, numpy as np, unicodedata, re, json
from sklearn.model_selection import train_test_split

ROOT=Path(__file__).resolve().parents[1]
RAW=ROOT/"data"/"raw"; OUT=ROOT/"data"/"processed"; OUT.mkdir(parents=True,exist_ok=True)
LABELS=["toxic","severe_toxic","obscene","threat","insult","identity_hate"]
def clean_text(v):
    if pd.isna(v): return ""
    v=unicodedata.normalize("NFKC",str(v)).replace("\u200b","").replace("\ufeff","")
    return re.sub(r"\s+"," ",v).strip()

j=pd.read_csv(RAW/"jigsaw"/"train.csv")
j=j[["id","comment_text"]+LABELS].copy()
j["text"]=j["comment_text"].map(clean_text); j=j.drop(columns=["comment_text"])
j=j[j.text.ne("")].drop_duplicates(subset=["text"],keep="first").reset_index(drop=True)
for c in LABELS: j[c]=pd.to_numeric(j[c],errors="coerce").fillna(0).astype("int8")
j["source"]="jigsaw"; j["spam_label"]=np.nan; j["spam"]=np.nan
j=j[["id","text","source"]+LABELS+["spam_label","spam"]]
rows=[]
with open(RAW/"sms"/"SMSSpamCollection",encoding="utf-8",errors="replace") as f:
    for line in f:
        line=line.rstrip("\r\n")
        if "\t" in line:
            label,text=line.split("\t",1); label=label.strip().lower()
            if label in ("ham","spam"): rows.append((text,label))
s=pd.DataFrame(rows,columns=["text","spam_label"]); s["text"]=s.text.map(clean_text)
s=s[s.text.ne("")].drop_duplicates(subset=["text"],keep="first").reset_index(drop=True)
s["id"]=[f"sms_{i:06d}" for i in range(len(s))]; s["source"]="sms"; s["spam"]=(s.spam_label=="spam").astype("int8")
for c in LABELS: s[c]=np.nan
s=s[["id","text","source"]+LABELS+["spam_label","spam"]]
master=pd.concat([j,s],ignore_index=True)
j.to_csv(OUT/"jigsaw_cleaned.csv",index=False); s.to_csv(OUT/"sms_cleaned.csv",index=False); master.to_csv(OUT/"unified_master.csv",index=False)
jt,jtmp=train_test_split(j,test_size=.2,random_state=42,stratify=j.toxic)
jv,je=train_test_split(jtmp,test_size=.5,random_state=42,stratify=jtmp.toxic)
st,stmp=train_test_split(s,test_size=.2,random_state=42,stratify=s.spam_label)
sv,se=train_test_split(stmp,test_size=.5,random_state=42,stratify=stmp.spam_label)
for df,n in [(jt,"toxicity_train"),(jv,"toxicity_validation"),(je,"toxicity_test"),(st,"spam_train"),(sv,"spam_validation"),(se,"spam_test")]: df.to_csv(OUT/f"{n}.csv",index=False)
manifest={"jigsaw_cleaned_rows":len(j),"sms_cleaned_rows":len(s),"unified_master_rows":len(master),"normalization":"NFKC; remove zero-width/BOM; collapse whitespace; trim; preserve punctuation, case, and negation","label_policy":"Task labels remain separate; missing labels are null, not zero.","split_policy":"Independent 80/10/10 split, random_state=42."}
(OUT/"data_manifest.json").write_text(json.dumps(manifest,indent=2))
print(json.dumps(manifest,indent=2))
