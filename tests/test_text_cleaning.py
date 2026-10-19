import unicodedata, re
def clean_text(value):
    if value is None: return ""
    value=unicodedata.normalize("NFKC",str(value)).replace("\u200b","").replace("\ufeff","")
    return re.sub(r"\\s+"," ",value).strip()
def test_whitespace_is_collapsed():
    assert clean_text("  hello\\n  world ") == "hello world"
def test_empty_input():
    assert clean_text("   ") == ""
